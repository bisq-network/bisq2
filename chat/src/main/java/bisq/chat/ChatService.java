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

import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.ChatChannelService;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
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
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;
    private final TwoPartyPrivateChatChannelService bisqEasyTwoPartyPrivateChatChannelService;
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;

    private final CommonPublicChatChannelService discussionPublicChatChannelService;
    private final TwoPartyPrivateChatChannelService discussionTwoPartyPrivateChatChannelService;
    private final ChatChannelSelectionService discussionChatChannelSelectionService;

    private final CommonPublicChatChannelService supportPublicChatChannelService;
    private final TwoPartyPrivateChatChannelService supportTwoPartyPrivateChatChannelService;
    private final ChatChannelSelectionService supportChatChannelSelectionService;

    private final CommonPublicChatChannelService eventsPublicChatChannelService;
    private final TwoPartyPrivateChatChannelService eventsTwoPartyPrivateChatChannelService;
    private final ChatChannelSelectionService eventsChatChannelSelectionService;


    public ChatService(PersistenceService persistenceService,
                       ProofOfWorkService proofOfWorkService,
                       NetworkService networkService,
                       UserIdentityService userIdentityService,
                       UserProfileService userProfileService) {

        // Trade
        bisqEasyPrivateTradeChatChannelService = new BisqEasyPrivateTradeChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService);
        bisqEasyPublicChatChannelService = new BisqEasyPublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService);
        bisqEasyTwoPartyPrivateChatChannelService = new TwoPartyPrivateChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChatChannelDomain.BISQ_EASY);

        bisqEasyChatChannelSelectionService = new BisqEasyChatChannelSelectionService(persistenceService,
                bisqEasyPrivateTradeChatChannelService,
                bisqEasyPublicChatChannelService,
                bisqEasyTwoPartyPrivateChatChannelService);

        // Discussion
        discussionTwoPartyPrivateChatChannelService = new TwoPartyPrivateChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChatChannelDomain.DISCUSSION);
        discussionPublicChatChannelService = new CommonPublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChatChannelDomain.DISCUSSION,
                List.of(new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "bisq"),
                        new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "bitcoin"),
                        new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "markets"),
                        new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "economy"),
                        new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, "offTopic")));

        discussionChatChannelSelectionService = new ChatChannelSelectionService(persistenceService,
                discussionTwoPartyPrivateChatChannelService,
                discussionPublicChatChannelService,
                ChatChannelDomain.DISCUSSION);

        // Events
        eventsTwoPartyPrivateChatChannelService = new TwoPartyPrivateChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChatChannelDomain.EVENTS);
        eventsPublicChatChannelService = new CommonPublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChatChannelDomain.EVENTS,
                List.of(new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "conferences"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "meetups"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "podcasts"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "noKyc"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "nodes"),
                        new CommonPublicChatChannel(ChatChannelDomain.EVENTS, "tradeEvents")));
        eventsChatChannelSelectionService = new ChatChannelSelectionService(persistenceService,
                eventsTwoPartyPrivateChatChannelService,
                eventsPublicChatChannelService,
                ChatChannelDomain.EVENTS);

        // Support
        supportTwoPartyPrivateChatChannelService = new TwoPartyPrivateChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                proofOfWorkService,
                ChatChannelDomain.SUPPORT);
        supportPublicChatChannelService = new CommonPublicChatChannelService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                ChatChannelDomain.SUPPORT,
                List.of(new CommonPublicChatChannel(ChatChannelDomain.SUPPORT, "support"),
                        new CommonPublicChatChannel(ChatChannelDomain.SUPPORT, "questions"),
                        new CommonPublicChatChannel(ChatChannelDomain.SUPPORT, "reports")));
        supportChatChannelSelectionService = new ChatChannelSelectionService(persistenceService,
                supportTwoPartyPrivateChatChannelService,
                supportPublicChatChannelService,
                ChatChannelDomain.SUPPORT);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFutureUtils.allOf(
                bisqEasyPrivateTradeChatChannelService.initialize(),
                bisqEasyPublicChatChannelService.initialize(),
                bisqEasyChatChannelSelectionService.initialize(),

                discussionTwoPartyPrivateChatChannelService.initialize(),
                discussionPublicChatChannelService.initialize(),
                discussionChatChannelSelectionService.initialize(),

                eventsTwoPartyPrivateChatChannelService.initialize(),
                eventsPublicChatChannelService.initialize(),
                eventsChatChannelSelectionService.initialize(),

                supportTwoPartyPrivateChatChannelService.initialize(),
                supportPublicChatChannelService.initialize(),
                supportChatChannelSelectionService.initialize()
        ).thenApply(list -> true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFutureUtils.allOf(
                bisqEasyPrivateTradeChatChannelService.shutdown(),
                bisqEasyPublicChatChannelService.shutdown(),
                bisqEasyChatChannelSelectionService.shutdown(),

                discussionTwoPartyPrivateChatChannelService.shutdown(),
                discussionPublicChatChannelService.shutdown(),
                discussionChatChannelSelectionService.shutdown(),

                eventsTwoPartyPrivateChatChannelService.shutdown(),
                eventsPublicChatChannelService.shutdown(),
                eventsChatChannelSelectionService.shutdown(),

                supportTwoPartyPrivateChatChannelService.shutdown(),
                supportPublicChatChannelService.shutdown(),
                supportChatChannelSelectionService.shutdown()

        ).thenApply(list -> true);
    }

    public ChatChannelService<?, ?, ?> getChatChannelService(ChatChannel<?> chatChannel) {
        if (chatChannel instanceof TwoPartyPrivateChatChannel) {
            return getTwoPartyPrivateChatChannelService(chatChannel.getChatChannelDomain());
        } else if (chatChannel.getChatChannelDomain() != ChatChannelDomain.BISQ_EASY) {
            return getCommonPublicChatChannelService(chatChannel.getChatChannelDomain());
        } else {
            if (chatChannel instanceof BisqEasyPrivateTradeChatChannel) {
                return bisqEasyPrivateTradeChatChannelService;
            } else {
                return bisqEasyPublicChatChannelService;
            }
        }
    }

    public TwoPartyPrivateChatChannelService getTwoPartyPrivateChatChannelService(ChatChannelDomain chatChannelDomain) {
        switch (chatChannelDomain) {
            case BISQ_EASY:
                return bisqEasyTwoPartyPrivateChatChannelService;
            case DISCUSSION:
                return discussionTwoPartyPrivateChatChannelService;
            case EVENTS:
                return eventsTwoPartyPrivateChatChannelService;
            case SUPPORT:
                return supportTwoPartyPrivateChatChannelService;
            default:
                throw new RuntimeException("Unexpected chatChannelDomain");
        }
    }

    public CommonPublicChatChannelService getCommonPublicChatChannelService(ChatChannelDomain chatChannelDomain) {
        switch (chatChannelDomain) {
            case BISQ_EASY:
                throw new RuntimeException("BISQ_EASY does not provide a CommonPublicChatChannelService");
            case DISCUSSION:
                return discussionPublicChatChannelService;
            case EVENTS:
                return eventsPublicChatChannelService;
            case SUPPORT:
                return supportPublicChatChannelService;
            default:
                throw new RuntimeException("Unexpected chatChannelDomain");
        }
    }

    public void reportUserProfile(UserProfile userProfile, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", userProfile, reason);
    }

    public ChatChannelSelectionService getChatChannelSelectionService(ChatChannelDomain chatChannelDomain) {
        switch (chatChannelDomain) {
            case BISQ_EASY:
                return bisqEasyChatChannelSelectionService;
            case DISCUSSION:
                return discussionChatChannelSelectionService;
            case EVENTS:
                return eventsChatChannelSelectionService;
            case SUPPORT:
                return supportChatChannelSelectionService;
            default:
                throw new RuntimeException("Unexpected chatChannelDomain");
        }
    }

    public void createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain chatChannelDomain, UserProfile peer) {
        TwoPartyPrivateChatChannelService chatChannelService = getTwoPartyPrivateChatChannelService(chatChannelDomain);
        chatChannelService.maybeCreateAndAddChannel(chatChannelDomain, peer)
                .ifPresent(channel -> getChatChannelSelectionService(chatChannelDomain).selectChannel(channel));
    }
}