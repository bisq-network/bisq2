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

package bisq.support;

import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.support.alert.AlertService;
import bisq.support.mediation.MediationService;
import bisq.support.security_manager.SecurityManagerService;
import bisq.user.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class SupportService implements Service {
    private final MediationService mediationService;
    private final AlertService alertService;
    private final SecurityManagerService securityManagerService;

    public SupportService(NetworkService networkService,
                          IdentityService identityService,
                          ChatService chatService,
                          UserService userService,
                          BondedRolesService bondedRolesService) {
        mediationService = new MediationService(networkService, chatService, userService, bondedRolesService);
        alertService = new AlertService(networkService, userService, bondedRolesService);
        securityManagerService = new SecurityManagerService(networkService, userService, bondedRolesService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        return mediationService.initialize()
                .thenCompose(result -> securityManagerService.initialize())
                .thenCompose(result -> alertService.initialize());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return mediationService.shutdown()
                .thenCompose(result -> securityManagerService.shutdown())
                .thenCompose(result -> alertService.shutdown());
    }
}