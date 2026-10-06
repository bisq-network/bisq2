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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Offers of each version reach the nodes of the other version with the same terms, and their removal reaches them too.
 * The offers use the three price types and both amount types, as the API of both versions creates them.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class OffersAcrossVersionsTest {
    private static final Duration PROPAGATION_TIMEOUT = Duration.ofMinutes(2);

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("offers-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void offersOfEachVersionReachTheOtherVersionWithTheSameTerms() throws Exception {
        List<String> offersOfOldNode = createOffers(network.oldApi);
        List<String> offersOfNewNode = createOffers(network.newApi);

        awaitOffers(network.newApi, offersOfOldNode, "the new node shows the offers of the old node");
        awaitOffers(network.oldApi, offersOfNewNode, "the old node shows the offers of the new node");

        for (String offerId : concat(offersOfOldNode, offersOfNewNode)) {
            JsonNode inOldNode = findOffer(network.oldApi, offerId).orElseThrow();
            JsonNode inNewNode = findOffer(network.newApi, offerId).orElseThrow();
            for (String field : List.of("direction", "market", "amountSpec", "priceSpec", "baseSidePaymentMethodSpecs",
                    "quoteSidePaymentMethodSpecs", "supportedLanguageCodes")) {
                assertEquals(inOldNode.path(field), inNewNode.path(field), offerId + ": " + field + " differs between the versions");
            }
        }
    }

    @Test
    void removedOfferDisappearsInTheOtherVersion() throws Exception {
        String offerOfOldNode = createOffers(network.oldApi).get(0);
        String offerOfNewNode = createOffers(network.newApi).get(0);
        awaitOffers(network.newApi, List.of(offerOfOldNode), "the new node shows the offer of the old node");
        awaitOffers(network.oldApi, List.of(offerOfNewNode), "the old node shows the offer of the new node");

        network.oldApi.removeOffer(offerOfOldNode);
        network.newApi.removeOffer(offerOfNewNode);

        MixedVersionNetwork.await("the new node removes the offer of the old node", PROPAGATION_TIMEOUT,
                () -> findOffer(network.newApi, offerOfOldNode).isEmpty());
        MixedVersionNetwork.await("the old node removes the offer of the new node", PROPAGATION_TIMEOUT,
                () -> findOffer(network.oldApi, offerOfNewNode).isEmpty());
    }

    // A buy offer with market price and a range, a sell offer with float price and a fixed amount, and a buy offer with
    // the market quote of the node as fixed price, as a mobile client may create it
    static List<String> createOffers(ApiClient api) {
        JsonNode market = api.market("USD");
        List<String> offerIds = new ArrayList<>();
        offerIds.add(api.createOffer(offerRequest(market, "BUY",
                JSON.createObjectNode().put("type", "QuoteSideRangeAmountSpec").put("minAmount", 500_000).put("maxAmount", 2_000_000),
                JSON.createObjectNode().put("type", "MarketPriceSpec"),
                List.of("MAIN_CHAIN", "LN"), List.of("ZELLE", "STRIKE"))));
        offerIds.add(api.createOffer(offerRequest(market, "SELL",
                JSON.createObjectNode().put("type", "QuoteSideFixedAmountSpec").put("amount", 1_500_000),
                JSON.createObjectNode().put("type", "FloatPriceSpec").put("percentage", 0.01),
                List.of("LN"), List.of("ZELLE"))));
        ObjectNode fixPriceSpec = JSON.createObjectNode().put("type", "FixPriceSpec");
        fixPriceSpec.set("priceQuote", marketQuote(api, "USD"));
        offerIds.add(api.createOffer(offerRequest(market, "BUY",
                JSON.createObjectNode().put("type", "QuoteSideRangeAmountSpec").put("minAmount", 500_000).put("maxAmount", 5_000_000),
                fixPriceSpec,
                List.of("MAIN_CHAIN"), List.of("ZELLE"))));
        return offerIds;
    }

    static ObjectNode offerRequest(JsonNode market,
                                   String direction,
                                   ObjectNode amountSpec,
                                   ObjectNode priceSpec,
                                   List<String> bitcoinPaymentMethods,
                                   List<String> fiatPaymentMethods) {
        ObjectNode request = JSON.createObjectNode();
        request.put("direction", direction);
        request.set("market", market);
        bitcoinPaymentMethods.forEach(request.putArray("bitcoinPaymentMethods")::add);
        fiatPaymentMethods.forEach(request.putArray("fiatPaymentMethods")::add);
        request.set("amountSpec", amountSpec);
        request.set("priceSpec", priceSpec);
        request.putArray("supportedLanguageCodes").add("en");
        return request;
    }

    static JsonNode marketQuote(ApiClient api, String currencyCode) {
        JsonNode quotes = api.get("/market-price/quotes").path("quotes");
        for (Map.Entry<String, JsonNode> entry : quotes.properties()) {
            if (entry.getValue().path("quoteSideMonetary").path("code").asText().equals(currencyCode)) {
                return entry.getValue();
            }
        }
        throw new IllegalStateException("No market price for " + currencyCode + ": " + quotes);
    }

    // A fixed price as a price quote of the API. It copies the market quote of the node and replaces the price of one
    // Bitcoin. Both versions build the price quote from its value and its two monetary values.
    static ObjectNode fixedPrice(ApiClient api, long price) {
        ObjectNode priceQuote = marketQuote(api, "USD").deepCopy();
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

    static Optional<JsonNode> findOffer(ApiClient api, String offerId) {
        return api.offers("USD").stream()
                .map(item -> item.path("bisqEasyOffer"))
                .filter(offer -> offer.path("id").asText().equals(offerId))
                .findAny();
    }

    static void awaitOffers(ApiClient api, List<String> offerIds, String description) throws InterruptedException {
        MixedVersionNetwork.await(description, PROPAGATION_TIMEOUT,
                () -> offerIds.stream().allMatch(offerId -> findOffer(api, offerId).isPresent()));
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }
}
