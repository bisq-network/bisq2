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

import bisq.chat.discuss.DiscussionChannelSelectionService;
import bisq.chat.discuss.priv.PrivateDiscussionChannelService;
import bisq.chat.discuss.pub.PublicDiscussionChannelService;
import bisq.chat.events.EventsChannelSelectionService;
import bisq.chat.events.priv.PrivateEventsChannelService;
import bisq.chat.events.pub.PublicEventsChannelService;
import bisq.chat.support.SupportChannelSelectionService;
import bisq.chat.support.priv.PrivateSupportChannelService;
import bisq.chat.support.pub.PublicSupportChannelService;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.chat.trade.pub.PublicTradeChannelService;
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
    private final PrivateSupportChannelService privateSupportChannelService;
    private final PublicSupportChannelService publicSupportChannelService;
    private final SupportChannelSelectionService supportChannelSelectionService;
    private final PrivateEventsChannelService privateEventsChannelService;
    private final PublicEventsChannelService publicEventsChannelService;
    private final EventsChannelSelectionService eventsChannelSelectionService;

    public ChatService(PersistenceService persistenceService,
                       ProofOfWorkService proofOfWorkService,
                       NetworkService networkService,
                       UserIdentityService userIdentityService) {

        // Trade
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

        // Discussion
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

        // Events
        privateEventsChannelService = new PrivateEventsChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);
        publicEventsChannelService = new PublicEventsChannelService(persistenceService,
                networkService,
                userIdentityService);
        eventsChannelSelectionService = new EventsChannelSelectionService(persistenceService,
                privateEventsChannelService,
                publicEventsChannelService);

        // Support
        privateSupportChannelService = new PrivateSupportChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);
        publicSupportChannelService = new PublicSupportChannelService(persistenceService,
                networkService,
                userIdentityService);
        supportChannelSelectionService = new SupportChannelSelectionService(persistenceService,
                privateSupportChannelService,
                publicSupportChannelService);
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
                discussionChannelSelectionService.initialize(),

                privateEventsChannelService.initialize(),
                publicEventsChannelService.initialize(),
                eventsChannelSelectionService.initialize(),

                privateSupportChannelService.initialize(),
                publicSupportChannelService.initialize(),
                supportChannelSelectionService.initialize()
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
                discussionChannelSelectionService.shutdown(),

                privateEventsChannelService.shutdown(),
                publicEventsChannelService.shutdown(),
                eventsChannelSelectionService.shutdown(),

                privateSupportChannelService.shutdown(),
                publicSupportChannelService.shutdown(),
                supportChannelSelectionService.shutdown()

        ).thenApply(list -> true);
    }

    public void reportUserProfile(UserProfile userProfile, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", userProfile, reason);
    }
}