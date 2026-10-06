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

package bisq.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Trades between the released version and this checkout with float and fixed prices, ranges and Lightning, as in the
 * trade matrix of the release test plan. The maker of this checkout checks a take more strictly (see
 * docs/specifications/trade/take-offer-request-validation.md): for a fixed price it allows only the rounding difference
 * instead of its maximum trade price deviation. Both versions must also compute the same contract hash for each type
 * of price, because each side verifies the contract signature of the other side.
 * <p>
 * The taker computes the amounts as the desktop of v2.1.13 does: from a fiat amount it computes the Bitcoin amount; from
 * a Bitcoin amount it computes the fiat amount and truncates it to 0.0001 of the currency.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class TradePricesAcrossVersionsTest {
    // 64 321.12 USD per Bitcoin. Prices and fiat amounts of USD have the precision 4.
    private static final long FIXED_PRICE = 643_211_200;
    // A Lightning invoice without amount, from the examples of BOLT 11
    private static final String LN_INVOICE = "lnbc1pvjluezsp5zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zygspp5qqqsyqcyq5" +
            "rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq9qrs" +
            "gq357wnc5r2ueh7ck6q93dj32dlqnls087fxdwk8qakdyafkq3yap9us6v52vjjsrvywa6rt52cm9r9zqt8r2t7mlcwspyetp5h2tztugp9lfyql";

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("trade-prices-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void oldTakerBuysAtFloatPriceWithinARangeOverLightning() throws Exception {
        ObjectNode priceSpec = floatPrice(0.01);
        String offerId = createOffer(network.newApi, "SELL", rangeAmount(500_000, 2_000_000), priceSpec,
                List.of("LN", "MAIN_CHAIN"));
        long quoteAmount = 1_200_000;
        long baseAmount = baseAmountFor(quoteAmount, Math.round(marketPrice() * 1.01));
        take(network.newApi, network.oldApi, "SELL", offerId, priceSpec, baseAmount, quoteAmount, "LN", LN_INVOICE);
    }

    @Test
    void oldTakerBuysAtFixedPriceEnteringTheBitcoinAmount() throws Exception {
        ObjectNode priceSpec = fixedPrice(network.newApi, FIXED_PRICE);
        String offerId = createOffer(network.newApi, "SELL", rangeAmount(500_000, 5_000_000), priceSpec,
                List.of("MAIN_CHAIN"));
        long baseAmount = 251_234;
        long quoteAmount = quoteAmountFor(baseAmount, FIXED_PRICE);
        take(network.newApi, network.oldApi, "SELL", offerId, priceSpec, baseAmount, quoteAmount, "MAIN_CHAIN",
                Trades.BTC_ADDRESS);
    }

    @Test
    void newTakerSellsAtFixedPriceToAnOldMaker() throws Exception {
        ObjectNode priceSpec = fixedPrice(network.oldApi, FIXED_PRICE);
        String offerId = createOffer(network.oldApi, "BUY", fixedAmount(Trades.QUOTE_AMOUNT), priceSpec,
                List.of("MAIN_CHAIN"));
        long baseAmount = baseAmountFor(Trades.QUOTE_AMOUNT, FIXED_PRICE);
        take(network.oldApi, network.newApi, "BUY", offerId, priceSpec, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                Trades.BTC_ADDRESS);
    }

    @Test
    void newTakerSellsAtFloatPriceBelowMarketToAnOldMaker() throws Exception {
        ObjectNode priceSpec = floatPrice(-0.02);
        String offerId = createOffer(network.oldApi, "BUY", rangeAmount(500_000, 2_000_000), priceSpec,
                List.of("MAIN_CHAIN", "LN"));
        long quoteAmount = 800_000;
        long baseAmount = baseAmountFor(quoteAmount, Math.round(marketPrice() * 0.98));
        take(network.oldApi, network.newApi, "BUY", offerId, priceSpec, baseAmount, quoteAmount, "MAIN_CHAIN",
                Trades.BTC_ADDRESS);
    }

    private static String createOffer(ApiClient maker,
                                      String direction,
                                      ObjectNode amountSpec,
                                      ObjectNode priceSpec,
                                      List<String> bitcoinPaymentMethods) {
        return maker.createOffer(OffersAcrossVersionsTest.offerRequest(maker.market("USD"), direction, amountSpec,
                priceSpec, bitcoinPaymentMethods, List.of("ZELLE")));
    }

    // The taker must see the price of the maker, else the trade would not test this type of price
    private static void take(ApiClient maker,
                             ApiClient taker,
                             String makerDirection,
                             String offerId,
                             ObjectNode priceSpec,
                             long baseAmount,
                             long quoteAmount,
                             String bitcoinPaymentMethod,
                             String bitcoinPaymentData) throws Exception {
        OffersAcrossVersionsTest.awaitOffers(taker, List.of(offerId), "the taker sees the offer of the maker");
        JsonNode priceSpecAtTaker = OffersAcrossVersionsTest.findOffer(taker, offerId).orElseThrow().path("priceSpec");
        assertEquals(priceSpec.path("type").asText(), priceSpecAtTaker.path("type").asText(), "type of the price");
        assertEquals(priceSpec.path("percentage").asDouble(), priceSpecAtTaker.path("percentage").asDouble(),
                "percentage of the float price");
        assertEquals(priceSpec.path("priceQuote").path("value").asLong(),
                priceSpecAtTaker.path("priceQuote").path("value").asLong(), "fixed price");
        String tradeId = taker.takeOffer(offerId, baseAmount, quoteAmount, bitcoinPaymentMethod, "ZELLE");
        Trades.completeAfterTake(maker, taker, makerDirection, tradeId, baseAmount, quoteAmount, bitcoinPaymentData);
    }

    // The market price of the fake price node, as a price quote value with the precision 4
    private long marketPrice() {
        return Math.round(network.priceServer.getPrice("USD") * 1e4);
    }

    // The Bitcoin amount for a fiat amount at a price, rounded to a satoshi
    private static long baseAmountFor(long quoteAmount, long price) {
        return Math.round(quoteAmount * 100_000_000.0 / price);
    }

    // The fiat amount for a Bitcoin amount at a price, truncated to 0.0001 of the currency
    private static long quoteAmountFor(long baseAmount, long price) {
        return Math.floorDiv(baseAmount * price, 100_000_000L);
    }

    private static ObjectNode fixedAmount(long amount) {
        return JSON.createObjectNode().put("type", "QuoteSideFixedAmountSpec").put("amount", amount);
    }

    private static ObjectNode rangeAmount(long minAmount, long maxAmount) {
        return JSON.createObjectNode().put("type", "QuoteSideRangeAmountSpec")
                .put("minAmount", minAmount)
                .put("maxAmount", maxAmount);
    }

    private static ObjectNode floatPrice(double percentage) {
        return JSON.createObjectNode().put("type", "FloatPriceSpec").put("percentage", percentage);
    }

    // A fixed price as a price quote of the API. It copies the market quote of the node and replaces the price of one
    // Bitcoin. Both versions build the price quote from its value and its two monetary values.
    private static ObjectNode fixedPrice(ApiClient api, long price) {
        ObjectNode priceQuote = OffersAcrossVersionsTest.marketQuote(api, "USD").deepCopy();
        priceQuote.put("value", price);
        ((ObjectNode) priceQuote.path("quoteSideMonetary")).put("value", price);
        JsonNode baseSideMonetary = priceQuote.path("baseSideMonetary");
        if (baseSideMonetary.path("value").asLong() != 100_000_000L) {
            throw new IllegalStateException("The market quote is not for one Bitcoin: " + priceQuote);
        }
        ObjectNode priceSpec = JSON.createObjectNode().put("type", "FixPriceSpec");
        priceSpec.set("priceQuote", priceQuote);
        return priceSpec;
    }
}
