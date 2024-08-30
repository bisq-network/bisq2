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

package bisq.user;

import bisq.bonded_roles.BondedRolesService;
import bisq.common.application.Service;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class UserService implements Service {
    private final BannedUserService bannedUserService;
    private final RepublishUserProfileService republishUserProfileService;

    private final UserProfileService userProfileService;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;

    public UserService(PersistenceService persistenceService,
                       SecurityService securityService,
                       IdentityService identityService,
                       NetworkService networkService,
                       BondedRolesService bondedRolesService) {

        bannedUserService = new BannedUserService(persistenceService,
                bondedRolesService.getAuthorizedBondedRolesService());

        userProfileService = new UserProfileService(persistenceService, securityService, networkService);

        userIdentityService = new UserIdentityService(persistenceService,
                securityService,
                identityService,
                networkService);

        republishUserProfileService = new RepublishUserProfileService(userIdentityService);

        reputationService = new ReputationService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                bannedUserService,
                bondedRolesService.getAuthorizedBondedRolesService());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return userProfileService.initialize()
                .thenCompose(result -> userIdentityService.initialize())
                .thenCompose(result -> republishUserProfileService.initialize())
                .thenCompose(result -> reputationService.initialize())
                .thenCompose(result -> bannedUserService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return userProfileService.shutdown()
                .thenCompose(result -> userIdentityService.shutdown())
                .thenCompose(result -> republishUserProfileService.shutdown())
                .thenCompose(result -> reputationService.shutdown())
                .thenCompose(result -> bannedUserService.shutdown());
    }
}