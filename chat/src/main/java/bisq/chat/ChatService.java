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

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeSelectionService;
import bisq.chat.common.CommonChannelSelectionService;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.chat.priv.PrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.application.Service;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.SystemNotificationService;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static bisq.chat.common.SubDomain.*;

@Slf4j
public class ChatService implements Service {
    private final PersistenceService persistenceService;
    private final NetworkService networkService;
    private final UserService userService;
    private final UserIdentityService userIdentityService;

    @Getter
    private final ChatNotificationService chatNotificationService;
    @Getter
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    @Getter
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    @Getter
    private final Map<ChatChannelDomain, CommonPublicChatChannelService> commonPublicChatChannelServices = new HashMap<>();
    @Getter
    private final Map<ChatChannelDomain, ChatChannelSelectionService> chatChannelSelectionServices = new HashMap<>();
    @Getter
    private final LeavePrivateChatManager leavePrivateChatManager;

    private final Map<ChatChannelDomain, TwoPartyPrivateChatChannelService> twoPartyPrivateChatChannelServices = new HashMap<>();

    public ChatService(PersistenceService persistenceService,
                       NetworkService networkService,
                       UserService userService,
                       SettingsService settingsService,
                       SystemNotificationService systemNotificationService) {
        this.persistenceService = persistenceService;
        this.networkService = networkService;
        this.userService = userService;
        this.userIdentityService = userService.getUserIdentityService();

        chatNotificationService = new ChatNotificationService(persistenceService,
                networkService,
                this,
                systemNotificationService,
                settingsService,
                userIdentityService,
                userService.getUserProfileService());

        // BISQ_EASY
        bisqEasyOfferbookChannelService = new BisqEasyOfferbookChannelService(persistenceService,
                networkService,
                userService);
        chatChannelSelectionServices.put(ChatChannelDomain.BISQ_EASY_OFFERBOOK,
                new BisqEasyOfferbookSelectionService(persistenceService, bisqEasyOfferbookChannelService));

        bisqEasyOpenTradeChannelService = new BisqEasyOpenTradeChannelService(persistenceService,
                networkService,
                userService);
        chatChannelSelectionServices.put(ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
                new BisqEasyOpenTradeSelectionService(persistenceService, bisqEasyOpenTradeChannelService,
                        userIdentityService));

        // DISCUSSION
        addToCommonPublicChatChannelServices(ChatChannelDomain.DISCUSSION,
                List.of(new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, DISCUSSION_BISQ)));

        twoPartyPrivateChatChannelServices.put(ChatChannelDomain.DISCUSSION,
                new TwoPartyPrivateChatChannelService(persistenceService,
                        networkService,
                        userService,
                        ChatChannelDomain.DISCUSSION));
        addToChatChannelSelectionServices(ChatChannelDomain.DISCUSSION);

        // SUPPORT
        addToCommonPublicChatChannelServices(ChatChannelDomain.SUPPORT,
                List.of(new CommonPublicChatChannel(ChatChannelDomain.SUPPORT, SUPPORT_SUPPORT)));
        addToChatChannelSelectionServices(ChatChannelDomain.SUPPORT);

        leavePrivateChatManager = new LeavePrivateChatManager(bisqEasyOpenTradeChannelService,
                twoPartyPrivateChatChannelServices,
                chatChannelSelectionServices,
                chatNotificationService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        List<CompletableFuture<Boolean>> list = new ArrayList<>(List.of(bisqEasyOfferbookChannelService.initialize(),
                bisqEasyOpenTradeChannelService.initialize()));
        list.addAll(commonPublicChatChannelServices.values().stream()
                .map(CommonPublicChatChannelService::initialize)
                .toList());
        list.addAll(getTwoPartyPrivateChatChannelServices()
                .map(PrivateChatChannelService::initialize)
                .toList());
        list.addAll(chatChannelSelectionServices.values().stream()
                .map(ChatChannelSelectionService::initialize)
                .toList());

        list.add(chatNotificationService.initialize());

        return CompletableFutureUtils.allOf(list).thenApply(result -> true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        List<CompletableFuture<Boolean>> list = new ArrayList<>(List.of(bisqEasyOfferbookChannelService.shutdown(),
                bisqEasyOpenTradeChannelService.shutdown()));
        list.addAll(commonPublicChatChannelServices.values().stream()
                .map(CommonPublicChatChannelService::shutdown)
                .toList());
        list.addAll(getTwoPartyPrivateChatChannelServices()
                .map(PrivateChatChannelService::shutdown)
                .toList());
        list.addAll(chatChannelSelectionServices.values().stream()
                .map(ChatChannelSelectionService::shutdown)
                .toList());

        list.add(chatNotificationService.shutdown());

        return CompletableFutureUtils.allOf(list).thenApply(result -> true);
    }

    public Optional<ChatChannelService<?, ?, ?>> findChatChannelService(@Nullable ChatChannel<?> chatChannel) {
        if (chatChannel == null) {
            return Optional.empty();
        }
        if (chatChannel instanceof CommonPublicChatChannel) {
            return Optional.ofNullable(commonPublicChatChannelServices.get(chatChannel.getChatChannelDomain()));
        } else if (chatChannel instanceof TwoPartyPrivateChatChannel) {
            return Optional.ofNullable(twoPartyPrivateChatChannelServices.get(chatChannel.getChatChannelDomain()));
        } else if (chatChannel instanceof BisqEasyOfferbookChannel) {
            return Optional.ofNullable(bisqEasyOfferbookChannelService);
        } else if (chatChannel instanceof BisqEasyOpenTradeChannel) {
            return Optional.ofNullable(bisqEasyOpenTradeChannelService);
        } else {
            throw new RuntimeException("Unexpected chatChannel instance. chatChannel=" + chatChannel);
        }
    }

    public Optional<TwoPartyPrivateChatChannel> createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain chatChannelDomain,
                                                                                          UserProfile peer) {
        return findTwoPartyPrivateChatChannelService(chatChannelDomain).stream()
                .flatMap(twoPartyPrivateChatChannelService ->
                        twoPartyPrivateChatChannelService.findOrCreateChannel(chatChannelDomain, peer).stream()
                                .peek(channel -> getChatChannelSelectionService(chatChannelDomain).selectChannel(channel)))
                .findAny();
    }

