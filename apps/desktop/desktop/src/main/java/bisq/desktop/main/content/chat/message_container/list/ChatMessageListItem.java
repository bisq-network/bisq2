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

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bisq_easy.BisqEasyServiceUtil;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.Citation;
import bisq.chat.bisq_easy.BisqEasyOfferMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.currency.Market;
import bisq.common.data.Pair;
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.BondedRoleBadge;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.resend.ResendMessageService;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bisq.chat.ChatMessageType.*;
import static bisq.desktop.main.content.chat.message_container.ChatMessageContainerView.EDITED_POST_FIX;
import static com.google.common.base.Preconditions.checkArgument;

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
    @Nullable
    private String messageId;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BooleanProperty showHighlighted = new SimpleBooleanProperty();

    // Delivery status
    private final Set<Pin> mapPins = new HashSet<>();
    private final Set<Pin> statusPins = new HashSet<>();
    private final BooleanProperty shouldShowTryAgain = new SimpleBooleanProperty();
    private final SimpleObjectProperty<Node> messageDeliveryStatusNode = new SimpleObjectProperty<>();
    private final Optional<ResendMessageService> resendMessageService;
    private ImageView successfulDeliveryIcon, connectingDeliveryIcon, pendingDeliveryIcon, addedToMailboxIcon, failedDeliveryIcon;
    private BisqMenuItem tryAgainMenuItem;

    // Reactions
    private final Pin userIdentityPin;
    private final HashMap<Reaction, ReactionItem> userReactions = new HashMap<>();
    private Optional<Pin> userReactionsPin = Optional.empty();

    // Bonded role badge
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BondedRoleBadge bondedRoleBadge = new BondedRoleBadge(false);

    public ChatMessageListItem(M chatMessage,
                               C chatChannel,
                               MarketPriceService marketPriceService,
                               UserProfileService userProfileService,
                               ReputationService reputationService,
                               BisqEasyTradeService bisqEasyTradeService,
                               UserIdentityService userIdentityService,
                               NetworkService networkService,
                               Optional<ResendMessageService> resendMessageService,
                               AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.chatMessage = chatMessage;
        this.chatChannel = chatChannel;
        this.marketPriceService = marketPriceService;
        this.userIdentityService = userIdentityService;
        this.resendMessageService = resendMessageService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;

        if (chatMessage instanceof PrivateChatMessage<?> privateChatMessage) {
            senderUserProfile = Optional.of(userProfileService.getManagedUserProfile(privateChatMessage.getSenderUserProfile()));
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

        if (isBisqEasyPublicChatMessageWithOffer() && chatMessage instanceof BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
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
            message = chatMessage.getTextOrNA() + editPostFix;
            offerAlreadyTaken = false;
        }

        userIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> UIThread.run(this::onUserIdentity));

        createAndAddSubscriptionToUserReactions(userProfileService);
        initializeDeliveryStatusIcons();
        addSubscriptionToMessageDeliveryStatus(networkService);
        setupBondedRoleBadge();
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
        userIdentityPin.unbind();
        bondedRoleBadge.dispose();
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

    public Optional<Pair<String, String>> getBisqEasyOfferAmountAndPriceSpec() {
        if (chatMessage instanceof BisqEasyOfferbookMessage) {
            BisqEasyOffer offer = ((BisqEasyOfferbookMessage) chatMessage).getBisqEasyOffer().orElseThrow();
            AmountSpec amountSpec = offer.getAmountSpec();
            PriceSpec priceSpec = offer.getPriceSpec();
            boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
            Market market = offer.getMarket();
            String quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, hasAmountRange, true);
            String priceSpecAsString = PriceSpecFormatter.getFormattedPriceSpec(priceSpec);
            return Optional.of(new Pair<>(quoteAmountAsString, priceSpecAsString));
        }
        return Optional.empty();
    }

    public List<FiatPaymentMethod> getBisqEasyOfferPaymentMethods() {
        if (chatMessage instanceof BisqEasyOfferbookMessage) {
            BisqEasyOffer offer = ((BisqEasyOfferbookMessage) chatMessage).getBisqEasyOffer().orElseThrow();
            return PaymentMethodSpecUtil.getPaymentMethods(offer.getQuoteSidePaymentMethodSpecs());
        }
        return Collections.emptyList();
    }

    public List<BitcoinPaymentMethod> getBisqEasyOfferSettlementMethods() {
        if (chatMessage instanceof BisqEasyOfferbookMessage) {
            BisqEasyOffer offer = ((BisqEasyOfferbookMessage) chatMessage).getBisqEasyOffer().orElseThrow();
            return PaymentMethodSpecUtil.getPaymentMethods(offer.getBaseSidePaymentMethodSpecs());
        }
        return Collections.emptyList();
    }

    private boolean hasBisqEasyOfferWithDirection(Direction direction) {
        if (chatMessage instanceof BisqEasyOfferMessage bisqEasyOfferMessage) {
            if (bisqEasyOfferMessage.hasBisqEasyOffer() && bisqEasyOfferMessage.getBisqEasyOffer().isPresent()) {
                return bisqEasyOfferMessage.getBisqEasyOffer().get().getDirection() == direction;
            }
        }
        return false;
    }

    private String getSupportedLanguageCodes(BisqEasyOfferbookMessage chatMessage,
                                             String separator,
                                             Function<String, String> toStringFunction) {
        return chatMessage.getBisqEasyOffer()
                .map(BisqEasyOffer::getSupportedLanguageCodes)
                .map(supportedLanguageCodes -> Joiner.on(separator)
                        .join(supportedLanguageCodes.stream()
                                .map(toStringFunction)
                                .collect(Collectors.toList())))
                .orElse("");
    }

    private String getLocalizedOfferBookMessage(BisqEasyOfferbookMessage chatMessage) {
        BisqEasyOffer bisqEasyOffer = chatMessage.getBisqEasyOffer().orElseThrow();
        String btcPaymentMethods = PaymentMethodSpecFormatter.fromPaymentMethodSpecs(bisqEasyOffer.getBaseSidePaymentMethodSpecs());
        String fiatPaymentMethods = PaymentMethodSpecFormatter.fromPaymentMethodSpecs(bisqEasyOffer.getQuoteSidePaymentMethodSpecs());
        return BisqEasyServiceUtil.createBasicOfferBookMessage(marketPriceService,
                bisqEasyOffer.getMarket(),
                btcPaymentMethods,
                fiatPaymentMethods,
                bisqEasyOffer.getAmountSpec(),
                bisqEasyOffer.getPriceSpec());
    }

    private void initializeDeliveryStatusIcons() {
        successfulDeliveryIcon = ImageUtil.getImageViewById("received-check-grey");
        connectingDeliveryIcon = ImageUtil.getImageViewById("connecting-grey");
        pendingDeliveryIcon = ImageUtil.getImageViewById("sent-message-grey");
        addedToMailboxIcon = ImageUtil.getImageViewById("mailbox-grey");
        failedDeliveryIcon = ImageUtil.getImageViewById("undelivered-message-yellow");
        tryAgainMenuItem = new BisqMenuItem("try-again-grey", "try-again-white");
        tryAgainMenuItem.useIconOnly(22);
        tryAgainMenuItem.setTooltip(new BisqTooltip(Res.get("chat.message.resendMessage")));
    }

    private void addSubscriptionToMessageDeliveryStatus(NetworkService networkService) {
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

    private void updateMessageStatus(String messageId, Observable<MessageDeliveryStatus> value) {
        // Delay to avoid ConcurrentModificationException
        UIThread.runOnNextRenderFrame(() -> statusPins.add(value.addObserver(status -> UIThread.run(() -> {
            ChatMessageListItem.this.messageId = messageId;
            boolean shouldShowTryAgain = false;
            if (status != null) {
                Label statusLabel = new Label();
                statusLabel.setTooltip(new BisqTooltip(Res.get("chat.message.deliveryState." + status.name())));
                switch (status) {
                    // Successful delivery
                    case ACK_RECEIVED:
                    case MAILBOX_MSG_RECEIVED:
                        statusLabel.setGraphic(successfulDeliveryIcon);
                        break;
                    // Pending delivery
                    case CONNECTING:
                        statusLabel.setGraphic(connectingDeliveryIcon);
                        break;
                    case SENT:
                    case TRY_ADD_TO_MAILBOX:
                        statusLabel.setGraphic(pendingDeliveryIcon);
                        break;
                    case ADDED_TO_MAILBOX:
                        statusLabel.setGraphic(addedToMailboxIcon);
                        break;
                    case FAILED:
                        statusLabel.setGraphic(failedDeliveryIcon);
                        shouldShowTryAgain = resendMessageService.map(service -> service.canManuallyResendMessage(messageId)).orElse(false);
                        break;
                }
                messageDeliveryStatusNode.set(statusLabel);
            }
            this.shouldShowTryAgain.set(shouldShowTryAgain);
        }))));
    }

    private void createAndAddSubscriptionToUserReactions(UserProfileService userProfileService) {
        if (!chatMessage.canShowReactions()) {
            return;
        }

        // Create all the ReactionItems
        UserProfile selectedUserProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
        Arrays.stream(Reaction.values()).forEach(reaction -> userReactions.put(reaction,
                new ReactionItem(reaction, selectedUserProfile, isMyMessage())));

        // Subscribe to changes
        userReactionsPin = Optional.ofNullable(chatMessage.getChatMessageReactions().addObserver(new CollectionObserver<>() {
            @Override
            public void add(ChatMessageReaction element) {
                Reaction reaction = getReactionFromOrdinal(element.getReactionId());
                UIThread.run(() -> {
                    if (userReactions.containsKey(reaction)) {
                        userProfileService.findUserProfile(element.getUserProfileId())
                                .filter(profile -> !userProfileService.isChatUserIgnored(profile))
                                .ifPresent(profile -> userReactions.get(reaction).addUser(element, profile));
                    }
                });
            }

            @Override
            public void remove(Object element) {
                ChatMessageReaction chatMessageReaction = (ChatMessageReaction) element;
                Reaction reaction = getReactionFromOrdinal(chatMessageReaction.getReactionId());
                UIThread.run(() -> {
                    if (userReactions.containsKey(reaction)) {
                        userProfileService.findUserProfile(chatMessageReaction.getUserProfileId())
                                .ifPresent(profile -> userReactions.get(reaction).removeUser(profile));
                    }
                });
            }

            @Override
            public void clear() {
                UIThread.run(userReactions::clear);
            }
        }));
    }

    private static Reaction getReactionFromOrdinal(int ordinal) {
        checkArgument(ordinal >= 0 && ordinal < Reaction.values().length, "Invalid reaction id: " + ordinal);
        return Reaction.values()[ordinal];
    }

    private void onUserIdentity() {
        UserProfile selectedUserProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
        userReactions.forEach((key, value) -> value.setSelectedUserProfile(selectedUserProfile));
    }

    private void setupBondedRoleBadge() {
        senderUserProfile.ifPresent(userProfile -> {
            Set<BondedRoleType> bondedRoleTypes = authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                    .filter(bondedRole ->
                            (bondedRole.getBondedRoleType() == BondedRoleType.MEDIATOR
                                    || bondedRole.getBondedRoleType() == BondedRoleType.MODERATOR)
                                    && userProfile.getId().equals(bondedRole.getProfileId()))
                    .map(AuthorizedBondedRole::getBondedRoleType)
                    .collect(Collectors.toSet());
            bondedRoleBadge.applyBondedRoleTypes(bondedRoleTypes);
        });
    }
}
