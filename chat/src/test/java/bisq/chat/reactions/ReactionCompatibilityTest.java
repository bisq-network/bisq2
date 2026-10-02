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

package bisq.chat.reactions;

import bisq.chat.ChatChannelDomain;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static bisq.chat.reactions.ChatMessageReaction.MAX_PUBLIC_REACTION_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactionCompatibilityTest {
    private static final String USER_PROFILE_ID = "0123456789abcdef0123456789abcdef01234567";

    @Test
    void fromOrdinalReturnsEmptyForUnsupportedFutureReaction() {
        assertTrue(Reaction.fromOrdinal(Reaction.PARTY.ordinal()).isPresent());
        assertFalse(Reaction.fromOrdinal(Reaction.values().length).isPresent());
    }

    // Version 2.1.13 parses only the reaction IDs 0 to 5. A node of 2.1.13 which receives a public reaction with a
    // higher ID closes the connection and drops the inventory response which contains it. So nodes of this version
    // must not accept, store or relay such a public reaction.
    @Test
    void receivedPublicReactionWithAReactionIdAboveTheBoundFailsAtParsing() {
        assertThrows(UnresolvableProtobufException.class,
                () -> ChatMessageReaction.fromProto(commonPublicReactionProto(MAX_PUBLIC_REACTION_ID + 1)));
        assertThrows(UnresolvableProtobufException.class,
                () -> ChatMessageReaction.fromProto(offerbookReactionProto(MAX_PUBLIC_REACTION_ID + 1)));
        assertThrows(UnresolvableProtobufException.class,
                () -> ChatMessageReaction.fromProto(commonPublicReactionProto(Integer.MAX_VALUE)));
        assertThrows(UnresolvableProtobufException.class,
                () -> ChatMessageReaction.fromProto(offerbookReactionProto(Integer.MAX_VALUE)));
    }

    @Test
    void publicReactionWithAReactionIdAboveTheBoundCannotBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> commonPublicReaction(MAX_PUBLIC_REACTION_ID + 1));
        assertThrows(IllegalArgumentException.class, () -> offerbookReaction(MAX_PUBLIC_REACTION_ID + 1));
    }

    @Test
    void publicReactionWithTheHighestAllowedReactionIdIsAccepted() throws UnresolvableProtobufException {
        assertEquals(5, MAX_PUBLIC_REACTION_ID);
        assertEquals(MAX_PUBLIC_REACTION_ID,
                ChatMessageReaction.fromProto(commonPublicReactionProto(MAX_PUBLIC_REACTION_ID)).getReactionId());
        assertEquals(MAX_PUBLIC_REACTION_ID,
                ChatMessageReaction.fromProto(offerbookReactionProto(MAX_PUBLIC_REACTION_ID)).getReactionId());
    }

    // The desktop and the API create a public reaction with the ordinal of a Reaction. A reaction which a later
    // version adds to the enum must not get an ID above the bound while the bound exists.
    @Test
    void everyReactionCanBePublishedAsAPublicReaction() {
        for (Reaction reaction : Reaction.values()) {
            assertDoesNotThrow(() -> commonPublicReaction(reaction.ordinal()));
            assertDoesNotThrow(() -> offerbookReaction(reaction.ordinal()));
        }
    }

    @Test
    void commonPublicChatMessageReactionStillRejectsNegativeReactionIds() {
        assertThrows(IllegalArgumentException.class, () -> commonPublicReaction(-1));
        assertThrows(IllegalArgumentException.class, () -> offerbookReaction(-1));
    }

    // Only the receiver gets a private reaction, so a reaction ID of a later version breaks no other node. The
    // receiver ignores a reaction which it does not know.
    @Test
    void privateReactionAllowsFutureReactionIds() throws UnresolvableProtobufException {
        UserProfile sender = createUserProfile("sender", 1001);
        UserProfile receiver = createUserProfile("receiver", 1002);
        int futureReactionId = MAX_PUBLIC_REACTION_ID + 1;
        TwoPartyPrivateChatMessageReaction reaction = new TwoPartyPrivateChatMessageReaction("reaction-id",
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                "channel-id",
                ChatChannelDomain.DISCUSSION,
                "message-id",
                futureReactionId,
                System.currentTimeMillis(),
                false);

        ChatMessageReaction parsed = ChatMessageReaction.fromProto(reaction.toValueProto(false));

        assertEquals(futureReactionId, parsed.getReactionId());
        assertFalse(Reaction.fromOrdinal(parsed.getReactionId()).isPresent());
    }

    private static CommonPublicChatMessageReaction commonPublicReaction(int reactionId) {
        return new CommonPublicChatMessageReaction("reaction-id",
                USER_PROFILE_ID,
                "channel-id",
                ChatChannelDomain.DISCUSSION,
                "message-id",
                reactionId,
                System.currentTimeMillis());
    }

    private static BisqEasyOfferbookMessageReaction offerbookReaction(int reactionId) {
        return new BisqEasyOfferbookMessageReaction("reaction-id",
                USER_PROFILE_ID,
                "channel-id",
                ChatChannelDomain.BISQ_EASY_OFFERBOOK,
                "message-id",
                reactionId,
                System.currentTimeMillis());
    }

    private static bisq.chat.protobuf.ChatMessageReaction commonPublicReactionProto(int reactionId) {
        return reactionProtoBuilder(reactionId, ChatChannelDomain.DISCUSSION)
                .setCommonPublicChatMessageReaction(
                        bisq.chat.protobuf.CommonPublicChatMessageReaction.newBuilder().build())
                .build();
    }

    private static bisq.chat.protobuf.ChatMessageReaction offerbookReactionProto(int reactionId) {
        return reactionProtoBuilder(reactionId, ChatChannelDomain.BISQ_EASY_OFFERBOOK)
                .setBisqEasyOfferbookMessageReaction(
                        bisq.chat.protobuf.BisqEasyOfferbookMessageReaction.newBuilder().build())
                .build();
    }

    private static bisq.chat.protobuf.ChatMessageReaction.Builder reactionProtoBuilder(int reactionId,
                                                                                       ChatChannelDomain domain) {
        return bisq.chat.protobuf.ChatMessageReaction.newBuilder()
                .setId("reaction-id")
                .setUserProfileId(USER_PROFILE_ID)
                .setChatChannelId("channel-id")
                .setChatChannelDomain(domain.toProtoEnum())
                .setChatMessageId("message-id")
                .setReactionId(reactionId)
                .setDate(System.currentTimeMillis());
    }

    private static UserProfile createUserProfile(String nickName, int port) {
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key-" + port);
        Address address = Address.from("127.0.0.1", port);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, nickName, proofOfWork, 0, networkId, "", "", "1.0.0");
    }
}
