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

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceProviderInfo;
import bisq.bonded_roles.release.AppType;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.user.reputation.ReputationScore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.BASE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The taker creates the contract with the price specification of the offer, see
 * docs/specifications/trade/bisq-easy-contract-price.md.
 */
class BisqEasyTradeServiceTakeOfferTest {
    @Test
    void takersContractCarriesThePriceSpecOfTheOffer() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        BisqEasyTradeService service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
        FloatPriceSpec priceSpec = new FloatPriceSpec(0.02);
        BisqEasyOffer offer = offer("offer-id", identity("maker").getNetworkId(), QUOTE_AMOUNT, priceSpec);
        // The taker buys, so its node checks the reputation of the seller (bisq-easy-seller-reputation-limit.md)
        MarketPrice usdMarketPrice = new MarketPrice(PriceQuote.fromFiatPrice(60_000, "USD"),
                System.currentTimeMillis(),
                mock(MarketPriceProviderInfo.class));
        when(serviceProvider.getBondedRolesService().getMarketPriceService().findMarketPrice(any()))
                .thenReturn(Optional.of(usdMarketPrice));
        when(serviceProvider.getUserService().getReputationService().getReputationScore(offer.getMakersUserProfileId()))
                .thenReturn(new ReputationScore(1_000_000, 5, 1));

        BisqEasyProtocol protocol = service.takerCreatesProtocol(identity("taker"),
                offer,
                Coin.asBtcFromValue(BASE_AMOUNT),
                Fiat.fromValue(QUOTE_AMOUNT, "USD"),
                offer.getBaseSidePaymentMethodSpecs().get(0),
                offer.getQuoteSidePaymentMethodSpecs().get(0),
                Optional.empty(),
                60_000_0000);

        assertEquals(priceSpec, protocol.getModel().getContract().getPriceSpec());
    }
}
