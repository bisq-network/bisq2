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

package bisq.desktop.main.content.chat.message_container.list;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.Citation;
import bisq.chat.bisqeasy.BisqEasyOfferMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.reactions.Reaction;
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.main.content.bisq_easy.BisqEasyServiceUtil;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.protobuf.User;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bisq.chat.ChatMessageType.LEAVE;
import static bisq.chat.ChatMessageType.PROTOCOL_LOG_MESSAGE;
import static bisq.desktop.main.content.chat.message_container.ChatMessageContainerView.EDITED_POST_FIX;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ChatMessageListItem<M extends ChatMessage, C extends ChatChannel<M>> implements Comparable<ChatMessageListItem<M, C>> {
    @EqualsAndHashCode.Include
    private final M chatMessage;
    @EqualsAndHashCode.Include
    private final C chatChannel;
    private final String message;
    private final String date;
    private final Optional<Citation> citation;
    private final Optional<UserProfile> senderUserProfile;
    private final String nym;
    private final String nickName;
    private final ReputationScore reputationScore;
    private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
    private final boolean offerAlreadyTaken;
    private final StringProperty messageDeliveryStatusTooltip = new SimpleStringProperty();
    private final ObjectProperty<AwesomeIcon> messageDeliveryStatusIcon = new SimpleObjectProperty<>();
    private final long lastSeen;
    private final String lastSeenAsString;
    @Nullable
    private MessageDeliveryStatus messageDeliveryStatus;
    @Nullable
    private String messageId;
    private Optional<String> messageDeliveryStatusIconColor = Optional.empty();
    private final Set<Pin> mapPins = new HashSet<>();
    private final Set<Pin> statusPins = new HashSet<>();
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BooleanProperty showHighlighted = new SimpleBooleanProperty();
    private Optional<Pin> userReactionsPin = Optional.empty();
    private final ObservableSet<String> happyReaction = new ObservableSet<>();

    public ChatMessageListItem(M chatMessage,
                               C chatChannel,
                               MarketPriceService marketPriceService,
                               UserProfileService userProfileService,
                               ReputationService reputationService,
                               BisqEasyTradeService bisqEasyTradeService,
                               UserIdentityService userIdentityService,
                               NetworkService networkService,
                               Optional<ResendMessageService> resendMessageService) {
        this.chatMessage = chatMessage;
        this.chatChannel = chatChannel;
        this.marketPriceService = marketPriceService;
        this.userIdentityService = userIdentityService;

        if (chatMessage instanceof PrivateChatMessage) {
            senderUserProfile = Optional.of(((PrivateChatMessage) chatMessage).getSenderUserProfile());
        } else {
            senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId());
        }

        citation = chatMessage.getCitation();
        date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()), DateFormat.MEDIUM, DateFormat.SHORT,
                true, " " + Res.get("temporal.at") + " ");

        nym = senderUserProfile.map(UserProfile::getNym).orElse("");
        nickName = senderUserProfile.map(UserProfile::getNickName).orElse("");

        reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore).orElse(ReputationScore.NONE);
        reputationScoreDisplay.setReputationScore(reputationScore);

        if (chatMessage instanceof BisqEasyOfferbookMessage &&
                ((BisqEasyOfferbookMessage) chatMessage).hasBisqEasyOffer()) {
            BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
            message = getLocalizedOfferBookMessage(bisqEasyOfferbookMessage);
            if (bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent()) {
                UserProfile userProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
                NetworkId takerNetworkId = userProfile.getNetworkId();
                BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().get();
                String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
                offerAlreadyTaken = bisqEasyTradeService.tradeExists(tradeId);
            } else {
                offerAlreadyTaken = false;
            }
        } else {
            // Normal chat message or BisqEasyOfferbookMessage without offer
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            offerAlreadyTaken = false;
        }

        lastSeen = senderUserProfile.map(userProfileService::getLastSeen).orElse(-1L);
        lastSeenAsString = TimeFormatter.formatAge(lastSeen);

        if (chatMessage instanceof CommonPublicChatMessage) {
            CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
            userReactionsPin = Optional.ofNullable(commonPublicChatMessage.getUserReactions().addObserver(new HashMapObserver<>() {
                @Override
                public void put(Reaction key, HashSet<String> value) {
                    value.forEach(userId -> {
                            Optional<UserProfile> userProfile = userProfileService.findUserProfile(userId);
                            if (userProfile.isPresent()) {
                                happyReaction.add(userProfile.get().getNickName());
                                System.out.println(key + " reaction from: " + userProfile.get().getNickName());
                            }
                    });
                }

                @Override
                public void putAll(Map<? extends Reaction, ? extends HashSet<String>> map) {
                    map.forEach((key, value) -> {
                        value.forEach(userId -> {
                            Optional<UserProfile> userProfile = userProfileService.findUserProfile(userId);
                            if (userProfile.isPresent()) {
                                happyReaction.add(userProfile.get().getNickName());
                                System.out.println(key + " reaction from: " + userProfile.get().getNickName());
                            }
                        });
                    });
                }

                @Override
                public void remove(Object key) {

                }

                @Override
                public void clear() {

                }
            }));
        }

        mapPins.add(networkService.getMessageDeliveryStatusByMessageId().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String messageId, Observable<MessageDeliveryStatus> value) {
                if (messageId.equals(chatMessage.getId())) {
                    // Delay to avoid ConcurrentModificationException
                    UIThread.runOnNextRenderFrame(() -> {
                        statusPins.add(value.addObserver(status -> {
                            UIThread.run(() -> {
                                messageDeliveryStatus = status;
                                ChatMessageListItem.this.messageId = messageId;
                                if (status != null) {
                                    messageDeliveryStatusIconColor = Optional.empty();
                                    messageDeliveryStatusTooltip.set(Res.get("chat.message.deliveryState." + status.name()));
                                    switch (status) {
                                        case CONNECTING:
                                            // -bisq-mid-grey-20: #808080;
                                            messageDeliveryStatusIconColor = Optional.of("#808080");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.SPINNER);
                                            break;
                                        case SENT:
                                            // -bisq-light-grey-50: #eaeaea;
                                            messageDeliveryStatusIconColor = Optional.of("#eaeaea");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.CIRCLE_ARROW_RIGHT);
                                            break;
                                        case ACK_RECEIVED:
                                            // -bisq2-green-dim-50: #2b5724;
                                            messageDeliveryStatusIconColor = Optional.of("#2b5724");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.OK_SIGN);
                                            break;
                                        case TRY_ADD_TO_MAILBOX:
                                            // -bisq2-yellow-dim-30: #915b15;
                                            messageDeliveryStatusIconColor = Optional.of("#915b15");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.SHARE_SIGN);
                                            break;
                                        case ADDED_TO_MAILBOX:
                                            // -bisq2-yellow-dim-30: #915b15;
                                            messageDeliveryStatusIconColor = Optional.of("#915b15");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.CLOUD_UPLOAD);
                                            break;
                                        case MAILBOX_MSG_RECEIVED:
                                            // -bisq2-green-dim-50: #2b5724;
                                            messageDeliveryStatusIconColor = Optional.of("#2b5724");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.CLOUD_DOWNLOAD);
                                            break;
                                        case FAILED:
                                            if (resendMessageService.map(service -> service.canManuallyResendMessage(messageId)).orElse(false)) {
                                                // -bisq2-yellow: #d0831f;
                                                messageDeliveryStatusIconColor = Optional.of("#d0831f");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.REFRESH);
                                                messageDeliveryStatusTooltip.set(Res.get("chat.message.deliveryState." + status.name()) + " " + Res.get("chat.message.resendMessage"));
                                                break;
                                            } else {
                                                // -bisq2-red: #d23246;
                                                messageDeliveryStatusIconColor = Optional.of("#d23246");
                                                messageDeliveryStatusIcon.set(AwesomeIcon.EXCLAMATION_SIGN);
                                                break;
                                            }
                                    }
                                }
                            });
                        }));
                    });
                }
            }

            @Override
            public void putAll(Map<? extends String, ? extends Observable<MessageDeliveryStatus>> map) {
                map.forEach(this::put);
            }

            @Override
            public void remove(Object key) {
            }

            @Override
            public void clear() {
            }
        }));
    }

    private String getLocalizedOfferBookMessage(BisqEasyOfferbookMessage chatMessage) {
        BisqEasyOffer bisqEasyOffer = chatMessage.getBisqEasyOffer().orElseThrow();
        String fiatPaymentMethods = PaymentMethodSpecFormatter.fromPaymentMethodSpecs(bisqEasyOffer.getQuoteSidePaymentMethodSpecs());
        return BisqEasyServiceUtil.createBasicOfferBookMessage(marketPriceService,
                bisqEasyOffer.getMarket(),
                fiatPaymentMethods,
                bisqEasyOffer.getAmountSpec(),
                bisqEasyOffer.getPriceSpec());
    }

    @Override
    public int compareTo(ChatMessageListItem o) {
        return Comparator.comparingLong(ChatMessage::getDate).compare(this.getChatMessage(), o.getChatMessage());
    }

    public boolean match(String filterString) {
        return filterString == null
                || filterString.isEmpty()
                || StringUtils.containsIgnoreCase(message, filterString)
                || StringUtils.containsIgnoreCase(nym, filterString)
                || StringUtils.containsIgnoreCase(nickName, filterString)
                || StringUtils.containsIgnoreCase(date, filterString);
    }

    public void dispose() {
        mapPins.forEach(Pin::unbind);
        statusPins.forEach(Pin::unbind);
        userReactionsPin.ifPresent(Pin::unbind);
    }

    public boolean hasTradeChatOffer() {
        return chatMessage instanceof BisqEasyOfferMessage &&
                ((BisqEasyOfferMessage) chatMessage).hasBisqEasyOffer();
    }

    public boolean isBisqEasyPublicChatMessageWithOffer() {
        return chatMessage instanceof BisqEasyOfferbookMessage && hasTradeChatOffer();
    }

    public boolean isPublicChannel() {
        return chatChannel instanceof PublicChatChannel;
    }

    public boolean isProtocolLogMessage() {
        return chatMessage.getChatMessageType() == PROTOCOL_LOG_MESSAGE;
    }

    public boolean isLeaveChatMessage() {
        return chatMessage.getChatMessageType() == LEAVE;
    }

    public String getSupportedLanguageCodes(BisqEasyOfferbookMessage chatMessage) {
        String result = getSupportedLanguageCodes(chatMessage, ", ", LanguageRepository::getDisplayLanguage);
        return result.isEmpty() ? "" : Res.get("chat.message.supportedLanguages") + " " + StringUtils.truncate(result, 100);
    }

    public String getSupportedLanguageCodesForTooltip(BisqEasyOfferbookMessage chatMessage) {
        String result = getSupportedLanguageCodes(chatMessage, "\n", LanguageRepository::getDisplayString);
        return result.isEmpty() ? "" : Res.get("chat.message.supportedLanguages") + "\n" + result;
    }

    public boolean isMyMessage() {
        return chatMessage.isMyMessage(userIdentityService);
    }

    public boolean isPeerMessage() {
        return !isMyMessage();
    }

    public boolean isBisqEasyPublicChatMessageWithMyOffer() {
        return isBisqEasyPublicChatMessageWithOffer() && isMyMessage();
    }

    public boolean isBisqEasyPublicChatMessageWithPeerOffer() {
        return isBisqEasyPublicChatMessageWithOffer() && !isMyMessage();
    }

    public boolean isBisqEasyPublicChatMessageWithPeerBuyOffer() {
        return isBisqEasyPublicChatMessageWithPeerOffer() && hasBisqEasyOfferWithDirection(Direction.BUY);
    }

    public boolean isBisqEasyPublicChatMessageWithPeerSellOffer() {
        return isBisqEasyPublicChatMessageWithPeerOffer() && hasBisqEasyOfferWithDirection(Direction.SELL);
    }

    public double getReputationStarCount() {
        return reputationScoreDisplay.getNumberOfStars();
    }

    private boolean hasBisqEasyOfferWithDirection(Direction direction) {
        if (chatMessage instanceof BisqEasyOfferMessage) {
            BisqEasyOfferMessage bisqEasyOfferMessage = (BisqEasyOfferMessage) chatMessage;
            if (bisqEasyOfferMessage.hasBisqEasyOffer() && bisqEasyOfferMessage.getBisqEasyOffer().isPresent()) {
                return bisqEasyOfferMessage.getBisqEasyOffer().get().getDirection() == direction;
            }
        }
        return false;
    }

    private String getSupportedLanguageCodes(BisqEasyOfferbookMessage chatMessage, String separator, Function<String, String> toStringFunction) {
        return chatMessage.getBisqEasyOffer()
                .map(BisqEasyOffer::getSupportedLanguageCodes)
                .map(supportedLanguageCodes -> Joiner.on(separator)
                        .join(supportedLanguageCodes.stream()
                                .map(toStringFunction)
                                .collect(Collectors.toList())))
                .orElse("");
    }
}
