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

import bisq.chat.channel.*;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class ChatService implements Service {
    private final PrivateTradeChannelService privateTradeChannelService;
    private final PrivateTwoPartyChannelService privateDiscussionChannelService;
    private final PublicTradeChannelService publicTradeChannelService;
    private final PublicChannelService publicDiscussionChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final ChannelSelectionService discussionChannelSelectionService;
    private final PrivateTwoPartyChannelService privateSupportChannelService;
    private final PublicChannelService publicSupportChannelService;
    private final ChannelSelectionService supportChannelSelectionService;
    private final PrivateTwoPartyChannelService privateEventsChannelService;
    private final PublicChannelService publicEventsChannelService;
    private final ChannelSelectionService eventsChannelSelectionService;

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
        privateDiscussionChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService,
                ChannelDomain.DISCUSSION);
        publicDiscussionChannelService = new PublicChannelService(persistenceService,
                networkService,
                userIdentityService,
                ChannelDomain.DISCUSSION,
                List.of(new PublicChannel(ChannelDomain.DISCUSSION, "bisq"),
                        new PublicChannel(ChannelDomain.DISCUSSION, "bitcoin"),
                        new PublicChannel(ChannelDomain.DISCUSSION, "markets"),
                        new PublicChannel(ChannelDomain.DISCUSSION, "economy"),
                        new PublicChannel(ChannelDomain.DISCUSSION, "offTopic")));

        discussionChannelSelectionService = new ChannelSelectionService(persistenceService,
                privateDiscussionChannelService,
                publicDiscussionChannelService,
                ChannelDomain.DISCUSSION);

        // Events
        privateEventsChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService,
                ChannelDomain.EVENTS);
        publicEventsChannelService = new PublicChannelService(persistenceService,
                networkService,
                userIdentityService,
                ChannelDomain.EVENTS,
                List.of(new PublicChannel(ChannelDomain.EVENTS, "conferences"),
                        new PublicChannel(ChannelDomain.EVENTS, "meetups"),
                        new PublicChannel(ChannelDomain.EVENTS, "podcasts"),
                        new PublicChannel(ChannelDomain.EVENTS, "noKyc"),
                        new PublicChannel(ChannelDomain.EVENTS, "nodes"),
                        new PublicChannel(ChannelDomain.EVENTS, "tradeEvents")));
        eventsChannelSelectionService = new ChannelSelectionService(persistenceService,
                privateEventsChannelService,
                publicEventsChannelService,
                ChannelDomain.EVENTS);

        // Support
        privateSupportChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService,
                ChannelDomain.SUPPORT);
        publicSupportChannelService = new PublicChannelService(persistenceService,
                networkService,
                userIdentityService,
                ChannelDomain.SUPPORT,
                List.of(new PublicChannel(ChannelDomain.SUPPORT, "support"),
                        new PublicChannel(ChannelDomain.SUPPORT, "questions"),
                        new PublicChannel(ChannelDomain.SUPPORT, "reports")));
        supportChannelSelectionService = new ChannelSelectionService(persistenceService,
                privateSupportChannelService,
                publicSupportChannelService,
                ChannelDomain.SUPPORT);
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