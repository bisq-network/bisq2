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

package bisq.http_api.web_socket.domain.trades;

import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.dto.DtoMappings;
import bisq.http_api.push_notification.PushNotificationService;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.user.profile.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static bisq.http_api.web_socket.subscription.Topic.TRADE_PROPERTIES;

@Slf4j
public class TradePropertiesWebSocketService extends BaseWebSocketService {
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final UserProfileService userProfileService;
    private final Optional<PushNotificationService> pushNotificationService;

    @Nullable
    private Pin tradesPin;
    private final Map<String, Set<Pin>> pinsByTradeId = new HashMap<>();

    // Deduplication tracking
    private final Set<String> notifiedPaymentInfo = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> perTradePeerMessageCount = new ConcurrentHashMap<>();
    private final Map<String, Pin> chatChannelPinsByTradeId = new ConcurrentHashMap<>();

    public TradePropertiesWebSocketService(ObjectMapper objectMapper,
                                           SubscriberRepository subscriberRepository,
                                           TradeService tradeService,
                                           BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService,
                                           UserProfileService userProfileService,
                                           Optional<PushNotificationService> pushNotificationService) {
        super(objectMapper, subscriberRepository, TRADE_PROPERTIES);
        bisqEasyTradeService = tradeService.getBisqEasyTradeService();
        this.bisqEasyOpenTradeChannelService = bisqEasyOpenTradeChannelService;
        this.userProfileService = userProfileService;
        this.pushNotificationService = pushNotificationService;
    }


