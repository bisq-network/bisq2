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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.application.DevMode;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedTimestampData;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileAgeRequestTimestampTest {
    private static final String DEV_PRIVATE_KEY_1 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY_1 = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String PROFILE_ID = "a".repeat(40);

    @TempDir
    Path tempDir;

    private NetworkService networkService;
    private DataService dataService;
    private AuthenticatedDataStorageService store;
    private ProfileAgeService profileAgeService;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(AuthorizedTimestampData.class, AuthorizedTimestampData.META_DATA);
        DevMode.setDevMode(true);
        PersistenceService persistenceService = new PersistenceService(tempDir);
        dataService = new DataService(persistenceService);
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(AuthorizedTimestampData.class.getSimpleName())
                .join();

        networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        when(networkService.confidentialSend(any(EnvelopePayloadMessage.class), any(NetworkId.class), any(NetworkIdWithKeyPair.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        AuthorizedOracleNode oracleNode = mock(AuthorizedOracleNode.class, RETURNS_DEEP_STUBS);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedOracleNodes()).thenReturn(Set.of(oracleNode));
        when(authorizedBondedRolesService.hasAuthorizedPubKey(any(AuthorizedData.class), eq(BondedRoleType.ORACLE_NODE)))
                .thenReturn(true);

        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getId()).thenReturn(PROFILE_ID);
        when(userProfile.getUserName()).thenReturn("l");
        UserIdentity userIdentity = mock(UserIdentity.class, RETURNS_DEEP_STUBS);
        when(userIdentity.getUserProfile()).thenReturn(userProfile);
        when(userIdentity.getId()).thenReturn(PROFILE_ID);
        UserIdentityService userIdentityService = mock(UserIdentityService.class, RETURNS_DEEP_STUBS);
        ObservableSet<UserIdentity> userIdentities = new ObservableSet<>();
        userIdentities.add(userIdentity);
        when(userIdentityService.getUserIdentities()).thenReturn(userIdentities);

        profileAgeService = new ProfileAgeService(persistenceService,
                networkService,
                userIdentityService,
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                authorizedBondedRolesService);
    }

    @AfterEach
    void tearDown() {
        try {
            dataService.shutdown();
        } finally {
            DevMode.setDevMode(false);
        }
    }

    @Test
    void aSentTimestampRequestRecordsLastRequested() throws Exception {
        assertThat(profileAgeService.getPersistableStore().getLastRequested()).isZero();

        profileAgeService.maybeRequestTimestampForAllUserProfiles();

        verifyRequestSent();
        long deadline = System.currentTimeMillis() + 5_000;
        while (profileAgeService.getPersistableStore().getLastRequested() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(profileAgeService.getPersistableStore().getLastRequested())
                .as("lastRequested after the request was sent")
                .isPositive();
    }

    @Test
    void aTimestampedProfileIsRequestedAgainWhileNoRequestWasRecorded() throws Exception {
        // The store of every install before the fix, and of a profile requested at its creation
        addTimestamp();
        assertThat(profileAgeService.getTimeStampedProfileIds()).containsExactly(PROFILE_ID);

        profileAgeService.maybeRequestTimestampForAllUserProfiles();

        verifyRequestSent();
    }

    @Test
    void aTimestampedProfileIsNotRequestedAgainRightAfterARecordedRequest() throws Exception {
        addTimestamp();
        profileAgeService.getPersistableStore().setLastRequested(System.currentTimeMillis());

        profileAgeService.maybeRequestTimestampForAllUserProfiles();

        verify(networkService, after(1_000).never()).confidentialSend(any(EnvelopePayloadMessage.class),
                any(NetworkId.class), any(NetworkIdWithKeyPair.class));
    }

    private void verifyRequestSent() {
        verify(networkService, timeout(5_000)).confidentialSend(any(AuthorizeTimestampRequest.class), any(NetworkId.class),
                any(NetworkIdWithKeyPair.class));
    }

    private void addTimestamp() throws GeneralSecurityException {
        KeyPair keyPair = new KeyPair(KeyGeneration.getPublicKeyFromHex(DEV_PUBLIC_KEY_1),
                KeyGeneration.getPrivateKeyFromHex(DEV_PRIVATE_KEY_1));
        AuthorizedTimestampData timestamp = new AuthorizedTimestampData(PROFILE_ID, 1_700_000_000_000L, false);
        AuthorizedData authorizedData = new AuthorizedData(timestamp,
                Optional.of(SignatureUtil.sign(timestamp.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess()).isTrue();
    }
}
