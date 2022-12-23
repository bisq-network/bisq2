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

import bisq.chat.channel.ChannelDomain;
import bisq.chat.channel.private_two_party.PrivateTwoPartyChannelService;
import bisq.chat.channel.public_moderated.PublicModeratedChannel;
import bisq.chat.channel.public_moderated.PublicModeratedChannelService;
import bisq.chat.discuss.DiscussionChannelSelectionService;
import bisq.chat.events.EventsChannelSelectionService;
import bisq.chat.support.SupportChannelSelectionService;
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
    private final PublicModeratedChannelService publicDiscussionChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final DiscussionChannelSelectionService discussionChannelSelectionService;
    private final PrivateTwoPartyChannelService privateSupportChannelService;
    private final PublicModeratedChannelService publicSupportChannelService;
    private final SupportChannelSelectionService supportChannelSelectionService;
    private final PrivateTwoPartyChannelService privateEventsChannelService;
    private final PublicModeratedChannelService publicEventsChannelService;
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
        privateDiscussionChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService,
                ChannelDomain.DISCUSSION);
        publicDiscussionChannelService = new PublicModeratedChannelService(persistenceService,
                networkService,
                userIdentityService,
                ChannelDomain.DISCUSSION,
                List.of(new PublicModeratedChannel(ChannelDomain.DISCUSSION, "bisq"),
                        new PublicModeratedChannel(ChannelDomain.DISCUSSION, "bitcoin"),
                        new PublicModeratedChannel(ChannelDomain.DISCUSSION, "markets"),
                        new PublicModeratedChannel(ChannelDomain.DISCUSSION, "economy"),
                        new PublicModeratedChannel(ChannelDomain.DISCUSSION, "offTopic")));

        discussionChannelSelectionService = new DiscussionChannelSelectionService(persistenceService,
                privateDiscussionChannelService,
                publicDiscussionChannelService);

        // Events
        privateEventsChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService,
                ChannelDomain.EVENTS);
        publicEventsChannelService = new PublicModeratedChannelService(persistenceService,
                networkService,
                userIdentityService,
                ChannelDomain.EVENTS,
                List.of(new PublicModeratedChannel(ChannelDomain.EVENTS, "conferences"),
                        new PublicModeratedChannel(ChannelDomain.EVENTS, "meetups"),
                        new PublicModeratedChannel(ChannelDomain.EVENTS, "podcasts"),
                        new PublicModeratedChannel(ChannelDomain.EVENTS, "noKyc"),
                        new PublicModeratedChannel(ChannelDomain.EVENTS, "nodes"),
                        new PublicModeratedChannel(ChannelDomain.EVENTS, "tradeEvents")));
        eventsChannelSelectionService = new EventsChannelSelectionService(persistenceService,
                privateEventsChannelService,
                publicEventsChannelService);

        // Support
        privateSupportChannelService = new PrivateTwoPartyChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService,
                ChannelDomain.SUPPORT);
        publicSupportChannelService = new PublicModeratedChannelService(persistenceService,
                networkService,
                userIdentityService,
                ChannelDomain.SUPPORT,
                List.of(new PublicModeratedChannel(ChannelDomain.SUPPORT, "support"),
                        new PublicModeratedChannel(ChannelDomain.SUPPORT, "questions"),
                        new PublicModeratedChannel(ChannelDomain.SUPPORT, "reports")));
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