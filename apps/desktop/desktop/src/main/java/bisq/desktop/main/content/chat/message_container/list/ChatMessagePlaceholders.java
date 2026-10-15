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

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.user.profile.UserProfile;

import java.util.HashSet;
import java.util.Optional;

/**
 * Creates the placeholder messages of the chat message list. A placeholder is a list row which is displayed like a
 * message: the chat rules warning of a private chat or a trade chat, and the indicator that older messages of a
 * public channel were removed. The desktop creates a placeholder locally. It is never sent, stored or published.
 */
final class ChatMessagePlaceholders {
    // The date of every placeholder: the oldest date which the rules of chat messages accept
    // (NetworkDataValidation.validateDate). A placeholder is an object of a chat message class, and the constructors
    // of these classes apply the rules of chat messages. So a placeholder needs a date which passes them; the date 0
    // does not. The list sorts its items by the date of the message. No message which passes the rules is older than
    // this date, so the placeholder is the first row of the list.
    static final long PLACEHOLDER_DATE = NetworkDataValidation.BISQ_1_LAUNCH_DATE + 1;

    private ChatMessagePlaceholders() {
    }

    static TwoPartyPrivateChatMessage createTwoPartyChatRulesWarning(ChatChannelDomain chatChannelDomain,
                                                                     String channelId,
                                                                     UserProfile senderUserProfile,
                                                                     UserProfile receiverUserProfile,
                                                                     String text) {
        return new TwoPartyPrivateChatMessage(StringUtils.createUid(),
                chatChannelDomain,
                channelId,
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                Optional.empty(),
                PLACEHOLDER_DATE,
                false,
                ChatMessageType.CHAT_RULES_WARNING,
                new HashSet<>());
    }

    static BisqEasyOpenTradeMessage createBisqEasyTradeChatRulesWarning(String tradeId,
                                                                        String channelId,
                                                                        UserProfile senderUserProfile,
                                                                        UserProfile receiverUserProfile,
                                                                        Optional<UserProfile> mediator,
                                                                        String text) {
        return new BisqEasyOpenTradeMessage(tradeId,
                StringUtils.createUid(),
                channelId,
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                Optional.empty(),
                PLACEHOLDER_DATE,
                false,
                mediator,
                ChatMessageType.CHAT_RULES_WARNING,
                Optional.empty(),
                new HashSet<>());
    }

    static MuSigOpenTradeMessage createMuSigTradeChatRulesWarning(String tradeId,
                                                                  String channelId,
                                                                  UserProfile senderUserProfile,
                                                                  UserProfile receiverUserProfile,
                                                                  Optional<UserProfile> mediator,
                                                                  Optional<UserProfile> arbitrator,
                                                                  String text) {
        return new MuSigOpenTradeMessage(tradeId,
                StringUtils.createUid(),
                channelId,
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                Optional.empty(),
                PLACEHOLDER_DATE,
                false,
                mediator,
                arbitrator,
                ChatMessageType.CHAT_RULES_WARNING,
                Optional.empty(),
                new HashSet<>());
    }

    static CommonPublicChatMessage createCommonPublicExpiredMessagesIndicator(ChatChannelDomain chatChannelDomain,
                                                                              String channelId,
                                                                              String authorUserProfileId,
                                                                              String text) {
        return new CommonPublicChatMessage(
                StringUtils.createUid(),
                chatChannelDomain,
                channelId,
                authorUserProfileId,
                Optional.of(text),
                Optional.empty(),
                PLACEHOLDER_DATE,
                false,
                ChatMessageType.EXPIRED_MESSAGES_INDICATOR
        );
    }

    static BisqEasyOfferbookMessage createBisqEasyOfferbookExpiredMessagesIndicator(ChatChannelDomain chatChannelDomain,
                                                                                    String channelId,
                                                                                    String authorUserProfileId,
                                                                                    String text) {
        return new BisqEasyOfferbookMessage(
                StringUtils.createUid(),
                chatChannelDomain,
                channelId,
                authorUserProfileId,
                Optional.empty(),
                Optional.of(text),
                Optional.empty(),
                PLACEHOLDER_DATE,
                false,
                ChatMessageType.EXPIRED_MESSAGES_INDICATOR
        );
    }
}