    public boolean isIdentityUsed(UserIdentity userIdentity) {
        boolean usedInAnyPrivateChannel = Stream.concat(bisqEasyOpenTradeChannelService.getChannels().stream(),
                        getTwoPartyPrivateChatChannelServices()
                                .flatMap(c -> c.getChannels().stream()))
                .anyMatch(c -> c.getMyUserIdentity().equals(userIdentity));

        boolean usedInAnyMessage = Stream.concat(bisqEasyOfferbookChannelService.getChannels().stream()
                                .flatMap(c -> c.getChatMessages().stream()),
                        commonPublicChatChannelServices.values().stream()
                                .flatMap(c -> c.getChannels().stream())
                                .flatMap(c -> c.getChatMessages().stream())
                )
                .anyMatch(m -> m.getAuthorUserProfileId().equals(userIdentity.getId()));
        return usedInAnyPrivateChannel || usedInAnyMessage;
    }

    public ChatChannelSelectionService getChatChannelSelectionService(ChatChannelDomain chatChannelDomain) {
        switch (chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK -> {
                return getBisqEasyOfferbookChannelSelectionService();
            }
            case BISQ_EASY_OPEN_TRADES -> {
                return getBisqEasyOpenTradesSelectionService();
            }
        }
        return chatChannelSelectionServices.get(chatChannelDomain);
    }

    public BisqEasyOfferbookSelectionService getBisqEasyOfferbookChannelSelectionService() {
        return (BisqEasyOfferbookSelectionService) getChatChannelSelectionServices().get(ChatChannelDomain.BISQ_EASY_OFFERBOOK);
    }

    public BisqEasyOpenTradeSelectionService getBisqEasyOpenTradesSelectionService() {
        return (BisqEasyOpenTradeSelectionService) getChatChannelSelectionServices().get(ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
    }

    private void addToCommonPublicChatChannelServices(ChatChannelDomain chatChannelDomain,
                                                      List<CommonPublicChatChannel> channels) {
        commonPublicChatChannelServices.put(chatChannelDomain,
                new CommonPublicChatChannelService(persistenceService,
                        networkService,
                        userService,
                        chatChannelDomain,
                        channels));
    }

    private void addToChatChannelSelectionServices(ChatChannelDomain chatChannelDomain) {
        Optional<TwoPartyPrivateChatChannelService> privateChatChannelService = findTwoPartyPrivateChatChannelService(chatChannelDomain);
        chatChannelSelectionServices.put(chatChannelDomain,
                new CommonChannelSelectionService(persistenceService,
                        privateChatChannelService,
                        commonPublicChatChannelServices.get(chatChannelDomain),
                        chatChannelDomain,
                        userIdentityService));
    }

    public Optional<TwoPartyPrivateChatChannelService> findTwoPartyPrivateChatChannelService(ChatChannelDomain chatChannelDomain) {
        return Optional.ofNullable(twoPartyPrivateChatChannelServices.get(chatChannelDomain));
    }

    public TwoPartyPrivateChatChannelService getTwoPartyPrivateChatChannelService() {
        return findTwoPartyPrivateChatChannelService(ChatChannelDomain.DISCUSSION).orElseThrow();
    }

    public Stream<TwoPartyPrivateChatChannelService> getTwoPartyPrivateChatChannelServices() {
        return twoPartyPrivateChatChannelServices.values().stream();
    }
}