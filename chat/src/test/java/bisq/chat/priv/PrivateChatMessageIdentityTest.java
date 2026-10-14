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

package bisq.chat.priv;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateChatMessageIdentityTest {
    private UserIdentity receiverIdentity;
    private UserProfile sender;
    private TwoPartyPrivateChatChannel channel;

    @BeforeEach
    void setUp() {
        receiverIdentity = createUserIdentity("receiver");
        sender = createUserIdentity("sender").getUserProfile();
        channel = new TwoPartyPrivateChatChannel(sender, receiverIdentity, ChatChannelDomain.DISCUSSION);
    }

    @Test
    void copyDeliveredAgainAfterAReactionIsNotAddedTwice() throws UnresolvableProtobufException {
        TwoPartyPrivateChatMessage stored = createMessage("I paid");
        TwoPartyPrivateChatMessage deliveredAgain = TwoPartyPrivateChatMessage.fromProto(stored.toValueProto(false));
        assertTrue(channel.addChatMessage(stored));

        assertTrue(stored.addPrivateChatMessageReaction(createReaction(stored)));

        assertFalse(channel.addChatMessage(deliveredAgain));
        assertEquals(1, channel.getChatMessages().size());
    }

    @Test
    void reactionsAreNotPartOfEquality() throws UnresolvableProtobufException {
        TwoPartyPrivateChatMessage stored = createMessage("I paid");
        TwoPartyPrivateChatMessage copy = TwoPartyPrivateChatMessage.fromProto(stored.toValueProto(false));

        stored.addPrivateChatMessageReaction(createReaction(stored));

        assertEquals(stored, copy);
        assertEquals(stored.hashCode(), copy.hashCode());
        assertNotEquals(stored, createMessage("I never paid"));
    }

    private TwoPartyPrivateChatMessage createMessage(String text) {
        UserProfile receiver = receiverIdentity.getUserProfile();
        return new TwoPartyPrivateChatMessage("message-1",
                ChatChannelDomain.DISCUSSION,
                channel.getId(),
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                text,
                Optional.empty(),
                1_700_000_000_000L,
                false,
                ChatMessageType.TEXT,
                new HashSet<>());
    }

    // The receiver reacts to the stored message and sends the reaction to the sender.
    private TwoPartyPrivateChatMessageReaction createReaction(TwoPartyPrivateChatMessage message) {
        return new TwoPartyPrivateChatMessageReaction("reaction-1",
                receiverIdentity.getUserProfile(),
                sender.getId(),
                sender.getNetworkId(),
                channel.getId(),
                ChatChannelDomain.DISCUSSION,
                message.getId(),
                0,
                1_700_000_000_000L,
                false);
    }

    private static UserIdentity createUserIdentity(String nickName) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", 1000);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), nickName));
        KeyBundle keyBundle = new KeyBundle(nickName, keyPair, TorKeyGeneration.generateKeyPair(), I2PKeyGeneration.generateKeyPair());
        UserProfile userProfile = new UserProfile(0,
                nickName,
                new ProofOfWork(networkId.getPubKeyHash(), 0, null, 1, new byte[72], 0),
                0,
                networkId,
                "",
                "",
                "");
        return new UserIdentity(new Identity(nickName, networkId, keyBundle), userProfile);
    }
}
