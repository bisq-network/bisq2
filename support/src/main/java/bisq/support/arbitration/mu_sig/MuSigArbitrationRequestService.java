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

package bisq.support.arbitration.mu_sig;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.security.DigestUtil;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MuSigArbitrationRequestService implements Service {
    private final UserProfileService userProfileService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    public MuSigArbitrationRequestService(UserService userService,
                                          BondedRolesService bondedRolesService) {
        userProfileService = userService.getUserProfileService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
    }

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Optional<UserProfile> selectArbitrator(String makersUserProfileId,
                                                  String takersUserProfileId,
                                                  String offerId,
                                                  Optional<String> mediatorsUserProfileId) {
        Set<AuthorizedBondedRole> arbitrators = authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                .filter(role -> role.getBondedRoleType() == BondedRoleType.ARBITRATOR)
                .filter(role -> !role.getProfileId().equals(makersUserProfileId) &&
                        !role.getProfileId().equals(takersUserProfileId))
                .filter(role -> mediatorsUserProfileId
                        .map(profileId -> !role.getProfileId().equals(profileId))
                        .orElse(true))
                .collect(Collectors.toSet());
        return selectArbitrator(arbitrators, makersUserProfileId, takersUserProfileId, offerId);
    }

    public Optional<UserProfile> selectArbitrator(Set<AuthorizedBondedRole> arbitrators,
                                                  String makersProfileId,
                                                  String takersProfileId,
                                                  String offerId) {
        if (arbitrators.isEmpty()) {
            return Optional.empty();
        }

        if (arbitrators.size() == 1) {
            return userProfileService.findUserProfile(arbitrators.iterator().next().getProfileId());
        }

        int index = getDeterministicIndex(arbitrators, makersProfileId, takersProfileId, offerId);

        ArrayList<AuthorizedBondedRole> list = new ArrayList<>(arbitrators);
        list.sort(Comparator.comparing(AuthorizedBondedRole::getProfileId));
        return userProfileService.findUserProfile(list.get(index).getProfileId());
    }

    private int getDeterministicIndex(Set<AuthorizedBondedRole> arbitrators,
                                      String makersProfileId,
                                      String takersProfileId,
                                      String offerId) {
        String input = makersProfileId + takersProfileId + offerId;
        byte[] hash = DigestUtil.hash(input.getBytes(StandardCharsets.UTF_8)); // returns 20 bytes
        // XOR multiple 4-byte chunks to use more of the hash
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        int space = buffer.getInt(); // First 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with last 4 bytes (20 bytes total)
        return Math.floorMod(space, arbitrators.size());
    }
}
