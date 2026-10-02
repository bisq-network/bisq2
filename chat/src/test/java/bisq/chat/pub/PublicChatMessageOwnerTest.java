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
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The author of a public chat message and of a reaction owns it (see docs/specifications/network/storage-ownership.md)
class PublicChatMessageOwnerTest {
    private final KeyPair authorKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final String authorProfileId = new PubKey(authorKeyPair.getPublic(), "keyId").getId();

    @Test
    void authorOwnsMessageAndReaction() {
        byte[] authorPubKeyHash = DigestUtil.hash(authorKeyPair.getPublic().getEncoded());

        assertFalse(message(authorProfileId).isDataInvalid());
        assertArrayEquals(authorPubKeyHash, message(authorProfileId).getOwnerPubKeyHash());
        assertFalse(reaction(authorProfileId).isDataInvalid());
        assertArrayEquals(authorPubKeyHash, reaction(authorProfileId).getOwnerPubKeyHash());
    }

    // Earlier versions accept only the profile ID which PubKey.getId() creates. The same hash in upper-case letters is
    // invalid data, so a message or reaction with it cannot be created, and a received one fails at parsing.
    @Test
    void profileIdWhichIsNotTheIdOfAPublicKeyIsInvalidData() {
        String upperCaseProfileId = authorProfileId.toUpperCase(Locale.ROOT);
        bisq.chat.protobuf.ChatMessage proto = message(authorProfileId).toProto(false).toBuilder()
                .setAuthorUserProfileId(upperCaseProfileId)
                .build();

        assertThrows(IllegalArgumentException.class, () -> message(upperCaseProfileId));
        assertThrows(IllegalArgumentException.class, () -> reaction(upperCaseProfileId));
        assertThrows(UnresolvableProtobufException.class, () -> CommonPublicChatMessage.fromProto(proto));
    }

    private static CommonPublicChatMessage message(String authorUserProfileId) {
        return new CommonPublicChatMessage(ChatChannelDomain.DISCUSSION,
                "channelId",
                authorUserProfileId,
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false);
    }

    private static CommonPublicChatMessageReaction reaction(String userProfileId) {
        return new CommonPublicChatMessageReaction("reactionId",
                userProfileId,
                "channelId",
                ChatChannelDomain.DISCUSSION,
                "messageId",
                0,
                System.currentTimeMillis());
    }
}
