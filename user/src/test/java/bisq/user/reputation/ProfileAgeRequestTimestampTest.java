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
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.persistence.PersistenceService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileAgeRequestTimestampTest {
    private static final String PROFILE_ID = "a".repeat(40);

    @TempDir
    Path tempDir;

    @Test
    void aSentTimestampRequestRecordsLastRequested() throws Exception {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.empty());
        when(networkService.confidentialSend(any(EnvelopePayloadMessage.class), any(NetworkId.class), any(NetworkIdWithKeyPair.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        AuthorizedOracleNode oracleNode = mock(AuthorizedOracleNode.class, RETURNS_DEEP_STUBS);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getAuthorizedOracleNodes()).thenReturn(Set.of(oracleNode));

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

        ProfileAgeService profileAgeService = new ProfileAgeService(new PersistenceService(tempDir),
                networkService,
                userIdentityService,
                mock(UserProfileService.class),
                mock(BannedUserService.class),
                authorizedBondedRolesService);
        assertThat(profileAgeService.getPersistableStore().getLastRequested()).isZero();

        Method requestRound = ProfileAgeService.class.getDeclaredMethod("maybeRequestTimestampForAllUserProfiles");
        requestRound.setAccessible(true);
        requestRound.invoke(profileAgeService);

        verify(networkService, timeout(5_000)).confidentialSend(any(AuthorizeTimestampRequest.class), any(NetworkId.class),
                any(NetworkIdWithKeyPair.class));

        long deadline = System.currentTimeMillis() + 5_000;
        while (profileAgeService.getPersistableStore().getLastRequested() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(profileAgeService.getPersistableStore().getLastRequested())
                .as("lastRequested after the request was sent")
                .isPositive();
    }
}
