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

package bisq.support.moderator;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatChannelDomain;
import bisq.common.application.Service;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class ModerationRequestService implements Service {
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;

    public ModerationRequestService(NetworkService networkService,
                                    UserService userService,
                                    BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        bannedUserService = userService.getBannedUserService();
    }

    public void reportUserProfile(UserProfile accusedUserProfile, String message) {
        UserIdentity myUserIdentity = userIdentityService.getSelectedUserIdentity();
        checkArgument(!bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile()));
        String reportSenderUserProfileId = myUserIdentity.getUserProfile().getId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        long date = System.currentTimeMillis();
        authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                .filter(e -> e.getBondedRoleType() == BondedRoleType.MODERATOR)
                .forEach(bondedRole -> {
                    ReportToModeratorMessage report = new ReportToModeratorMessage(date,
                            reportSenderUserProfileId,
                            accusedUserProfile,
                            message,
                            ChatChannelDomain.DISCUSSION);
                    networkService.confidentialSend(report, bondedRole.getNetworkId(), senderNetworkIdWithKeyPair);
                });
    }
}