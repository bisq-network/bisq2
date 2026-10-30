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

package bisq.mu_sig;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
import bisq.trade.mu_sig.MuSigTradeService;

import bisq.user.identity.UserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MusigNotificationService implements Service {
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final ChatNotificationService chatNotificationService;
    private final MuSigTradeService muSigTradeService;
    private final MuSigMediatorService muSigMediatorService;

    @Nullable
    private Pin tradesPin, channelsPin, changedChatNotificationPin;

    @Getter
    private final ObservableSet<ChatNotification> tradeNotifications = new ObservableSet<>();
    // notifications that are not yet associated with an open trade channel
    private final Set<ChatNotification> orphanedNotifications = new CopyOnWriteArraySet<>();

    public MusigNotificationService(MuSigOpenTradeChannelService muSigOpenTradeChannelService,
                                    ChatService chatService,
                                    MuSigTradeService muSigTradeService,
                                    MuSigMediatorService muSigMediatorService) {
        this.muSigOpenTradeChannelService = muSigOpenTradeChannelService;
        this.chatNotificationService = chatService.getChatNotificationService();
        this.muSigTradeService = muSigTradeService;
        this.muSigMediatorService = muSigMediatorService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);

        tradesPin = muSigTradeService.getTradeById().addObserver(this::handleTradesOrTradeChannelsChange);
        channelsPin = muSigOpenTradeChannelService.getChannels().addObserver(this::handleTradesOrTradeChannelsChange);

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");

        if (tradesPin != null) {
            tradesPin.unbind();
            tradesPin = null;
        }

        if (channelsPin != null) {
            channelsPin.unbind();
            channelsPin = null;
        }

        if (changedChatNotificationPin != null) {
            changedChatNotificationPin.unbind();
            changedChatNotificationPin = null;
        }

        return CompletableFuture.completedFuture(true);
    }

    public Stream<ChatNotification> getMusigNotConsumedNotifications() {
        return chatNotificationService.getNotConsumedNotifications().filter(chatNotification ->
                (chatNotification.getChatChannelDomain() == ChatChannelDomain.MU_SIG_OFFERBOOK ||
                        chatNotification.getChatChannelDomain() == ChatChannelDomain.MU_SIG_OPEN_TRADES) &&
                        !isMediatorsNotification(chatNotification)
        );
    }

    public boolean hasMusigNotifications() {
        return getMusigNotConsumedNotifications().findAny().isPresent();
    }

    public boolean isMediatorsNotification(ChatNotification notification) {
        if (notification != null && notification.getChatChannelDomain() == ChatChannelDomain.MU_SIG_OPEN_TRADES) {
            Optional<UserIdentity> myMediatorUserIdentity = muSigMediatorService.findMyMediatorUserIdentity(notification.getMediator());
            return myMediatorUserIdentity.isPresent();
        } else {
            return false;
        }
    }

    private void handleNotification(@Nullable ChatNotification notification) {
        if (notification != null) {
            processNotifications(chatNotificationService.getNotConsumedNotifications(ChatChannelDomain.MU_SIG_OPEN_TRADES));
        }
    }

    // in case a trade or trade channel is added, it could be the destination for the unconsumed notification
    private void handleTradesOrTradeChannelsChange() {
        Set<ChatNotification> allNotifications = new HashSet<>(orphanedNotifications);
        Set<ChatNotification> unconsumedNotifications = chatNotificationService
                .getNotConsumedNotifications(ChatChannelDomain.MU_SIG_OPEN_TRADES)
                .collect(Collectors.toSet());
        allNotifications.addAll(unconsumedNotifications);
        processNotifications(allNotifications.stream());
    }

    // unconsumed notifications that match a newly opened channel are removed from the orphaned notifications
    private void processNotifications(Stream<ChatNotification> notifications) {
        tradeNotifications.setAll(notifications
                .flatMap(notification -> {
                    if (notification.getTradeId()
                            .flatMap(muSigTradeService::findOpenTrade)
                            .filter(trade -> muSigOpenTradeChannelService.findChannelByTradeId(trade.getId()).isPresent())
                            .isPresent()) {
                        orphanedNotifications.remove(notification);
                        return Stream.of(notification);
                    } else {
                        orphanedNotifications.add(notification);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toSet()));
    }
}