/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.reputation;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedTimestampData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Only timestamps which an oracle node authorized count as existing timestamps (rule 6 of
 * docs/specifications/bonded-roles/authorized-data.md). While only static oracle nodes are allowed, the store accepts
 * timestamps only from an oracle node key (rule 16). The lookup does not rely on that, so the tests put timestamps of
 * other keys into the store directly. The oracle node key of the tests is a dev key, which is an oracle node key in dev
 * mode.
 */
class ProfileAgeAuthorizationTest {
    // The first of the dev keys of AuthorizedPubKeys.DEV_PUB_KEYS. In dev mode it is an oracle node key.
    private static final String DEV_PRIVATE_KEY_1 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY_1 = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";

    private static final String PROFILE_ID = "a".repeat(40);
    private static final String OTHER_PROFILE_ID = "b".repeat(40);

    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private ProfileAgeService profileAgeService;
    private KeyPair oracleKeyPair;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(AuthorizedTimestampData.class, AuthorizedTimestampData.META_DATA);
        PersistenceService persistenceService = new PersistenceService(tempDir);
        dataService = new DataService(persistenceService);
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedTimestampData.class.getSimpleName())
                .join();
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        DevMode.setDevMode(true);
        oracleKeyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_1),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_1));
        AuthorizedBondedRolesService authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService,
                false,
                Set.of(Hex.encode(oracleKeyPair.getPublic().getEncoded())),
                true);
        profileAgeService = new ProfileAgeService(persistenceService,
                networkService,
                mock(UserIdentityService.class),
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                authorizedBondedRolesService);
    }

    @AfterEach
    void tearDown() {
        try {
            // Finish queued writes before JUnit deletes the temporary directory.
            store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
            dataService.shutdown();
        } finally {
            DevMode.setDevMode(false);
        }
    }

    @Test
    void storedTimestampFromAnotherKeyDoesNotCountAsTimestamped() throws GeneralSecurityException {
        putTimestampOfAnotherKey(PROFILE_ID, KeyGeneration.generateDefaultEcKeyPair());

        assertThat(profileAgeService.getTimeStampedProfileIds()).isEmpty();
    }

    @Test
    void storedOracleTimestampCountsBeforeProfileAgeIsApplied() throws GeneralSecurityException {
        addTimestamp(PROFILE_ID, oracleKeyPair);

        assertThat(profileAgeService.getDataSetByHash()).isEmpty();
        assertThat(profileAgeService.getTimeStampedProfileIds()).containsExactly(PROFILE_ID);
    }

    @Test
    void mixedPublicationsCountOnlyProfilesWithAnOracleTimestamp() throws GeneralSecurityException {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        putTimestampOfAnotherKey(PROFILE_ID, otherKeyPair);
        putTimestampOfAnotherKey(OTHER_PROFILE_ID, otherKeyPair);
        addTimestamp(PROFILE_ID, oracleKeyPair);

        assertThat(profileAgeService.getTimeStampedProfileIds()).containsExactly(PROFILE_ID);
    }

    @Test
    void untrustedCopyDoesNotCountAfterOracleTimestampIsRemoved() throws GeneralSecurityException {
        AuthorizedData oracleTimestamp = addTimestamp(PROFILE_ID, oracleKeyPair);
        putTimestampOfAnotherKey(PROFILE_ID, KeyGeneration.generateDefaultEcKeyPair());
        assertThat(profileAgeService.getTimeStampedProfileIds()).containsExactly(PROFILE_ID);

        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, oracleTimestamp, oracleKeyPair))
                .isSuccess()).isTrue();

        assertThat(dataService.getAuthorizedData()).hasSize(1);
        assertThat(profileAgeService.getTimeStampedProfileIds()).isEmpty();
    }

    // The store accepts a timestamp of an oracle node key, also without the static flag
    private AuthorizedData addTimestamp(String profileId, KeyPair keyPair) throws GeneralSecurityException {
        AuthorizedData authorizedData = authorize(profileId, keyPair);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess()).isTrue();
        return authorizedData;
    }

    // The store rejects a timestamp of another key (rules 2 and 16), and removes it when it loads. The lookup does not
    // rely on that (rule 6), so the test puts the timestamp into the store directly.
    private void putTimestampOfAnotherKey(String profileId, KeyPair keyPair) throws GeneralSecurityException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authorize(profileId, keyPair),
                keyPair);
        assertThat(store.add(request).isAuthorizedKeyNotStatic()).isTrue();
        store.getPersistableStore().getMap().put(request.getStorageKey(), request);
    }

    private static AuthorizedData authorize(String profileId, KeyPair keyPair) throws GeneralSecurityException {
        AuthorizedTimestampData timestamp = new AuthorizedTimestampData(profileId, 1_700_000_000_000L, false);
        return new AuthorizedData(timestamp,
                Optional.of(SignatureUtil.sign(timestamp.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }
}
