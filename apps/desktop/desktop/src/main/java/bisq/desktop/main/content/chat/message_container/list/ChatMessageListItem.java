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
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
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
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bisq.chat.ChatMessageType.LEAVE;
import static bisq.chat.ChatMessageType.PROTOCOL_LOG_MESSAGE;
import static bisq.desktop.main.content.chat.message_container.ChatMessageContainerView.EDITED_POST_FIX;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ChatMessageListItem<M extends ChatMessage, C extends ChatChannel<M>> implements Comparable<ChatMessageListItem<M, C>> {
    private static final List<Reaction> REACTION_DISPLAY_ORDER = Arrays.asList(Reaction.THUMBS_UP, Reaction.THUMBS_DOWN,
            Reaction.HAPPY, Reaction.LAUGH, Reaction.HEART, Reaction.PARTY);

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
    private final long lastSeen;
    private final String lastSeenAsString;
    @Nullable
    private String messageId;
    private Optional<ResendMessageService> resendMessageService;
    private final Set<Pin> mapPins = new HashSet<>();
    private final Set<Pin> statusPins = new HashSet<>();
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BooleanProperty showHighlighted = new SimpleBooleanProperty();
    private Optional<Pin> userReactionsPin = Optional.empty();
    private final HashMap<Reaction, Set<UserProfile>> userReactions = new HashMap<>();
    private final SimpleObjectProperty<Node> reactionsNode = new SimpleObjectProperty<>();
    private final BooleanProperty shouldShouldTryAgain = new SimpleBooleanProperty();
    private final ImageView successfulDeliveryIcon, pendingDeliveryIcon, failedDeliveryIcon;
    private final Label tryAgainStatus;
    private final SimpleObjectProperty<Node> messageDeliverStatusNode = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Node> tryAgainStatusNode = new SimpleObjectProperty<>();
    @Nullable
    private MessageDeliveryStatus messageDeliveryStatus;

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
        this.resendMessageService = resendMessageService;

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

        if (isBisqEasyPublicChatMessageWithOffer()) {
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

        // TODO: Release all the listeners when destroying this object

        if (chatMessage.canShowReactions()) {
            userReactionsPin = Optional.ofNullable(chatMessage.getChatMessageReactions().addObserver(new CollectionObserver<>() {
                @Override
                public void add(ChatMessageReaction element) {
                    int reactionIdx = element.getReactionId();
                    checkArgument(reactionIdx >= 0 && reactionIdx < Reaction.values().length, "Invalid reaction id: " + reactionIdx);

                    // TODO: Add tooltip with user nickname, label with count, etc
                    Reaction reaction = Reaction.values()[reactionIdx];
                    Optional<UserProfile> userProfile = userProfileService.findUserProfile(element.getUserProfileId());
                    userProfile.ifPresent(profile -> {
                        if (!userReactions.containsKey(reaction)) {
                            userReactions.put(reaction, new HashSet<>());
                        }
                        userReactions.get(reaction).add(profile);
                        log.info("{} reacted with {}", profile.getNickName(), reaction);
                    });

                    setupDisplayReactionsNode();
                    //logReactionsCount();
                }

                @Override
                public void remove(Object element) {
                    ChatMessageReaction chatMessageReaction = (ChatMessageReaction) element;
                    int reactionIdx = chatMessageReaction.getReactionId();
                    checkArgument(reactionIdx >= 0 && reactionIdx < Reaction.values().length, "Invalid reaction id: " + reactionIdx);

                    Reaction reaction = Reaction.values()[reactionIdx];
                    Optional<UserProfile> userProfile = userProfileService.findUserProfile(chatMessageReaction.getUserProfileId());
                    userProfile.ifPresent(profile -> {
                        if (userReactions.containsKey(reaction)) {
                            userReactions.get(reaction).remove(profile);
                        }
                        if (userReactions.containsKey(reaction) && userReactions.get(reaction).isEmpty()) {
                            userReactions.remove(reaction);
                        }
                        log.info("{} removed reaction {}", profile.getNickName(), reaction);
                    });

                    setupDisplayReactionsNode();
                    //logReactionsCount();
                }

                @Override
                public void clear() {
                    userReactions.clear();
                    log.info("Clearing reactions");

                    setupDisplayReactionsNode();
                    //logReactionsCount();
                }
            }));
        }

        successfulDeliveryIcon = ImageUtil.getImageViewById("received-check-grey");
        pendingDeliveryIcon = ImageUtil.getImageViewById("sent-message-grey");
        failedDeliveryIcon = ImageUtil.getImageViewById("undelivered-message-yellow");
        tryAgainStatus = new Label();
        tryAgainStatus.setTooltip(new BisqTooltip(Res.get("chat.message.resendMessage")));
        tryAgainStatus.setGraphic(ImageUtil.getImageViewById("try-again-grey"));
        tryAgainStatus.getStyleClass().add("medium-text");
        tryAgainStatus.setCursor(Cursor.HAND);

        mapPins.add(networkService.getMessageDeliveryStatusByMessageId().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String messageId, Observable<MessageDeliveryStatus> value) {
                if (messageId.equals(chatMessage.getId())) {
                    updateMessageStatus(messageId, value);
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

    private void setupDisplayReactionsNode() {
        HBox reactions = new HBox(5);
        reactions.setAlignment(Pos.BOTTOM_LEFT);
        // TODO: order here should be defined by time when this was added
        REACTION_DISPLAY_ORDER.forEach(reaction -> {
            if (userReactions.containsKey(reaction)) {
                reactions.getChildren().add(new Label("", ImageUtil.getImageViewById(reaction.toString().replace("_", "").toLowerCase())));
            }
        });
        reactionsNode.set(reactions);
    }

    private void logReactionsCount() {
        StringBuilder reactionsCount = new StringBuilder("\n");
        REACTION_DISPLAY_ORDER.forEach(reaction ->
                reactionsCount.append(String.format("%s: %s\n", reaction,
                        userReactions.containsKey(reaction) ? userReactions.get(reaction).size() : 0)));
        log.info(reactionsCount.toString());
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

    private void updateMessageStatus(String messageId, Observable<MessageDeliveryStatus> value) {
        // Delay to avoid ConcurrentModificationException
        UIThread.runOnNextRenderFrame(() -> {
            statusPins.add(value.addObserver(status -> {
                UIThread.run(() -> {
                    messageDeliveryStatus = status;
                    ChatMessageListItem.this.messageId = messageId;
                    boolean shouldShowTryAgain = false;
                    if (status != null) {
                        Label statusLabel = new Label();
                        statusLabel.setTooltip(new BisqTooltip(Res.get("chat.message.deliveryState." + status.name())));
                        statusLabel.getStyleClass().add("medium-text");
                        switch (status) {
                            // Successful delivery
                            case ACK_RECEIVED:
                            case MAILBOX_MSG_RECEIVED:
                                statusLabel.setGraphic(successfulDeliveryIcon);
                                break;
                            // Pending deliver
                            case CONNECTING:
                            case SENT:
                            case TRY_ADD_TO_MAILBOX:
                            case ADDED_TO_MAILBOX:
                                statusLabel.setGraphic(pendingDeliveryIcon);
                                break;
                            // Failed to deliver
                            case FAILED:
                                statusLabel.setGraphic(failedDeliveryIcon);
                                shouldShowTryAgain = resendMessageService.map(service -> service.canManuallyResendMessage(messageId)).orElse(false);
                                break;
                        }
                        messageDeliverStatusNode.set(statusLabel);
                    }
                    shouldShouldTryAgain.set(shouldShowTryAgain);
                });
            }));
        });
    }
}
