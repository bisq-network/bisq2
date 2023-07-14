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
import bisq.network.NetworkService;
import bisq.support.mediation.MediationService;
import bisq.support.security_manager.SecurityManagerService;
import bisq.user.UserService;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class SupportService implements Service {
    private final MediationService mediationService;
    private final SecurityManagerService securityManagerService;

    @Getter
    @ToString
    public static final class Config {
        private final com.typesafe.config.Config securityManagerConfig;

        public Config(com.typesafe.config.Config securityManagerConfig) {
            this.securityManagerConfig = securityManagerConfig;
        }

        public static SupportService.Config from(com.typesafe.config.Config typeSafeConfig) {
            return new SupportService.Config(typeSafeConfig.getConfig("securityManager"));
        }
    }

    public SupportService(SupportService.Config config,
                          NetworkService networkService,
                          ChatService chatService,
                          UserService userService,
                          BondedRolesService bondedRolesService) {
        mediationService = new MediationService(networkService, chatService, userService, bondedRolesService);
        securityManagerService = new SecurityManagerService(SecurityManagerService.Config.from(config.getSecurityManagerConfig()),
                networkService,
                userService,
                bondedRolesService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        return mediationService.initialize()
                .thenCompose(result -> securityManagerService.initialize());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return mediationService.shutdown()
                .thenCompose(result -> securityManagerService.shutdown());
    }
}