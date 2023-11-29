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
import bisq.persistence.PersistenceService;
import bisq.support.mediation.MediationRequestService;
import bisq.support.mediation.MediatorService;
import bisq.support.moderator.ModeratorService;
import bisq.support.release_manager.ReleaseManagerService;
import bisq.support.security_manager.SecurityManagerService;
import bisq.user.UserService;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class SupportService implements Service {
    private final MediationRequestService mediationRequestService;
    private final SecurityManagerService securityManagerService;
    private final ModeratorService moderatorService;
    private final ReleaseManagerService releaseManagerService;
    private final MediatorService mediatorService;

    @Getter
    @ToString
    public static final class Config {
        private final com.typesafe.config.Config securityManagerConfig;
        private final com.typesafe.config.Config releaseManagerService;
        private final com.typesafe.config.Config moderatorConfig;

        public Config(com.typesafe.config.Config securityManagerConfig,
                      com.typesafe.config.Config releaseManagerService,
                      com.typesafe.config.Config moderatorConfig) {
            this.securityManagerConfig = securityManagerConfig;
            this.releaseManagerService = releaseManagerService;
            this.moderatorConfig = moderatorConfig;
        }

        public static SupportService.Config from(com.typesafe.config.Config typeSafeConfig) {
            return new SupportService.Config(typeSafeConfig.getConfig("securityManager"),
                    typeSafeConfig.getConfig("releaseManager"),
                    typeSafeConfig.getConfig("moderator"));
        }
    }

    public SupportService(SupportService.Config config,
                          PersistenceService persistenceService,
                          NetworkService networkService,
                          ChatService chatService,
                          UserService userService,
                          BondedRolesService bondedRolesService) {
        mediationRequestService = new MediationRequestService(networkService,
                chatService,
                userService,
                bondedRolesService);
        mediatorService = new MediatorService(persistenceService,
                networkService,
                chatService,
                userService,
                bondedRolesService);
        securityManagerService = new SecurityManagerService(SecurityManagerService.Config.from(config.getSecurityManagerConfig()),
                networkService,
                userService,
                bondedRolesService);
        releaseManagerService = new ReleaseManagerService(ReleaseManagerService.Config.from(config.getReleaseManagerService()),
                networkService,
                userService,
                bondedRolesService);
        moderatorService = new ModeratorService(ModeratorService.Config.from(config.getModeratorConfig()),
                persistenceService,
                networkService,
                userService,
                bondedRolesService,
                chatService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return mediationRequestService.initialize()
                .thenCompose(result -> mediatorService.initialize())
                .thenCompose(result -> moderatorService.initialize())
                .thenCompose(result -> releaseManagerService.initialize())
                .thenCompose(result -> securityManagerService.initialize());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return mediationRequestService.shutdown()
                .thenCompose(result -> mediatorService.shutdown())
                .thenCompose(result -> moderatorService.shutdown())
                .thenCompose(result -> releaseManagerService.shutdown())
                .thenCompose(result -> securityManagerService.shutdown());
    }
}