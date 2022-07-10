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
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.social.chat.ChatService;
import bisq.social.offer.TradeChatOfferService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class SocialService implements ModuleService {
    @Getter
    @ToString
    public static final class Config {
        public Config() {
        }

        public static Config from(com.typesafe.config.Config socialConfig) {
            return null;
        }
    }

    private final SocialService.Config config;
    private final ChatService chatService;
    private final TradeChatOfferService tradeChatOfferService;

    public SocialService(SocialService.Config config,
                         PersistenceService persistenceService,
                         IdentityService identityService,
                         SecurityService securityService,
                         NetworkService networkService,
                         UserIdentityService userIdentityService) {
        this.config = config;
        chatService = new ChatService(persistenceService, identityService, securityService.getProofOfWorkService(), networkService, userIdentityService);
        tradeChatOfferService = new TradeChatOfferService(networkService, identityService, chatService, persistenceService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ModuleService
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return chatService.initialize()
                .thenCompose(result -> tradeChatOfferService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }
}