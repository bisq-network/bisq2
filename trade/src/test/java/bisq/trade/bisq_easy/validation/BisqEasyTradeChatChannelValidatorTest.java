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

package bisq.trade.bisq_easy.validation;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.channel;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.networkId;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.userProfile;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BisqEasyTradeChatChannelValidatorTest {
    private NetworkId makerNetworkId;
    private NetworkId mediatorNetworkId;
    private BisqEasyOffer offer;
    private BisqEasyTrade trade;

    @BeforeEach
    void setUp() {
        makerNetworkId = networkId("maker");
        mediatorNetworkId = networkId("mediator");
        offer = offer("offer-id", makerNetworkId, QUOTE_AMOUNT);
        Identity takerIdentity = identity("taker");
        trade = buyerAsTakerTrade(contract(offer, takerIdentity.getNetworkId(), Optional.of(userProfile(mediatorNetworkId))), takerIdentity);
    }

    private BisqEasyOpenTradeChannel channelWith(BisqEasyOffer channelOffer, NetworkId peer, Optional<NetworkId> mediator) {
        return channel(trade, channelOffer, userProfile(peer), mediator.map(id -> userProfile(id)));
    }

    @Test
    void channelMatchingTheContractPassesAfterSerialization() throws UnresolvableProtobufException {
        // The peer's copy of the offer arrives serialized.
        BisqEasyOffer receivedOffer = BisqEasyOffer.fromProto(offer.toProto(false));
        BisqEasyOpenTradeChannel channel = channelWith(receivedOffer, makerNetworkId, Optional.of(mediatorNetworkId));

        assertTrue(BisqEasyTradeChatChannelValidator.findMismatch(channel, trade).isEmpty());
        assertDoesNotThrow(() -> BisqEasyTradeChatChannelValidator.validateChatChannel(Optional.of(channel), trade));
    }

    @Test
    void mediatorIsComparedByNetworkId() {
        // Another profile object with the same network ID, as after a nick name change.
        BisqEasyOpenTradeChannel channel = channelWith(offer, makerNetworkId, Optional.of(mediatorNetworkId));

        assertTrue(BisqEasyTradeChatChannelValidator.findMismatch(channel, trade).isEmpty());
    }

    @Test
    void offerWithAnotherAmountDoesNotMatch() {
        BisqEasyOffer manipulatedOffer = offer("offer-id", makerNetworkId, QUOTE_AMOUNT * 2);
        BisqEasyOpenTradeChannel channel = channelWith(manipulatedOffer, makerNetworkId, Optional.of(mediatorNetworkId));

        assertTrue(BisqEasyTradeChatChannelValidator.findMismatch(channel, trade).orElseThrow().contains("offer"));
    }

    @Test
    void anotherMediatorDoesNotMatch() {
        BisqEasyOpenTradeChannel channel = channelWith(offer, makerNetworkId, Optional.of(networkId("other-mediator")));

        assertTrue(BisqEasyTradeChatChannelValidator.findMismatch(channel, trade).orElseThrow().contains("mediator"));
    }

    @Test
    void missingMediatorDoesNotMatch() {
        BisqEasyOpenTradeChannel channel = channelWith(offer, makerNetworkId, Optional.empty());

        assertTrue(BisqEasyTradeChatChannelValidator.findMismatch(channel, trade).orElseThrow().contains("mediator"));
    }

    @Test
    void anotherPeerDoesNotMatch() {
        // The channel shows a high reputation profile while the contract was signed by another one.
        BisqEasyOpenTradeChannel channel = channelWith(offer, networkId("other-maker"), Optional.of(mediatorNetworkId));

        assertTrue(BisqEasyTradeChatChannelValidator.findMismatch(channel, trade).orElseThrow().contains("peer"));
    }

    @Test
    void missingChannelIsRefused() {
        TradeProtocolException exception = assertThrows(TradeProtocolException.class,
                () -> BisqEasyTradeChatChannelValidator.validateChatChannel(Optional.empty(), trade));

        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, exception.getTradeProtocolFailure());
    }

    @Test
    void mismatchingChannelIsRefused() {
        BisqEasyOpenTradeChannel channel = channelWith(offer, networkId("other-maker"), Optional.of(mediatorNetworkId));

        TradeProtocolException exception = assertThrows(TradeProtocolException.class,
                () -> BisqEasyTradeChatChannelValidator.validateChatChannel(Optional.of(channel), trade));

        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, exception.getTradeProtocolFailure());
    }
}
