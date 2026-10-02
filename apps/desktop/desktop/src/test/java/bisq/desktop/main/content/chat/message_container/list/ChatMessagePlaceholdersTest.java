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
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.market.MarketRepository;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.util.DateUtils;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The placeholders are objects of the chat message classes, so their constructors apply the rules of chat messages.
 * Each test creates one placeholder with the kind of arguments which the chat message list passes.
 */
class ChatMessagePlaceholdersTest {
    private static final String CHAT_RULES_WARNING_TEXT = "Do not exchange contact information to buy or sell " +
            "Bitcoin outside Bisq to prevent fraud and scams.";
    private static final String EXPIRED_MESSAGES_TEXT = "Older messages were deleted. Messages in this channel " +
            "expire after 10 days.";

    private final UserProfile peer = createUserProfile("peer", 1001);
    private final UserProfile me = createUserProfile("me", 1002);
    private final UserProfile mediator = createUserProfile("mediator", 1003);
    private final UserProfile arbitrator = createUserProfile("arbitrator", 1004);
    // A trade ID is the string of a UUID
    private final String tradeId = StringUtils.createUid();

    @Test
    void chatRulesWarningOfATwoPartyPrivateChatIsCreated() {
        String channelId = TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, peer.getId(), me.getId());

        TwoPartyPrivateChatMessage message = ChatMessagePlaceholders.createTwoPartyChatRulesWarning(
                ChatChannelDomain.DISCUSSION, channelId, peer, me, CHAT_RULES_WARNING_TEXT);

        assertThat(message.getChatMessageType()).isEqualTo(ChatMessageType.CHAT_RULES_WARNING);
        assertThat(message.getText()).contains(CHAT_RULES_WARNING_TEXT);
        assertSortsBeforeEveryMessageOfBisq2(message);
    }

    @Test
    void chatRulesWarningOfABisqEasyTradeChatIsCreated() {
        BisqEasyOpenTradeMessage message = ChatMessagePlaceholders.createBisqEasyTradeChatRulesWarning(
                tradeId, BisqEasyOpenTradeChannel.createId(tradeId), peer, me, Optional.of(mediator),
                CHAT_RULES_WARNING_TEXT);

        assertThat(message.getChatMessageType()).isEqualTo(ChatMessageType.CHAT_RULES_WARNING);
        assertThat(message.getText()).contains(CHAT_RULES_WARNING_TEXT);
        assertSortsBeforeEveryMessageOfBisq2(message);
    }

    @Test
    void chatRulesWarningOfAMuSigTradeChatIsCreated() {
        MuSigOpenTradeMessage message = ChatMessagePlaceholders.createMuSigTradeChatRulesWarning(
                tradeId, MuSigOpenTradeChannel.createId(tradeId), peer, me, Optional.of(mediator),
                Optional.of(arbitrator), CHAT_RULES_WARNING_TEXT);

        assertThat(message.getChatMessageType()).isEqualTo(ChatMessageType.CHAT_RULES_WARNING);
        assertThat(message.getText()).contains(CHAT_RULES_WARNING_TEXT);
        assertSortsBeforeEveryMessageOfBisq2(message);
    }

    @Test
    void expiredMessagesIndicatorOfACommonPublicChannelIsCreated() {
        CommonPublicChatMessage message = ChatMessagePlaceholders.createCommonPublicExpiredMessagesIndicator(
                ChatChannelDomain.DISCUSSION, SubDomain.DISCUSSION_BISQ.getChannelId(), me.getId(),
                EXPIRED_MESSAGES_TEXT);

        assertThat(message.getChatMessageType()).isEqualTo(ChatMessageType.EXPIRED_MESSAGES_INDICATOR);
        assertThat(message.getText()).contains(EXPIRED_MESSAGES_TEXT);
        assertSortsBeforeEveryMessageOfBisq2(message);
    }

    @Test
    void expiredMessagesIndicatorOfAnOfferbookChannelIsCreated() {
        String channelId = new BisqEasyOfferbookChannel(MarketRepository.getUSDBitcoinMarket()).getId();

        BisqEasyOfferbookMessage message = ChatMessagePlaceholders.createBisqEasyOfferbookExpiredMessagesIndicator(
                ChatChannelDomain.BISQ_EASY_OFFERBOOK, channelId, me.getId(), EXPIRED_MESSAGES_TEXT);

        assertThat(message.getChatMessageType()).isEqualTo(ChatMessageType.EXPIRED_MESSAGES_INDICATOR);
        assertThat(message.getText()).contains(EXPIRED_MESSAGES_TEXT);
        assertThat(message.getBisqEasyOffer()).isEmpty();
        assertSortsBeforeEveryMessageOfBisq2(message);
    }

    @Test
    void placeholderDateIsTheOldestDateWhichTheRulesOfChatMessagesAccept() {
        assertThatCode(() -> NetworkDataValidation.validateDate(ChatMessagePlaceholders.PLACEHOLDER_DATE))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> NetworkDataValidation.validateDate(ChatMessagePlaceholders.PLACEHOLDER_DATE - 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // The list sorts its items by the date of the message. No user created a Bisq 2 message before Bisq 2 existed.
    private static void assertSortsBeforeEveryMessageOfBisq2(ChatMessage placeholder) {
        assertThat(placeholder.getDate()).isEqualTo(ChatMessagePlaceholders.PLACEHOLDER_DATE);
        assertThat(placeholder.getDate()).isLessThan(DateUtils.BISQ_2_LAUNCH_DATE.getTime());
    }

    private static UserProfile createUserProfile(String nickName, int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        Address address = Address.from("127.0.0.1", port);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, nickName, proofOfWork, 0, networkId, "", "", "1.0.0");
    }
}
