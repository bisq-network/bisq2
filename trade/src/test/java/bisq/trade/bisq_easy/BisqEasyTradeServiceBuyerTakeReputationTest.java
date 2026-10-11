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

package bisq.trade.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceProviderInfo;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.bonded_roles.release.AppType;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.trade.ServiceProvider;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.BASE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The node of a buyer-taker checks the reputation of the seller-maker for the trade amount, for every client, with the
 * same rules as the node of a buyer-maker (see docs/specifications/trade/bisq-easy-seller-reputation-limit.md). The
 * trade amount of the test data is 100 USD, which requires a reputation score of 20 000.
 */
class BisqEasyTradeServiceBuyerTakeReputationTest {
    private MarketPriceService marketPriceService;
    private ReputationService reputationService;
    private BisqEasyTradeService service;

    @BeforeEach
    void setUp() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        marketPriceService = mock(MarketPriceService.class);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        MarketPrice usdMarketPrice = new MarketPrice(PriceQuote.fromFiatPrice(100_000, "USD"),
                System.currentTimeMillis(),
                mock(MarketPriceProviderInfo.class));
        when(marketPriceService.findMarketPrice(any())).thenReturn(Optional.of(usdMarketPrice));
        reputationService = mock(ReputationService.class);
        when(reputationService.getReputationScore(any(String.class))).thenReturn(new ReputationScore(0, 0, 0));
        when(serviceProvider.getUserService().getReputationService()).thenReturn(reputationService);
        service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
    }

    @Test
    void takeAboveTheReputationOfTheSellerIsRefused() {
        BisqEasyOffer sellOffer = offer("offer-id", identity("maker").getNetworkId(), QUOTE_AMOUNT);
        sellerHasScore(sellOffer, 1_000);

        TradeProtocolException exception = assertThrows(TradeProtocolException.class, () -> take(sellOffer));

        assertEquals(TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW, exception.getTradeProtocolFailure());
        assertTrue(service.getTrades().isEmpty());
    }

    @Test
    void takeWhichTheReputationOfTheSellerCoversIsAccepted() {
        BisqEasyOffer sellOffer = offer("offer-id", identity("maker").getNetworkId(), QUOTE_AMOUNT);
        sellerHasScore(sellOffer, 20_000);

        assertDoesNotThrow(() -> take(sellOffer));
    }

    // The same tolerance as in the take offer views of the desktop: 5 % of the score of the seller, at least 200.
    // 19 100 with the tolerance is 20 055.
    @Test
    void takeWhichTheReputationCoversOnlyWithTheToleranceIsAccepted() {
        BisqEasyOffer sellOffer = offer("offer-id", identity("maker").getNetworkId(), QUOTE_AMOUNT);
        sellerHasScore(sellOffer, 19_100);

        assertDoesNotThrow(() -> take(sellOffer));
    }

    @Test
    void withoutMarketPriceTheTakeIsRefused() {
        BisqEasyOffer sellOffer = offer("offer-id", identity("maker").getNetworkId(), QUOTE_AMOUNT);
        sellerHasScore(sellOffer, 20_000);
        when(marketPriceService.findMarketPrice(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> take(sellOffer));
        assertTrue(service.getTrades().isEmpty());
    }

    // The taker sells, so the node of the maker checks the reputation of the taker
    @Test
    void takeOfASellerIsNotChecked() {
        BisqEasyOffer buyOffer = buyOffer();

        assertDoesNotThrow(() -> take(buyOffer));
    }

    private void sellerHasScore(BisqEasyOffer offer, long score) {
        when(reputationService.getReputationScore(offer.getMakersUserProfileId()))
                .thenReturn(new ReputationScore(score, 0, 0));
    }

    private void take(BisqEasyOffer offer) {
        service.takerCreatesProtocol(identity("taker"),
                offer,
                Coin.asBtcFromValue(BASE_AMOUNT),
                Fiat.fromValue(QUOTE_AMOUNT, "USD"),
                offer.getBaseSidePaymentMethodSpecs().get(0),
                offer.getQuoteSidePaymentMethodSpecs().get(0),
                Optional.empty(),
                100_000_0000);
    }

    private static BisqEasyOffer buyOffer() {
        return new BisqEasyOffer("buy-offer-id",
                System.currentTimeMillis(),
                identity("maker").getNetworkId(),
                Direction.BUY,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(QUOTE_AMOUNT),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                1,
                "1.0.0",
                "2.1.13");
    }
}
