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
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageListItemsByIdTest {
    private UserProfile trader;
    private UserProfile mediator;
    private ChatMessageListItemsById<Item> itemsById;

    @BeforeEach
    void setUp() {
        trader = createUserProfile("trader");
        mediator = createUserProfile("mediator");
        itemsById = new ChatMessageListItemsById<>(Item::chatMessage);
    }

    @Test
    void keepsTwoVersionsUnderOneId() {
        itemsById.add(new Item(createMessage("m1", "I paid")));
        itemsById.add(new Item(createMessage("m1", "I never paid")));

        assertTrue(itemsById.contains(createMessage("m1", "I paid")));
        assertTrue(itemsById.contains(createMessage("m1", "I never paid")));
        assertFalse(itemsById.contains(createMessage("m1", "hello")));
        assertFalse(itemsById.contains(createMessage("m2", "I paid")));
    }

    @Test
    void findsDisplayedMessageAfterAReactionWasAdded() {
        BisqEasyOpenTradeMessage message = createMessage("m1", "I paid");
        itemsById.add(new Item(message));

        message.getChatMessageReactions().add(createReaction("m1"));

        assertTrue(itemsById.contains(message));
    }

    @Test
    void findsCopyDeliveredAgainAfterAReactionWasAdded() {
        BisqEasyOpenTradeMessage message = createMessage("m1", "I paid");
        itemsById.add(new Item(message));

        message.getChatMessageReactions().add(createReaction("m1"));

        assertTrue(itemsById.contains(createMessage("m1", "I paid")));
    }

    @Test
    void removingOneVersionKeepsTheOther() {
        Item paid = new Item(createMessage("m1", "I paid"));
        Item neverPaid = new Item(createMessage("m1", "I never paid"));
        itemsById.add(paid);
        itemsById.add(neverPaid);

        assertSame(neverPaid, itemsById.remove(createMessage("m1", "I never paid")).orElseThrow());
        assertTrue(itemsById.contains(createMessage("m1", "I paid")));
        assertFalse(itemsById.contains(createMessage("m1", "I never paid")));
        assertFalse(itemsById.isEmpty());

        assertSame(paid, itemsById.remove(createMessage("m1", "I paid")).orElseThrow());
        assertTrue(itemsById.isEmpty());
    }

    @Test
    void removalOfMessageNotDisplayedFallsBackToTheId() {
        Item paid = new Item(createMessage("m1", "I paid"));
        itemsById.add(paid);

        assertEquals(Optional.empty(), itemsById.remove(createMessage("m2", "hello")));
        assertSame(paid, itemsById.remove(createMessage("m1", "edited")).orElseThrow());
        assertTrue(itemsById.isEmpty());
    }

    @Test
    void clearRemovesAllItems() {
        itemsById.add(new Item(createMessage("m1", "I paid")));

        itemsById.clear();

        assertTrue(itemsById.isEmpty());
        assertFalse(itemsById.contains(createMessage("m1", "I paid")));
    }

    private record Item(BisqEasyOpenTradeMessage chatMessage) {
    }

    private BisqEasyOpenTradeMessage createMessage(String id, String text) {
        return new BisqEasyOpenTradeMessage("trade-1",
                id,
                BisqEasyOpenTradeChannel.createId("trade-1"),
                trader,
                mediator.getId(),
                mediator.getNetworkId(),
                text,
                Optional.empty(),
                1_700_000_000_000L,
                false,
                Optional.of(mediator),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }

    private BisqEasyOpenTradeMessageReaction createReaction(String chatMessageId) {
        return new BisqEasyOpenTradeMessageReaction("reaction-" + chatMessageId,
                mediator,
                trader.getId(),
                trader.getNetworkId(),
                BisqEasyOpenTradeChannel.createId("trade-1"),
                ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
                chatMessageId,
                0,
                1_700_000_000_000L,
                false);
    }

    private static UserProfile createUserProfile(String nickName) {
        Address address = Address.from("127.0.0.1", 1000);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), nickName));
        return new UserProfile(0,
                nickName,
                new ProofOfWork(new byte[20], 0, null, 1, new byte[72], 0),
                0,
                networkId,
                "",
                "",
                "");
    }
}
