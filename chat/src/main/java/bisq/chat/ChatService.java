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
import bisq.user.profile.UserProfileService;
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
    private final PublicChatChannelService publicDiscussionChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final ChannelSelectionService discussionChannelSelectionService;
    private final PrivateTwoPartyChannelService privateSupportChannelService;
    private final PublicChatChannelService publicSupportChannelService;
    private final ChannelSelectionService supportChannelSelectionService;
    private final PrivateTwoPartyChannelService privateEventsChannelService;
    private final PublicChatChannelService publicEventsChannelService;
    private final ChannelSelectionService eventsChannelSelectionService;

    public ChatService(PersistenceService persistenceService,
                       ProofOfWorkService proofOfWorkService,
                       NetworkService networkService,
                       UserIdentityService userIdentityService,
                       UserProfileService userProfileService) {

        // Trade
        privateTradeChannelService = new PrivateTradeChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService);
        publicTradeChannelService = new PublicTradeChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService);
        tradeChannelSelectionService = new TradeChannelSelectionService(persistenceService,
                privateTradeChannelService,
                publicTradeChannelService);

        // Discussion
        privateDiscussionChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChannelDomain.DISCUSSION);
        publicDiscussionChannelService = new PublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChannelDomain.DISCUSSION,
                List.of(new PublicChatChannel(ChannelDomain.DISCUSSION, "bisq"),
                        new PublicChatChannel(ChannelDomain.DISCUSSION, "bitcoin"),
                        new PublicChatChannel(ChannelDomain.DISCUSSION, "markets"),
                        new PublicChatChannel(ChannelDomain.DISCUSSION, "economy"),
                        new PublicChatChannel(ChannelDomain.DISCUSSION, "offTopic")));

        discussionChannelSelectionService = new ChannelSelectionService(persistenceService,
                privateDiscussionChannelService,
                publicDiscussionChannelService,
                ChannelDomain.DISCUSSION);

        // Events
        privateEventsChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChannelDomain.EVENTS);
        publicEventsChannelService = new PublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChannelDomain.EVENTS,
                List.of(new PublicChatChannel(ChannelDomain.EVENTS, "conferences"),
                        new PublicChatChannel(ChannelDomain.EVENTS, "meetups"),
                        new PublicChatChannel(ChannelDomain.EVENTS, "podcasts"),
                        new PublicChatChannel(ChannelDomain.EVENTS, "noKyc"),
                        new PublicChatChannel(ChannelDomain.EVENTS, "nodes"),
                        new PublicChatChannel(ChannelDomain.EVENTS, "tradeEvents")));
        eventsChannelSelectionService = new ChannelSelectionService(persistenceService,
                privateEventsChannelService,
                publicEventsChannelService,
                ChannelDomain.EVENTS);

        // Support
        privateSupportChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChannelDomain.SUPPORT);
        publicSupportChannelService = new PublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChannelDomain.SUPPORT,
                List.of(new PublicChatChannel(ChannelDomain.SUPPORT, "support"),
                        new PublicChatChannel(ChannelDomain.SUPPORT, "questions"),
                        new PublicChatChannel(ChannelDomain.SUPPORT, "reports")));
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