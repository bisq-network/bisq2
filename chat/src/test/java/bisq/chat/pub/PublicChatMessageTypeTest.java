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

package bisq.chat.pub;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Clients publish public chat and offerbook messages only as text, so a node does not store, relay or serve a public
// message of another type (see docs/specifications/chat/message-types.md).
class PublicChatMessageTypeTest {
    private final String authorProfileId =
            new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "keyId").getId();

    @ParameterizedTest
    @EnumSource(value = ChatMessageType.class, names = {"TEXT", "EXPIRED_MESSAGES_INDICATOR"},
            mode = EnumSource.Mode.EXCLUDE)
    void messageOfAnotherTypeFailsAtParsing(ChatMessageType type) {
        bisq.chat.protobuf.ChatMessage commonProto = withType(commonMessage(ChatMessageType.TEXT), type);
        bisq.chat.protobuf.ChatMessage offerbookProto = withType(offerbookMessage(ChatMessageType.TEXT), type);

        assertThrows(UnresolvableProtobufException.class, () -> CommonPublicChatMessage.fromProto(commonProto));
        assertThrows(UnresolvableProtobufException.class, () -> BisqEasyOfferbookMessage.fromProto(offerbookProto));
        assertThrows(IllegalArgumentException.class, () -> commonMessage(type));
        assertThrows(IllegalArgumentException.class, () -> offerbookMessage(type));
    }

    @Test
    void textMessageIsParsed() throws UnresolvableProtobufException {
        bisq.chat.protobuf.ChatMessage commonProto = commonMessage(ChatMessageType.TEXT).toProto(false);
        bisq.chat.protobuf.ChatMessage offerbookProto = offerbookMessage(ChatMessageType.TEXT).toProto(false);

        assertEquals(ChatMessageType.TEXT, CommonPublicChatMessage.fromProto(commonProto).getChatMessageType());
        assertEquals(ChatMessageType.TEXT, BisqEasyOfferbookMessage.fromProto(offerbookProto).getChatMessageType());
    }

    // A type which a newer version adds is read as TEXT, so the rule does not reject it.
    @Test
    void typeOfNewerVersionIsReadAsText() throws UnresolvableProtobufException {
        bisq.chat.protobuf.ChatMessage proto = commonMessage(ChatMessageType.TEXT).toProto(false).toBuilder()
                .setChatMessageTypeValue(99)
                .build();

        assertEquals(ChatMessageType.TEXT, CommonPublicChatMessage.fromProto(proto).getChatMessageType());
    }

    // The desktop creates the expired messages indicator for display. The type has no protobuf value.
    @Test
    void expiredMessagesIndicatorCanBeCreated() {
        assertDoesNotThrow(() -> commonMessage(ChatMessageType.EXPIRED_MESSAGES_INDICATOR));
        assertDoesNotThrow(() -> offerbookMessage(ChatMessageType.EXPIRED_MESSAGES_INDICATOR));
    }

    private static bisq.chat.protobuf.ChatMessage withType(PublicChatMessage message, ChatMessageType type) {
        return message.toProto(false).toBuilder()
                .setChatMessageType(type.toProtoEnum())
                .build();
    }

    private CommonPublicChatMessage commonMessage(ChatMessageType type) {
        return new CommonPublicChatMessage(StringUtils.createUid(),
                ChatChannelDomain.DISCUSSION,
                "channelId",
                authorProfileId,
                Optional.of("text"),
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                type);
    }

    private BisqEasyOfferbookMessage offerbookMessage(ChatMessageType type) {
        return new BisqEasyOfferbookMessage(StringUtils.createUid(),
                ChatChannelDomain.BISQ_EASY_OFFERBOOK,
                "channelId",
                authorProfileId,
                Optional.empty(),
                Optional.of("text"),
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                type);
    }
}
