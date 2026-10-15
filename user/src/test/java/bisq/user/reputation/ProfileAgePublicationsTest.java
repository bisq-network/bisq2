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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.reputation;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.DevMode;
import bisq.common.data.ByteArray;
import bisq.common.observable.map.ObservableHashMap;
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
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedTimestampData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Several oracle nodes publish the timestamp of a profile, each with its own key. The profile age is the oldest
 * timestamp of the profile which an oracle node authorized, as at the start of a node (rule 18 of
 * docs/specifications/bonded-roles/authorized-data.md, finding O-3 of the review 2026-10-03).
 * <p>
 * While only static oracle nodes are allowed, the store accepts timestamps only from an oracle node key (rule 16). The
 * oracle node keys of the tests are dev keys, which are the oracle node keys in dev mode.
 */
class ProfileAgePublicationsTest {
    // The first two of the dev keys of AuthorizedPubKeys.DEV_PUB_KEYS. In dev mode they are the oracle node keys.
    private static final String DEV_PRIVATE_KEY_1 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY_1 = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PRIVATE_KEY_2 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104201c6685cef332eecfddb253b601c0ed89e7c822d2d03c53d5a720a860b5dc9419a00706052b8104000aa144034200044b3a5f869a4a4346fb37ca70991ff55f3ef4f0ed3a31b51238aee75858af48fddaeafb11b9c59b3e9f551ee9022a767e5f39dcea2b4a96e1c610a5f1b1a1878e";
    private static final String DEV_PUBLIC_KEY_2 = "3056301006072a8648ce3d020106052b8104000a034200044b3a5f869a4a4346fb37ca70991ff55f3ef4f0ed3a31b51238aee75858af48fddaeafb11b9c59b3e9f551ee9022a767e5f39dcea2b4a96e1c610a5f1b1a1878e";

    private static final String PROFILE_ID = "a".repeat(40);
    private static final long OLDER_DATE = 1_700_000_000_000L;
    private static final long NEWER_DATE = 1_700_000_100_000L;

