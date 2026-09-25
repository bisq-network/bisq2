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

import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
import bisq.chat.mu_sig.open_trades.MuSigDisputeAgentType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.market.Market;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static bisq.chat.ChatMessageType.CHAT_RULES_WARNING;
import static bisq.chat.ChatMessageType.LEAVE;
import static bisq.chat.ChatMessageType.PROTOCOL_LOG_MESSAGE;
import static bisq.chat.ChatMessageType.TAKE_BISQ_EASY_OFFER;
import static bisq.chat.ChatMessageType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ChatChannelMessageTypeTest {
    private static final UserIdentity MY_USER_IDENTITY = ChatTestUsers.createUserIdentity("me");
    private static final UserProfile PEER = ChatTestUsers.createUserIdentity("peer").getUserProfile();
    private static final String TRADE_ID = "tradeId";
    private static final long DATE = System.currentTimeMillis();

    @ParameterizedTest
    @EnumSource(ChatMessageType.class)
    void twoPartyChannelHoldsOnlyTextAndLeave(ChatMessageType type) {
        TwoPartyPrivateChatChannel channel = new TwoPartyPrivateChatChannel(PEER, MY_USER_IDENTITY, ChatChannelDomain.DISCUSSION);

        assertAddedOnlyIfSupported(EnumSet.of(TEXT, LEAVE), channel, twoPartyMessage(channel, type));
    }

    @ParameterizedTest
    @EnumSource(ChatMessageType.class)
    void bisqEasyTradeChannelHoldsOnlyTypesClientsSend(ChatMessageType type) {
        BisqEasyOpenTradeChannel channel = BisqEasyOpenTradeChannel.create(TRADE_ID,
                mock(BisqEasyOffer.class),
                MY_USER_IDENTITY,
                Set.of(PEER),
                Optional.empty(),
                false);

        assertAddedOnlyIfSupported(EnumSet.of(TEXT, LEAVE, TAKE_BISQ_EASY_OFFER, PROTOCOL_LOG_MESSAGE),
                channel,
                bisqEasyTradeMessage(channel, type));
    }

    @ParameterizedTest
    @EnumSource(ChatMessageType.class)
    void muSigTradeChannelHoldsOnlyTypesClientsSend(ChatMessageType type) {
        MuSigOpenTradeChannel channel = MuSigOpenTradeChannel.create(TRADE_ID,
                MY_USER_IDENTITY,
                Set.of(PEER),
                Optional.empty(),
                Optional.empty(),
                MuSigDisputeAgentType.NONE);

        assertAddedOnlyIfSupported(EnumSet.of(TEXT, LEAVE, PROTOCOL_LOG_MESSAGE), channel, muSigTradeMessage(channel, type));
    }

    @ParameterizedTest
    @EnumSource(ChatMessageType.class)
    void publicChannelHoldsOnlyText(ChatMessageType type) {
        CommonPublicChatChannel channel = new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, SubDomain.DISCUSSION_BISQ);

        assertAddedOnlyIfSupported(EnumSet.of(TEXT), channel, publicMessage(channel, type));
    }

    @ParameterizedTest
    @EnumSource(ChatMessageType.class)
    void offerbookChannelHoldsOnlyText(ChatMessageType type) {
        BisqEasyOfferbookChannel channel = new BisqEasyOfferbookChannel(new Market("BTC", "EUR", "Bitcoin", "Euro"));

        assertAddedOnlyIfSupported(EnumSet.of(TEXT), channel, offerbookMessage(channel, type));
    }

    @ParameterizedTest
    @EnumSource(ChatMessageType.class)
    void twoPartyChannelHoldsAlsoOwnProtocolLogMessages(ChatMessageType type) {
        TwoPartyPrivateChatChannel channel = new TwoPartyPrivateChatChannel(PEER, MY_USER_IDENTITY, ChatChannelDomain.DISCUSSION);
        TwoPartyPrivateChatMessage message = ownTwoPartyMessage(channel, type);

        boolean isHeld = EnumSet.of(TEXT, LEAVE, PROTOCOL_LOG_MESSAGE).contains(type);
        assertEquals(isHeld, channel.addChatMessage(message));
        assertEquals(isHeld, channel.getChatMessages().contains(message));
    }

    @Test
    void storedPrivateChannelLoadsWithOwnProtocolLogMessages() throws UnresolvableProtobufException {
        TwoPartyPrivateChatChannel channel = new TwoPartyPrivateChatChannel(PEER, MY_USER_IDENTITY, ChatChannelDomain.DISCUSSION);
        TwoPartyPrivateChatMessage ownNotice = ownTwoPartyMessage(channel, PROTOCOL_LOG_MESSAGE);
        channel.addChatMessage(ownNotice);

        ChatChannel<? extends ChatMessage> loaded = ChatChannel.fromProto(channel.toProto(false));

        assertEquals(Set.of(ownNotice.getId()), loaded.getChatMessages().stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toSet()));
    }

    @Test
    void storedPrivateChannelLoadsWithoutMessagesOfUnsupportedTypes() throws UnresolvableProtobufException {
        TwoPartyPrivateChatChannel channel = new TwoPartyPrivateChatChannel(PEER, MY_USER_IDENTITY, ChatChannelDomain.DISCUSSION);
        TwoPartyPrivateChatMessage textMessage = twoPartyMessage(channel, TEXT);
        channel.addChatMessage(textMessage);
        // Clients before this rule stored received messages of any type
        bisq.chat.protobuf.ChatChannel proto = channel.toProto(false);
        bisq.chat.protobuf.ChatChannel storedProto = proto.toBuilder()
                .setTwoPartyPrivateChatChannel(proto.getTwoPartyPrivateChatChannel().toBuilder()
                        .addChatMessages(twoPartyMessage(channel, CHAT_RULES_WARNING).toValueProto(false)))
                .build();

        ChatChannel<? extends ChatMessage> loaded = ChatChannel.fromProto(storedProto);

        assertEquals(2, storedProto.getTwoPartyPrivateChatChannel().getChatMessagesCount());
        assertEquals(Set.of(textMessage.getId()), loaded.getChatMessages().stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toSet()));
    }

    private static <M extends ChatMessage> void assertAddedOnlyIfSupported(Set<ChatMessageType> supportedTypes,
                                                                          ChatChannel<M> channel,
                                                                          M message) {
        boolean isSupported = supportedTypes.contains(message.getChatMessageType());
        assertEquals(isSupported, message.hasSupportedChatMessageType());
        assertEquals(isSupported, channel.addChatMessage(message));
        assertEquals(isSupported, channel.getChatMessages().contains(message));
    }

    private static TwoPartyPrivateChatMessage twoPartyMessage(TwoPartyPrivateChatChannel channel, ChatMessageType type) {
        return new TwoPartyPrivateChatMessage(StringUtils.createUid(),
                channel.getChatChannelDomain(),
                channel.getId(),
                PEER,
                MY_USER_IDENTITY.getId(),
                MY_USER_IDENTITY.getUserProfile().getNetworkId(),
                "text",
                Optional.empty(),
                DATE,
                false,
                type,
                new HashSet<>());
    }

    // A notice which the client adds itself, e.g. that the peer is banned
    private static TwoPartyPrivateChatMessage ownTwoPartyMessage(TwoPartyPrivateChatChannel channel, ChatMessageType type) {
        return new TwoPartyPrivateChatMessage(StringUtils.createUid(),
                channel.getChatChannelDomain(),
                channel.getId(),
                MY_USER_IDENTITY.getUserProfile(),
                PEER.getId(),
                PEER.getNetworkId(),
                "text",
                Optional.empty(),
                DATE,
                false,
                type,
                new HashSet<>());
    }

    private static BisqEasyOpenTradeMessage bisqEasyTradeMessage(BisqEasyOpenTradeChannel channel, ChatMessageType type) {
        return new BisqEasyOpenTradeMessage(TRADE_ID,
                StringUtils.createUid(),
                channel.getId(),
                PEER,
                MY_USER_IDENTITY.getId(),
                MY_USER_IDENTITY.getUserProfile().getNetworkId(),
                "text",
                Optional.empty(),
                DATE,
                false,
                Optional.empty(),
                type,
                Optional.empty(),
                new HashSet<>());
    }

    private static MuSigOpenTradeMessage muSigTradeMessage(MuSigOpenTradeChannel channel, ChatMessageType type) {
        return new MuSigOpenTradeMessage(TRADE_ID,
                StringUtils.createUid(),
                channel.getId(),
                PEER,
                MY_USER_IDENTITY.getId(),
                MY_USER_IDENTITY.getUserProfile().getNetworkId(),
                "text",
                Optional.empty(),
                DATE,
                false,
                Optional.empty(),
                Optional.empty(),
                type,
                Optional.empty(),
                new HashSet<>());
    }

    private static CommonPublicChatMessage publicMessage(CommonPublicChatChannel channel, ChatMessageType type) {
        return new CommonPublicChatMessage(StringUtils.createUid(),
                channel.getChatChannelDomain(),
                channel.getId(),
                PEER.getId(),
                Optional.of("text"),
                Optional.empty(),
                DATE,
                false,
                type);
    }

    private static BisqEasyOfferbookMessage offerbookMessage(BisqEasyOfferbookChannel channel, ChatMessageType type) {
        return new BisqEasyOfferbookMessage(StringUtils.createUid(),
                channel.getChatChannelDomain(),
                channel.getId(),
                PEER.getId(),
                Optional.empty(),
                Optional.of("text"),
                Optional.empty(),
                DATE,
                false,
                type);
    }
}
