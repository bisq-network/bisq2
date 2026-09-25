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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The contract price spec must equal the price spec of our own offer. The own offer check anchors the offer's
 * price spec, the price spec check anchors the contract's.
 */
class BisqEasyTakeOfferRequestPriceSpecTest {
    private static final long OFFER_DATE = System.currentTimeMillis();
    private static final PriceSpec TAKERS_FIXED_PRICE = new FixPriceSpec(PriceQuote.fromFiatPrice(30_000, "USD"));

    private final NetworkId myNetworkId = networkId();
    private final BisqEasyOffer myOffer = offer(new MarketPriceSpec());
    private BisqEasyTakeOfferRequest message;
    private BisqEasyContract takersContract;
    private BisqEasyTakeOfferRequestHandler handler;

    @BeforeEach
    void setUp() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        BisqEasyOfferbookMessage offerbookMessage = mock(BisqEasyOfferbookMessage.class);
        when(offerbookMessage.getBisqEasyOffer()).thenReturn(Optional.of(myOffer));
        when(offerbookMessage.getAuthorUserProfileId()).thenReturn(myOffer.getMakersUserProfileId());
        BisqEasyOfferbookChannel channel = mock(BisqEasyOfferbookChannel.class);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>(Set.of(offerbookMessage)));
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>(Set.of(channel)));

        takersContract = mock(BisqEasyContract.class, RETURNS_DEEP_STUBS);
        message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);

        BisqEasyTrade trade = mock(BisqEasyTrade.class, RETURNS_DEEP_STUBS);
        when(trade.getMyIdentity().getNetworkId()).thenReturn(myNetworkId);
        handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
    }

    @Test
    void verifyRejectsAnotherPriceSpecForOurOffer() throws UnresolvableProtobufException {
        when(takersContract.getOffer()).thenReturn(copyOf(myOffer));
        when(takersContract.getPriceSpec()).thenReturn(TAKERS_FIXED_PRICE);

        assertThrows(IllegalArgumentException.class, () -> handler.verify(message));
        verify(takersContract, never()).getTaker();
    }

    @Test
    void verifyRejectsAnOfferCopyWithAnotherPriceSpec() {
        when(takersContract.getOffer()).thenReturn(offer(TAKERS_FIXED_PRICE));
        when(takersContract.getPriceSpec()).thenReturn(TAKERS_FIXED_PRICE);

        TradeProtocolException exception = assertThrows(TradeProtocolException.class, () -> handler.verify(message));
        assertEquals(TradeProtocolFailure.NO_MATCHING_OFFER_FOUND, exception.getTradeProtocolFailure());
    }

    @Test
    void verifyContinuesWithThePriceSpecOfOurOffer() throws UnresolvableProtobufException {
        when(takersContract.getOffer()).thenReturn(copyOf(myOffer));
        when(takersContract.getPriceSpec()).thenReturn(copyOf(myOffer).getPriceSpec());

        // Control case: the sender check that follows runs against deep stubs and fails.
        assertThrows(IllegalArgumentException.class, () -> handler.verify(message));
        verify(takersContract).getTaker();
    }

    // The taker's copy of our offer, as received from the network
    private static BisqEasyOffer copyOf(BisqEasyOffer offer) throws UnresolvableProtobufException {
        return BisqEasyOffer.fromProto(offer.toProto(false));
    }

    private BisqEasyOffer offer(PriceSpec priceSpec) {
        return new BisqEasyOffer("offer-id",
                OFFER_DATE,
                myNetworkId,
                Direction.SELL,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(100_0000),
                priceSpec,
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                1,
                "1.0.0",
                "2.1.14");
    }

    private static NetworkId networkId() {
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "maker"));
    }
}
