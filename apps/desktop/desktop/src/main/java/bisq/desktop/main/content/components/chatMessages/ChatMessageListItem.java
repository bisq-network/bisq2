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

package bisq.desktop.main.content.components.chatMessages;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.Citation;
import bisq.chat.bisqeasy.BisqEasyOfferMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bisq.chat.ChatMessageType.LEAVE;
import static bisq.chat.ChatMessageType.SYSTEM_MESSAGE;
import static bisq.desktop.main.content.components.chatMessages.ChatMessagesComponent.View.EDITED_POST_FIX;

@Slf4j
@Getter
@EqualsAndHashCode
public final class ChatMessageListItem<M extends ChatMessage, C extends ChatChannel<M>> implements Comparable<ChatMessageListItem<M, C>> {
    private final M chatMessage;
    private final C chatChannel;
    private final String message;
    private final String date;
    private final Optional<Citation> citation;
    private final Optional<UserProfile> senderUserProfile;
    private final String nym;
    private final String nickName;
    @EqualsAndHashCode.Exclude
    private final ReputationScore reputationScore;
    private final boolean canTakeOffer;
    @EqualsAndHashCode.Exclude
    private final StringProperty messageDeliveryStatusTooltip = new SimpleStringProperty();
    @EqualsAndHashCode.Exclude
    private final ObjectProperty<AwesomeIcon> messageDeliveryStatusIcon = new SimpleObjectProperty<>();
    @EqualsAndHashCode.Exclude
    private Optional<String> messageDeliveryStatusIconColor = Optional.empty();
    @EqualsAndHashCode.Exclude
    private final Set<Pin> mapPins = new HashSet<>();
    @EqualsAndHashCode.Exclude
    private final Set<Pin> statusPins = new HashSet<>();

    public ChatMessageListItem(M chatMessage,
                               C chatChannel,
                               UserProfileService userProfileService,
                               ReputationService reputationService,
                               BisqEasyTradeService bisqEasyTradeService,
                               UserIdentityService userIdentityService,
                               NetworkService networkService) {
        this.chatMessage = chatMessage;
        this.chatChannel = chatChannel;

        if (chatMessage instanceof PrivateChatMessage) {
            senderUserProfile = Optional.of(((PrivateChatMessage) chatMessage).getSenderUserProfile());
        } else {
            senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId());
        }
        String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
        message = chatMessage.getText() + editPostFix;
        citation = chatMessage.getCitation();
        date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()), DateFormat.MEDIUM, DateFormat.SHORT,
                true, " " + Res.get("temporal.at") + " ");

        nym = senderUserProfile.map(UserProfile::getNym).orElse("");
        nickName = senderUserProfile.map(UserProfile::getNickName).orElse("");

        reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore).orElse(ReputationScore.NONE);

        if (chatMessage instanceof BisqEasyOfferbookMessage) {
            BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
            if (userIdentityService.getSelectedUserIdentity() != null && bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent()) {
                UserProfile userProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
                NetworkId takerNetworkId = userProfile.getNetworkId();
                BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().get();
                String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
                canTakeOffer = !bisqEasyTradeService.tradeExists(tradeId);
            } else {
                canTakeOffer = false;
            }
        } else {
            canTakeOffer = false;
        }

        mapPins.add(networkService.getMessageDeliveryStatusByMessageId().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, Observable<MessageDeliveryStatus> value) {
                if (key.equals(chatMessage.getId())) {
                    // Delay to avoid ConcurrentModificationException
                    UIThread.runOnNextRenderFrame(() -> {
                        statusPins.add(value.addObserver(status -> {
                            UIThread.run(() -> {
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
                                            // -bisq2-yellow: #d0831f;
                                            messageDeliveryStatusIconColor = Optional.of("#d0831f");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.SHARE_SIGN);
                                            break;
                                        case ADDED_TO_MAILBOX:
                                            // -bisq2-yellow: #d0831f;
                                            messageDeliveryStatusIconColor = Optional.of("#d0831f");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.CLOUD_UPLOAD);
                                            break;
                                        case MAILBOX_MSG_RECEIVED:
                                            // -bisq2-green-dim-50: #2b5724;
                                            messageDeliveryStatusIconColor = Optional.of("#2b5724");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.CLOUD_DOWNLOAD);
                                            break;
                                        case FAILED:
                                            // -bisq2-red: #d02c1f;
                                            messageDeliveryStatusIconColor = Optional.of("#d02c1f");
                                            messageDeliveryStatusIcon.set(AwesomeIcon.EXCLAMATION_SIGN);
                                            break;
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

    public boolean isSystemMessage() {
        return chatMessage.getChatMessageType() == SYSTEM_MESSAGE;
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
