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

package bisq.chat;

import bisq.chat.channel.discuss.DiscussionChannelSelectionService;
import bisq.chat.channel.trade.TradeChannelSelectionService;
import bisq.chat.channel.discuss.priv.PrivateDiscussionChannelService;
import bisq.chat.channel.trade.priv.PrivateTradeChannelService;
import bisq.chat.channel.discuss.pub.PublicDiscussionChannelService;
import bisq.chat.channel.trade.pub.PublicTradeChannelService;
import bisq.common.application.Service;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class ChatService implements Service {
    private final PrivateTradeChannelService privateTradeChannelService;
    private final PrivateDiscussionChannelService privateDiscussionChannelService;
    private final PublicTradeChannelService publicTradeChannelService;
    private final PublicDiscussionChannelService publicDiscussionChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final DiscussionChannelSelectionService discussionChannelSelectionService;

    public ChatService(PersistenceService persistenceService,
                       ProofOfWorkService proofOfWorkService,
                       NetworkService networkService,
                       UserIdentityService userIdentityService) {

        privateTradeChannelService = new PrivateTradeChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);
        publicTradeChannelService = new PublicTradeChannelService(persistenceService,
                networkService,
                userIdentityService);
        tradeChannelSelectionService = new TradeChannelSelectionService(persistenceService,
                privateTradeChannelService,
                publicTradeChannelService);

        privateDiscussionChannelService = new PrivateDiscussionChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);
        publicDiscussionChannelService = new PublicDiscussionChannelService(persistenceService,
                networkService,
                userIdentityService);
        discussionChannelSelectionService = new DiscussionChannelSelectionService(persistenceService,
                privateDiscussionChannelService,
                publicDiscussionChannelService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFutureUtils.allOf(
                privateTradeChannelService.initialize(),
                publicTradeChannelService.initialize(),
                tradeChannelSelectionService.initialize(),
                privateDiscussionChannelService.initialize(),
                publicDiscussionChannelService.initialize(),
                discussionChannelSelectionService.initialize()
        ).thenApply(list -> true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFutureUtils.allOf(
                privateTradeChannelService.shutdown(),
                publicTradeChannelService.shutdown(),
                tradeChannelSelectionService.shutdown(),
                privateDiscussionChannelService.shutdown(),
                publicDiscussionChannelService.shutdown(),
                discussionChannelSelectionService.shutdown()
        ).thenApply(list -> true);
    }

    public void reportUserProfile(UserProfile userProfile, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", userProfile, reason);
    }
}