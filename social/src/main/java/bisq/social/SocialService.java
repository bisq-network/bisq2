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

package bisq.social;

import bisq.common.application.ModuleService;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.oracle.ots.OpenTimestampService;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.social.chat.ChatService;
import bisq.social.offer.TradeChatOfferService;
import bisq.social.user.ChatUserService;
import bisq.social.user.reputation.ReputationService;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class SocialService implements ModuleService {
    private final ChatUserService chatUserService;
    private final Config config;
    private final ReputationService reputationService;
    private final ChatService chatService;
    private final TradeChatOfferService tradeChatOfferService;

    @Getter
    @ToString
    public static final class Config {
        private final com.typesafe.config.Config userProfileServiceConfig;

        public Config(com.typesafe.config.Config userProfileServiceConfig) {
            this.userProfileServiceConfig = userProfileServiceConfig;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getConfig("userProfile"));
        }
    }

    public SocialService(Config config,
                         PersistenceService persistenceService,
                         IdentityService identityService,
                         SecurityService securityService,
                         OpenTimestampService openTimestampService,
                         NetworkService networkService) {
        this.config = config;
        chatUserService = new ChatUserService(ChatUserService.Config.from(config.getUserProfileServiceConfig()),
                persistenceService,
                identityService,
                openTimestampService,
                networkService);
        reputationService = new ReputationService(persistenceService, networkService, chatUserService);
        chatService = new ChatService(persistenceService, identityService, securityService.getProofOfWorkService(), networkService, chatUserService);
        tradeChatOfferService = new TradeChatOfferService(networkService, identityService, chatService, persistenceService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ModuleService
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return chatUserService.initialize()
                .thenCompose(result -> reputationService.initialize())
                .thenCompose(result -> chatService.initialize())
                .thenCompose(result -> tradeChatOfferService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }
}