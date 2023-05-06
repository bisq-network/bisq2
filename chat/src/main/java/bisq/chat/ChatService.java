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
import bisq.chat.trade.channel.PrivateTradeChannelService;
import bisq.chat.trade.channel.PublicTradeChannelService;
import bisq.chat.trade.channel.TradeChannelSelectionService;
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
    private final PrivateTwoPartyChatChannelService privateDiscussionChannelService;
    private final PublicTradeChannelService publicTradeChannelService;
    private final CommonPublicChatChannelService publicDiscussionChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final ChannelSelectionService discussionChannelSelectionService;
    private final PrivateTwoPartyChatChannelService privateSupportChannelService;
    private final CommonPublicChatChannelService publicSupportChannelService;
    private final ChannelSelectionService supportChannelSelectionService;
    private final PrivateTwoPartyChatChannelService privateEventsChannelService;
    private final CommonPublicChatChannelService publicEventsChannelService;
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
        privateDiscussionChannelService = new PrivateTwoPartyChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChannelDomain.DISCUSSION);
        publicDiscussionChannelService = new CommonPublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChannelDomain.DISCUSSION,
                List.of(new CommonPublicChatChannel(ChannelDomain.DISCUSSION, "bisq"),
                        new CommonPublicChatChannel(ChannelDomain.DISCUSSION, "bitcoin"),
                        new CommonPublicChatChannel(ChannelDomain.DISCUSSION, "markets"),
                        new CommonPublicChatChannel(ChannelDomain.DISCUSSION, "economy"),
                        new CommonPublicChatChannel(ChannelDomain.DISCUSSION, "offTopic")));

        discussionChannelSelectionService = new ChannelSelectionService(persistenceService,
                privateDiscussionChannelService,
                publicDiscussionChannelService,
                ChannelDomain.DISCUSSION);

        // Events
        privateEventsChannelService = new PrivateTwoPartyChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChannelDomain.EVENTS);
        publicEventsChannelService = new CommonPublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChannelDomain.EVENTS,
                List.of(new CommonPublicChatChannel(ChannelDomain.EVENTS, "conferences"),
                        new CommonPublicChatChannel(ChannelDomain.EVENTS, "meetups"),
                        new CommonPublicChatChannel(ChannelDomain.EVENTS, "podcasts"),
                        new CommonPublicChatChannel(ChannelDomain.EVENTS, "noKyc"),
                        new CommonPublicChatChannel(ChannelDomain.EVENTS, "nodes"),
                        new CommonPublicChatChannel(ChannelDomain.EVENTS, "tradeEvents")));
        eventsChannelSelectionService = new ChannelSelectionService(persistenceService,
                privateEventsChannelService,
                publicEventsChannelService,
                ChannelDomain.EVENTS);

        // Support
        privateSupportChannelService = new PrivateTwoPartyChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChannelDomain.SUPPORT);
        publicSupportChannelService = new CommonPublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChannelDomain.SUPPORT,
                List.of(new CommonPublicChatChannel(ChannelDomain.SUPPORT, "support"),
                        new CommonPublicChatChannel(ChannelDomain.SUPPORT, "questions"),
                        new CommonPublicChatChannel(ChannelDomain.SUPPORT, "reports")));
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