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

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.trade.ServiceProvider;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.util.Optional;
import java.util.Set;

import static bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequestValidator.validate;
import static bisq.trade.bisq_easy.protocol.messages.OfferbookTestData.offerbookChannel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyTakeOfferRequestValidatorTest {
    @Test
    void verifyRejectsTakeOfferRequestFromIgnoredTaker() {
        String takersProfileId = "takersProfileId";
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getUserService().getUserProfileService().isChatUserIgnored(takersProfileId))
                .thenReturn(true);
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>());

        BisqEasyContract takersContract = mock(BisqEasyContract.class);
        when(takersContract.getOffer()).thenReturn(mock(BisqEasyOffer.class));
        BisqEasyTakeOfferRequest message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);
        when(message.getSender().getId()).thenReturn(takersProfileId);

        Identity myIdentity = mock(Identity.class);

        TradeProtocolException exception =
                assertThrows(TradeProtocolException.class, () -> validate(serviceProvider, myIdentity, message));
        // The rejection sent to the taker must not disclose that they have been ignored: it is the rejection for an offer
        // which the maker does not have, thrown from the same line
        assertEquals(TradeProtocolFailure.NO_MATCHING_OFFER_FOUND, exception.getTradeProtocolFailure());
        assertFalse(exception.getMessage().toLowerCase().contains("ignor"));

        when(serviceProvider.getUserService().getUserProfileService().isChatUserIgnored(takersProfileId))
                .thenReturn(false);
        TradeProtocolException notFound =
                assertThrows(TradeProtocolException.class, () -> validate(serviceProvider, myIdentity, message));
        assertEquals(notFound.getTradeProtocolFailure(), exception.getTradeProtocolFailure());
        assertEquals(notFound.getMessage(), exception.getMessage());
        assertEquals(notFound.getStackTrace()[0], exception.getStackTrace()[0]);
    }

    @Test
    void verifyRejectsTakeOfferRequestWithOverflowingAmountConversion() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        Market market = MarketRepository.getUSDBitcoinMarket();

        BisqEasyOffer takersOffer = mock(BisqEasyOffer.class);
        when(takersOffer.getId()).thenReturn("offer-id");
        when(takersOffer.getMarket()).thenReturn(market);
        when(takersOffer.getAmountSpec()).thenReturn(new QuoteSideFixedAmountSpec(Long.MAX_VALUE));

        BisqEasyContract takersContract = mock(BisqEasyContract.class, RETURNS_DEEP_STUBS);
        when(takersContract.getOffer()).thenReturn(takersOffer);
        when(takersContract.getBaseSideAmount()).thenReturn(1L);
        when(takersContract.getQuoteSideAmount()).thenReturn(Long.MAX_VALUE);
        // A fixed price of one quote unit per BTC: converting the Long.MAX_VALUE quote amount
        // to the base side multiplies by 10^8 and overflows a long. The contract keeps the offer's price spec.
        FixPriceSpec priceSpec = new FixPriceSpec(PriceQuote.fromFiatPrice(0.0001, "USD"));
        when(takersOffer.getPriceSpec()).thenReturn(priceSpec);
        when(takersContract.getPriceSpec()).thenReturn(priceSpec);

        NetworkId takerNetworkId = mock(NetworkId.class, RETURNS_DEEP_STUBS);
        when(takersContract.getTaker().getNetworkId()).thenReturn(takerNetworkId);

        BisqEasyTakeOfferRequest message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);
        when(message.getSender()).thenReturn(takerNetworkId);

        // The offer is in an offerbook message of ours
        NetworkId myNetworkId = mock(NetworkId.class);
        when(myNetworkId.getId()).thenReturn("maker-profile");
        Identity myIdentity = mock(Identity.class);
        when(myIdentity.getNetworkId()).thenReturn(myNetworkId);
        when(takersOffer.getMakerNetworkId()).thenReturn(myNetworkId);
        BisqEasyOfferbookChannel channel = offerbookChannel("maker-profile", takersOffer);
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>(Set.of(channel)));
        when(serviceProvider.getContractService().arePublicKeysMatching(any(java.security.PublicKey.class), any(java.security.PublicKey.class))).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validate(serviceProvider, myIdentity, message));
        assertTrue(exception.getMessage().contains("take offer request"));
    }

    // A fixed price offer needs no market price, so the maker can have none for the market. The check of the amount
    // then failed with a NullPointerException instead of its own result.
    @Test
    void fixedPriceRequestIsCheckedWithoutMarketPrice() {
        // 100 USD at 100 000 USD/BTC are 100 000 sat
        assertAmountAccepted(fixedPriceRequest(Direction.SELL, 100_000, 1_000_000, 100_000));
        // The taker asks for 20 % more Bitcoin
        assertPriceDeviation(fixedPriceRequest(Direction.SELL, 100_000, 1_000_000, 120_000));
    }

    // The request passed the checks of the amount if validate() fails at the next check: the payment methods, which
    // the request of the fixture does not have
    private static void assertAmountAccepted(Request request) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, request::validate);
        assertTrue(exception.getMessage().startsWith("BaseSidePaymentMethodSpec"), exception.getMessage());
    }

    private static void assertPriceDeviation(Request request) {
        TradeProtocolException exception = assertThrows(TradeProtocolException.class, request::validate);
        assertEquals(TradeProtocolFailure.PRICE_DEVIATION, exception.getTradeProtocolFailure());
    }

    private record Request(ServiceProvider serviceProvider, Identity myIdentity, BisqEasyTakeOfferRequest message) {
        void validate() {
            BisqEasyTakeOfferRequestValidator.validate(serviceProvider, myIdentity, message);
        }
    }

    // The maker has no market price for USD
    private static Request fixedPriceRequest(Direction makersDirection,
                                             double fixedPrice,
                                             long quoteSideAmount,
                                             long baseSideAmount) {
        return request(makersDirection,
                new FixPriceSpec(PriceQuote.fromFiatPrice(fixedPrice, "USD")),
                Optional.empty(),
                quoteSideAmount,
                baseSideAmount);
    }

    // A request for our USD offer with a fixed quote amount, which passes the checks before the checks of the amount.
    // The maker's maximum trade price deviation is 5 %.
    private static Request request(Direction makersDirection,
                                   PriceSpec priceSpec,
                                   Optional<PriceQuote> marketPriceQuote,
                                   long quoteSideAmount,
                                   long baseSideAmount) {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getSettingsService().getMaxTradePriceDeviation().get()).thenReturn(0.05);
        Market market = MarketRepository.getUSDBitcoinMarket();
        ObservableHashMap<Market, MarketPrice> marketPrices = new ObservableHashMap<>();
        marketPriceQuote.ifPresent(quote -> {
            MarketPrice marketPrice = mock(MarketPrice.class);
            when(marketPrice.getPriceQuote()).thenReturn(quote);
            marketPrices.put(market, marketPrice);
        });
        MarketPriceService marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPrices);
        when(marketPriceService.findMarketPrice(market)).thenReturn(Optional.ofNullable(marketPrices.get(market)));

        BisqEasyOffer takersOffer = mock(BisqEasyOffer.class);
        when(takersOffer.getId()).thenReturn("offer-id");
        when(takersOffer.getMarket()).thenReturn(market);
        when(takersOffer.getMakersDirection()).thenReturn(makersDirection);
        when(takersOffer.getAmountSpec()).thenReturn(new QuoteSideFixedAmountSpec(quoteSideAmount));
        when(takersOffer.getPriceSpec()).thenReturn(priceSpec);

        BisqEasyContract takersContract = mock(BisqEasyContract.class, RETURNS_DEEP_STUBS);
        when(takersContract.getOffer()).thenReturn(takersOffer);
        when(takersContract.getPriceSpec()).thenReturn(priceSpec);
        when(takersContract.getQuoteSideAmount()).thenReturn(quoteSideAmount);
        when(takersContract.getBaseSideAmount()).thenReturn(baseSideAmount);
        NetworkId takerNetworkId = mock(NetworkId.class, RETURNS_DEEP_STUBS);
        when(takersContract.getTaker().getNetworkId()).thenReturn(takerNetworkId);

        BisqEasyTakeOfferRequest message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);
        when(message.getSender()).thenReturn(takerNetworkId);

        // The offer is in an offerbook message of ours
        NetworkId myNetworkId = mock(NetworkId.class);
        when(myNetworkId.getId()).thenReturn("maker-profile");
        Identity myIdentity = mock(Identity.class);
        when(myIdentity.getNetworkId()).thenReturn(myNetworkId);
        when(takersOffer.getMakerNetworkId()).thenReturn(myNetworkId);
        BisqEasyOfferbookChannel channel = offerbookChannel("maker-profile", takersOffer);
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>(Set.of(channel)));
        when(serviceProvider.getContractService().arePublicKeysMatching(any(PublicKey.class), any(PublicKey.class)))
                .thenReturn(true);
        return new Request(serviceProvider, myIdentity, message);
    }
}
