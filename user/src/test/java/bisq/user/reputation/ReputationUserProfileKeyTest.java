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
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReputationUserProfileKeyTest {
    @Test
    void keyDerivedFromProfileIdEqualsKeyOfProfile() {
        UserProfile userProfile = ReputationTestProfiles.createUserProfile();

        for (SourceReputationService<?> service : createServices()) {
            assertThat(service.getUserProfileKey(userProfile.getId()))
                    .as(service.getClass().getSimpleName())
                    .isEqualTo(service.getUserProfileKey(userProfile));
        }
    }

    private static List<SourceReputationService<?>> createServices() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        NetworkService networkService = mock(NetworkService.class);
        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        BannedUserService bannedUserService = mock(BannedUserService.class);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        WitnessReputationClaimRegistry claimRegistry = new WitnessReputationClaimRegistry();
        return List.of(
                new ProofOfBurnService(networkService, userIdentityService, userProfileService, bannedUserService,
                        authorizedBondedRolesService),
                new BondedReputationService(networkService, userIdentityService, userProfileService, bannedUserService,
                        authorizedBondedRolesService),
                new AccountAgeService(persistenceService, networkService, userIdentityService, userProfileService,
                        bannedUserService, authorizedBondedRolesService, claimRegistry),
                new SignedWitnessService(persistenceService, networkService, userIdentityService, userProfileService,
                        bannedUserService, authorizedBondedRolesService, claimRegistry),
                new ProfileAgeService(persistenceService, networkService, userIdentityService, userProfileService,
                        bannedUserService, authorizedBondedRolesService));
    }
}
