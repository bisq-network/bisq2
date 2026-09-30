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

package bisq.support.dispute.mu_sig;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;

import java.util.Optional;
import java.util.stream.Stream;

public final class MuSigDisputeRoleIdentityResolver {
    private MuSigDisputeRoleIdentityResolver() {
    }

    public static Optional<UserIdentity> findMyUserIdentity(Optional<UserProfile> userProfile,
                                                            AuthorizedBondedRolesService authorizedBondedRolesService,
                                                            UserIdentityService userIdentityService,
                                                            BondedRoleType bondedRoleType) {
        return userProfile.flatMap(profile -> findMyUserIdentities(authorizedBondedRolesService, userIdentityService, bondedRoleType)
                .filter(userIdentity -> userIdentity.getUserProfile().getId().equals(profile.getId()))
                .findAny());
    }

    private static Stream<UserIdentity> findMyUserIdentities(AuthorizedBondedRolesService authorizedBondedRolesService,
                                                             UserIdentityService userIdentityService,
                                                             BondedRoleType bondedRoleType) {
        // If we got banned we still want to show the admin UI.
        return authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                .filter(data -> data.getBondedRoleType() == bondedRoleType)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getProfileId()).stream());
    }
}