    @TempDir
    Path tempDir;

    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private ProfileAgeService profileAgeService;
    private KeyPair oracleKeyPair;
    private KeyPair otherOracleKeyPair;

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
        // The static oracle node keys of this version
        DevMode.setDevMode(true);
        oracleKeyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_1),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_1));
        otherOracleKeyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_2),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_2));
        AuthorizedBondedRolesService authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService,
                false,
                Set.of(Hex.encode(oracleKeyPair.getPublic().getEncoded()),
                        Hex.encode(otherOracleKeyPair.getPublic().getEncoded())),
                true);
        // The profile store holds the profile
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getId()).thenReturn(PROFILE_ID);
        when(userProfile.getProfileAgeKey()).thenReturn(profileKey());
        ObservableHashMap<String, UserProfile> userProfileById = new ObservableHashMap<>();
        userProfileById.put(PROFILE_ID, userProfile);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.getUserProfileById()).thenReturn(userProfileById);
        when(userProfileService.findUserProfile(PROFILE_ID)).thenReturn(Optional.of(userProfile));
        profileAgeService = new ProfileAgeService(persistenceService,
                networkService,
                mock(UserIdentityService.class, RETURNS_DEEP_STUBS),
                userProfileService,
                mock(BannedUserService.class),
                authorizedBondedRolesService);
        // As NetworkService.addDataServiceListener does
        dataService.addListener(authorizedBondedRolesService);
        profileAgeService.initialize().join();
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        try {
            profileAgeService.shutdown().join();
            store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
            dataService.shutdown();
        } finally {
            DevMode.setDevMode(false);
        }
    }

    @Test
    void profileAgeStaysWhileAnotherOracleNodePublishesTheTimestamp() throws GeneralSecurityException {
        AuthorizedTimestampData timestamp = new AuthorizedTimestampData(PROFILE_ID, OLDER_DATE, false);
        AuthorizedData publication = publish(timestamp, oracleKeyPair);
        AuthorizedData publicationOfOtherOracle = publish(timestamp, otherOracleKeyPair);

        remove(publication, oracleKeyPair);

        assertThat(profileAgeService.getDataSetByHash().get(profileKey())).containsExactly(timestamp);
        assertThat(profileAgeService.getScoreByUserProfileId()).containsKey(PROFILE_ID);

        remove(publicationOfOtherOracle, otherOracleKeyPair);

        assertThat(profileAgeService.getDataSetByHash()).doesNotContainKey(profileKey());
        assertThat(profileAgeService.getScoreByUserProfileId()).doesNotContainKey(PROFILE_ID);
    }

    // A node which starts takes the oldest timestamp of the profile which is still published
    @Test
    void removalOfTheOldestTimestampGivesTheProfileTheNextOne() throws GeneralSecurityException {
        AuthorizedTimestampData olderTimestamp = new AuthorizedTimestampData(PROFILE_ID, OLDER_DATE, false);
        AuthorizedTimestampData newerTimestamp = new AuthorizedTimestampData(PROFILE_ID, NEWER_DATE, false);
        AuthorizedData olderPublication = publish(olderTimestamp, oracleKeyPair);
        AuthorizedData newerPublication = publish(newerTimestamp, otherOracleKeyPair);

        remove(olderPublication, oracleKeyPair);

        assertThat(profileAgeService.getDataSetByHash().get(profileKey())).containsExactly(newerTimestamp);

        remove(newerPublication, otherOracleKeyPair);

        assertThat(profileAgeService.getDataSetByHash()).doesNotContainKey(profileKey());
    }

    @Test
    void removalOfANewerTimestampKeepsTheProfileAge() throws GeneralSecurityException {
        AuthorizedTimestampData olderTimestamp = new AuthorizedTimestampData(PROFILE_ID, OLDER_DATE, false);
        AuthorizedTimestampData newerTimestamp = new AuthorizedTimestampData(PROFILE_ID, NEWER_DATE, false);
        publish(olderTimestamp, oracleKeyPair);
        AuthorizedData newerPublication = publish(newerTimestamp, otherOracleKeyPair);

        remove(newerPublication, otherOracleKeyPair);

        assertThat(profileAgeService.getDataSetByHash().get(profileKey())).containsExactly(olderTimestamp);
        assertThat(profileAgeService.getScoreByUserProfileId()).containsKey(PROFILE_ID);
    }

    // A copy of another key does not count (rules 6 and 10). Its removal must not remove the profile age.
    @Test
    void removalOfACopyOfAnotherKeyKeepsTheProfileAge() throws GeneralSecurityException {
        AuthorizedTimestampData timestamp = new AuthorizedTimestampData(PROFILE_ID, OLDER_DATE, false);
        publish(timestamp, oracleKeyPair);
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedData copy = putTimestampOfAnotherKey(timestamp, otherKeyPair);

        remove(copy, otherKeyPair);

        assertThat(profileAgeService.getDataSetByHash().get(profileKey())).containsExactly(timestamp);
    }

    private AuthorizedData publish(AuthorizedTimestampData timestamp, KeyPair keyPair) throws GeneralSecurityException {
        AuthorizedData authorizedData = authorize(timestamp, keyPair);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess()).isTrue();
        return authorizedData;
    }

    // The store rejects a timestamp of another key (rules 2 and 16), and removes it when it loads. The service does not
    // rely on that (rule 6), so the test puts the timestamp into the store directly.
    private AuthorizedData putTimestampOfAnotherKey(AuthorizedTimestampData timestamp, KeyPair keyPair)
            throws GeneralSecurityException {
        AuthorizedData authorizedData = authorize(timestamp, keyPair);
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authorizedData, keyPair);
        assertThat(store.add(request).isAuthorizedKeyNotStatic()).isTrue();
        store.getPersistableStore().getMap().put(request.getStorageKey(), request);
        return authorizedData;
    }

    private static AuthorizedData authorize(AuthorizedTimestampData timestamp, KeyPair keyPair)
            throws GeneralSecurityException {
        return new AuthorizedData(timestamp,
                Optional.of(SignatureUtil.sign(timestamp.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
    }

    private void remove(AuthorizedData authorizedData, KeyPair keyPair) throws GeneralSecurityException {
        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess())
                .isTrue();
    }

    private static ByteArray profileKey() {
        return new ByteArray(PROFILE_ID.getBytes(StandardCharsets.UTF_8));
    }
}