    @Override
    public CompletableFuture<Boolean> initialize() {
        tradesPin = bisqEasyTradeService.getTrades().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyTrade bisqEasyTrade) {
                String tradeId = bisqEasyTrade.getId();
                pinsByTradeId.computeIfAbsent(tradeId, k -> new HashSet<>());
                Set<Pin> pins = pinsByTradeId.get(tradeId);
                pins.add(observeTradeState(bisqEasyTrade, tradeId));
                pins.add(observeInterruptTradeInitiator(bisqEasyTrade, tradeId));
                pins.add(observePaymentAccountData(bisqEasyTrade, tradeId));
                pins.add(observeBitcoinPaymentData(bisqEasyTrade, tradeId));
                pins.add(observePaymentProof(bisqEasyTrade, tradeId));
                pins.add(observeErrorMessage(bisqEasyTrade, tradeId));
                pins.add(observeErrorStackTrace(bisqEasyTrade, tradeId));
                pins.add(observeTradeProtocolFailure(bisqEasyTrade, tradeId));
                pins.add(observePeersErrorMessage(bisqEasyTrade, tradeId));
                pins.add(observePeersErrorStackTrace(bisqEasyTrade, tradeId));
                pins.add(observePeersTradeProtocolFailure(bisqEasyTrade, tradeId));

                // Observe chat messages for this trade
                observeChatMessages(bisqEasyTrade, tradeId);
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyTrade bisqEasyTrade) {
                    String tradeId = bisqEasyTrade.getId();
                    Optional.ofNullable(pinsByTradeId.remove(tradeId))
                            .ifPresent(set -> set.forEach(Pin::unbind));

                    // Clean up chat channel observer
                    Optional.ofNullable(chatChannelPinsByTradeId.remove(tradeId))
                            .ifPresent(Pin::unbind);

                    // Clean up deduplication tracking
                    notifiedPaymentInfo.remove(tradeId);
                    perTradePeerMessageCount.remove(tradeId);
                }
            }

            @Override
            public void clear() {
                pinsByTradeId.values().forEach(set -> set.forEach(Pin::unbind));
                pinsByTradeId.clear();

                chatChannelPinsByTradeId.values().forEach(Pin::unbind);
                chatChannelPinsByTradeId.clear();

                notifiedPaymentInfo.clear();
                perTradePeerMessageCount.clear();
            }
        });
        return CompletableFuture.completedFuture(true);
    }


    private Pin observeTradeState(BisqEasyTrade bisqEasyTrade, String tradeId) {
        // Track if this is the first emission (current value) or a real change
        final boolean[] isFirstEmission = {true};

        return bisqEasyTrade.tradeStateObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.tradeState = Optional.of(DtoMappings.BisqEasyTradeStateMapping.fromBisq2Model(value));
                send(Map.of(tradeId, data));

                // Only send push notification for actual state changes, not initial value
                // This prevents notifying users about their own actions
                if (!isFirstEmission[0]) {
                    handleTradeStateNotification(bisqEasyTrade, tradeId, value);
                }
                isFirstEmission[0] = false;
            }
        });
    }

    private void handleTradeStateNotification(BisqEasyTrade bisqEasyTrade, String tradeId, BisqEasyTradeState state) {
        pushNotificationService.ifPresent(service -> {
            String userProfileId = bisqEasyTrade.getMyIdentity().getId();
            boolean isBuyer = bisqEasyTrade.isBuyer();
            String peerUserName = getPeerUserName(bisqEasyTrade);

            String message;
            boolean isUrgent = true;

            switch (state) {
                // Payment related states
                case BUYER_SENT_FIAT_SENT_CONFIRMATION -> {
                    message = isBuyer
                        ? "You confirmed sending payment to " + peerUserName
                        : peerUserName + " confirmed sending payment to you";
                }
                case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION -> {
                    message = isBuyer
                        ? "Your payment confirmation was received by " + peerUserName
                        : "You received payment confirmation from " + peerUserName;
                }
                case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
                     SELLER_CONFIRMED_FIAT_RECEIPT -> {
                    message = isBuyer
                        ? peerUserName + " confirmed receiving your payment"
                        : "You confirmed receiving payment from " + peerUserName;
                }

                // BTC transfer states
                case SELLER_SENT_BTC_SENT_CONFIRMATION -> {
                    message = isBuyer
                        ? peerUserName + " confirmed sending Bitcoin to you"
                        : "You confirmed sending Bitcoin to " + peerUserName;
                }
                case BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> {
                    message = isBuyer
                        ? "You received Bitcoin confirmation from " + peerUserName
                        : peerUserName + " received your Bitcoin confirmation";
                }

                // Offer taking states
                case TAKER_SENT_TAKE_OFFER_REQUEST -> {
                    message = "Your offer was taken by " + peerUserName;
                }
                case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                     MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> {
                    message = "Your offer was taken by " + peerUserName;
                }

                // Payment account info exchange states
                case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                     TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                     MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> {
                    // Only notify if we haven't already notified for payment info
                    if (notifiedPaymentInfo.add(tradeId)) {
                        message = isBuyer
                            ? "You received payment info from " + peerUserName
                            : "You sent payment info to " + peerUserName;
                    } else {
                        return; // Skip duplicate notification
                    }
                }

                // Terminal states
                case BTC_CONFIRMED -> {
                    message = "Trade completed successfully with " + peerUserName;
                }
                case REJECTED -> {
                    message = "You rejected the trade with " + peerUserName;
                }
                case PEER_REJECTED -> {
                    message = peerUserName + " rejected the trade";
                }
                case CANCELLED -> {
                    message = "You cancelled the trade with " + peerUserName;
                }
                case PEER_CANCELLED -> {
                    message = peerUserName + " cancelled the trade";
                }
                case FAILED -> {
                    message = "Trade with " + peerUserName + " failed";
                }
                case FAILED_AT_PEER -> {
                    message = "Trade with " + peerUserName + " failed at peer";
                }

                default -> {
                    // For other states, send generic notification
                    message = "Trade state changed: " + state.name();
                    isUrgent = false;
                }
            }

            service.sendTradeNotification(userProfileId, tradeId, state.name(), message, isUrgent);
        });
    }

    private Pin observeInterruptTradeInitiator(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.getInterruptTradeInitiator().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.interruptTradeInitiator = Optional.of(DtoMappings.RoleMapping.fromBisq2Model(value));
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observePaymentAccountData(BisqEasyTrade bisqEasyTrade, String tradeId) {
        // Track if this is the first emission (current value) or a real change
        final boolean[] isFirstEmission = {true};

        return bisqEasyTrade.getPaymentAccountData().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.paymentAccountData = Optional.of(value);
                send(Map.of(tradeId, data));

                // Only send push notification for actual changes, not initial value
                // This prevents notifying users about their own actions
                if (!isFirstEmission[0] && notifiedPaymentInfo.add(tradeId)) {
                    pushNotificationService.ifPresent(service -> {
                        String userProfileId = bisqEasyTrade.getMyIdentity().getId();
                        boolean isSeller = !bisqEasyTrade.isBuyer();
                        String peerUserName = getPeerUserName(bisqEasyTrade);

                        String message = isSeller
                            ? "You sent payment info to " + peerUserName
                            : "You received payment info from " + peerUserName;

                        service.sendTradeNotification(userProfileId, tradeId, "PAYMENT_INFO_UPDATED", message, true);
                    });
                }
                isFirstEmission[0] = false;
            }
        });
    }

    private Pin observeBitcoinPaymentData(BisqEasyTrade bisqEasyTrade, String tradeId) {
        // Track if this is the first emission (current value) or a real change
        final boolean[] isFirstEmission = {true};

        return bisqEasyTrade.getBitcoinPaymentData().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.bitcoinPaymentData = Optional.of(value);
                send(Map.of(tradeId, data));

                // Only send push notification for actual changes, not initial value
                // This prevents notifying users about their own actions
                if (!isFirstEmission[0]) {
                    pushNotificationService.ifPresent(service -> {
                        String userProfileId = bisqEasyTrade.getMyIdentity().getId();
                        boolean isBuyer = bisqEasyTrade.isBuyer();
                        String peerUserName = getPeerUserName(bisqEasyTrade);

                        String message = isBuyer
                            ? "You sent Bitcoin address to " + peerUserName
                            : "You received Bitcoin address from " + peerUserName;

                        service.sendTradeNotification(userProfileId, tradeId, "BITCOIN_INFO_UPDATED", message, true);
                    });
                }
                isFirstEmission[0] = false;
            }
        });
    }

    private Pin observePaymentProof(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.getPaymentProof().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.paymentProof = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observeErrorMessage(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.errorMessageObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.errorMessage = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observeErrorStackTrace(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.errorStackTraceObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.errorStackTrace = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observeTradeProtocolFailure(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.tradeProtocolFailureObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.tradeProtocolFailure = Optional.of(DtoMappings.TradeProtocolFailureMapping.fromBisq2Model(value));
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observePeersErrorMessage(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.peersErrorMessageObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.peersErrorMessage = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observePeersErrorStackTrace(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.peersErrorStackTraceObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.peersErrorStackTrace = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }
    private Pin observePeersTradeProtocolFailure(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.peersTradeProtocolFailureObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.peersTradeProtocolFailure = Optional.of(DtoMappings.TradeProtocolFailureMapping.fromBisq2Model(value));
                send(Map.of(tradeId, data));
            }
        });
    }

    private void observeChatMessages(BisqEasyTrade bisqEasyTrade, String tradeId) {
        // Find the chat channel for this trade
        bisqEasyOpenTradeChannelService.findChannel(tradeId).ifPresent(channel -> {
            // Initialize message count
            int initialCount = getUnignoredPeerMessageCount(channel, bisqEasyTrade);
            perTradePeerMessageCount.put(tradeId, initialCount);

            // Observe chat messages
            Pin chatPin = channel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(BisqEasyOpenTradeMessage message) {
                    handleNewChatMessage(bisqEasyTrade, tradeId, channel);
                }

                @Override
                public void remove(Object element) {
                    // Messages are not removed
                }

                @Override
                public void clear() {
                    // Messages are not cleared
                }
            });

            chatChannelPinsByTradeId.put(tradeId, chatPin);
        });
    }

    private void handleNewChatMessage(BisqEasyTrade bisqEasyTrade, String tradeId, BisqEasyOpenTradeChannel channel) {
        int currentCount = getUnignoredPeerMessageCount(channel, bisqEasyTrade);
        int lastCount = perTradePeerMessageCount.getOrDefault(tradeId, 0);

        if (currentCount > lastCount) {
            perTradePeerMessageCount.put(tradeId, currentCount);

            // Send push notification for new chat message
            pushNotificationService.ifPresent(service -> {
                String userProfileId = bisqEasyTrade.getMyIdentity().getId();
                String peerUserName = getPeerUserName(bisqEasyTrade);
                String message = "New message from " + peerUserName;

                service.sendTradeNotification(userProfileId, tradeId, "NEW_CHAT_MESSAGE", message, true);
            });
        }
    }

    private int getUnignoredPeerMessageCount(BisqEasyOpenTradeChannel channel, BisqEasyTrade bisqEasyTrade) {
        Set<String> ignoredUserProfileIds = userProfileService.getIgnoredUserProfileIds();
        String myUserProfileId = bisqEasyTrade.getMyIdentity().getId();

        return (int) channel.getChatMessages().stream()
                .filter(msg -> msg.getChatMessageType() == ChatMessageType.TEXT)
                .filter(msg -> !msg.getAuthorUserProfileId().equals(myUserProfileId))
                .filter(msg -> !ignoredUserProfileIds.contains(msg.getAuthorUserProfileId()))
                .count();
    }

    private String getPeerUserName(BisqEasyTrade bisqEasyTrade) {
        return bisqEasyOpenTradeChannelService.findChannel(bisqEasyTrade.getId())
                .map(channel -> channel.getPeer().getUserName())
                .orElse("Unknown");
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (tradesPin != null) {
            tradesPin.unbind();
            tradesPin = null;
        }
        pinsByTradeId.values().forEach(set -> set.forEach(Pin::unbind));
        pinsByTradeId.clear();

        chatChannelPinsByTradeId.values().forEach(Pin::unbind);
        chatChannelPinsByTradeId.clear();

        notifiedPaymentInfo.clear();
        perTradePeerMessageCount.clear();

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        List<Map<String, TradePropertiesDto>> maps = bisqEasyTradeService.getTrades().stream()
                .map(bisqEasyTrade -> {
                    var data = new TradePropertiesDto();
                    data.tradeState = Optional.ofNullable(DtoMappings.BisqEasyTradeStateMapping.fromBisq2Model(bisqEasyTrade.getTradeState()));
                    data.interruptTradeInitiator = Optional.ofNullable(DtoMappings.RoleMapping.fromBisq2Model(bisqEasyTrade.getInterruptTradeInitiator().get()));
                    data.paymentAccountData = Optional.ofNullable(bisqEasyTrade.getPaymentAccountData().get());
                    data.bitcoinPaymentData = Optional.ofNullable(bisqEasyTrade.getBitcoinPaymentData().get());
                    data.paymentProof = Optional.ofNullable(bisqEasyTrade.getPaymentProof().get());
                    data.errorMessage = Optional.ofNullable(bisqEasyTrade.getErrorMessage());
                    data.errorStackTrace = Optional.ofNullable(bisqEasyTrade.getErrorStackTrace());
                    data.tradeProtocolFailure = Optional.ofNullable(DtoMappings.TradeProtocolFailureMapping.fromBisq2Model(bisqEasyTrade.getTradeProtocolFailure()));
                    data.peersErrorMessage = Optional.ofNullable(bisqEasyTrade.getPeersErrorMessage());
                    data.peersErrorStackTrace = Optional.ofNullable(bisqEasyTrade.getPeersErrorStackTrace());
                    data.peersTradeProtocolFailure = Optional.ofNullable(DtoMappings.TradeProtocolFailureMapping.fromBisq2Model(bisqEasyTrade.getPeersTradeProtocolFailure()));
                    return Map.of(bisqEasyTrade.getId(), data);
                })
                .collect(Collectors.toList());
        return toJson(maps);
    }

    private void send(Map<String, TradePropertiesDto> map) {
        send(Collections.singletonList(map));
    }

    private void send(List<Map<String, TradePropertiesDto>> maps) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        toJson(maps).ifPresent(json -> {
            subscriberRepository.findSubscribers(topic)
                    .ifPresent(subscribers -> subscribers
                            .forEach(subscriber -> send(json, subscriber, ModificationType.REPLACE)));
        });
    }
}
