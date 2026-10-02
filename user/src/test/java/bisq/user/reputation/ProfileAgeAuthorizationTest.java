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

class ProfileAgeAuthorizationTest {
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
        oracleKeyPair = KeyGeneration.generateDefaultEcKeyPair();
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
        // Finish queued writes before JUnit deletes the temporary directory.
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        dataService.shutdown();
    }

    @Test
    void storedTimestampFromAnotherKeyDoesNotCountAsTimestamped() throws GeneralSecurityException {
        addTimestamp(PROFILE_ID, KeyGeneration.generateDefaultEcKeyPair());

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
        addTimestamp(PROFILE_ID, otherKeyPair);
        addTimestamp(OTHER_PROFILE_ID, otherKeyPair);
        addTimestamp(PROFILE_ID, oracleKeyPair);

        assertThat(profileAgeService.getTimeStampedProfileIds()).containsExactly(PROFILE_ID);
    }

    @Test
    void untrustedCopyDoesNotCountAfterOracleTimestampIsRemoved() throws GeneralSecurityException {
        AuthorizedData oracleTimestamp = addTimestamp(PROFILE_ID, oracleKeyPair);
        addTimestamp(PROFILE_ID, KeyGeneration.generateDefaultEcKeyPair());
        assertThat(profileAgeService.getTimeStampedProfileIds()).containsExactly(PROFILE_ID);

        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, oracleTimestamp, oracleKeyPair))
                .isSuccess()).isTrue();

        assertThat(dataService.getAuthorizedData()).hasSize(1);
        assertThat(profileAgeService.getTimeStampedProfileIds()).isEmpty();
    }

    private AuthorizedData addTimestamp(String profileId, KeyPair keyPair) throws GeneralSecurityException {
        // Without the static flag, storage checks the signature but leaves oracle authority to the application.
        AuthorizedTimestampData timestamp = new AuthorizedTimestampData(profileId, 1_700_000_000_000L, false);
        AuthorizedData authorizedData = new AuthorizedData(timestamp,
                Optional.of(SignatureUtil.sign(timestamp.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess()).isTrue();
        return authorizedData;
    }
}
