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

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static bisq.e2e.ApiClient.JSON;

/** Steps and checks of Bisq Easy trades through the API, shared by the test classes. */
final class Trades {
    static final Duration STEP_TIMEOUT = Duration.ofMinutes(2);
    // 150 USD, the fiat precision is 4
    static final long QUOTE_AMOUNT = 1_500_000;
    static final String BTC_ADDRESS = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";
    static final String TX_ID = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b";
    private static final Set<String> FAILED_STATES = Set.of("REJECTED", "PEER_REJECTED", "CANCELLED", "PEER_CANCELLED",
            "FAILED", "FAILED_AT_PEER");

    private Trades() {
    }

    // An offer with market price, a fixed amount of 150 USD, main chain and Zelle
    static String createOffer(ApiClient maker, String direction) {
        return maker.createOffer(OffersAcrossVersionsTest.offerRequest(maker.market("USD"), direction,
                JSON.createObjectNode().put("type", "QuoteSideFixedAmountSpec").put("amount", QUOTE_AMOUNT),
                JSON.createObjectNode().put("type", "MarketPriceSpec"),
                List.of("MAIN_CHAIN"), List.of("ZELLE")));
    }

    // The taker derives the Bitcoin amount from the fiat amount and the market price, which all nodes get from the fake
    // price node. Fiat has the precision 4 and Bitcoin the precision 8.
    static long baseAmount(FakePriceServer priceServer) {
        return Math.round(QUOTE_AMOUNT * 1e8 / (priceServer.getPrice("USD") * 1e4));
    }

    static Optional<JsonNode> findTrade(ApiClient api, String tradeId) {
        return api.openTrades().stream()
                .filter(item -> item.path("trade").path("id").asText().equals(tradeId))
                .findAny();
    }

    // Waits until the current state of the trade on the node matches. Fails at once if the trade failed.
    static void awaitTradeState(ApiClient api,
                                String tradeId,
                                String description,
                                Predicate<String> expectedState) throws InterruptedException {
        MixedVersionNetwork.await(description, STEP_TIMEOUT, () -> {
            JsonNode properties = api.tradeProperties().get(tradeId);
            if (properties == null) {
                return false;
            }
            String state = properties.path("tradeState").asText();
            if (FAILED_STATES.contains(state)) {
                throw new AssertionError("Trade " + tradeId + " ended in " + state + ". Error: " +
                        properties.path("errorMessage").asText() + " Error of the peer: " +
                        properties.path("peersErrorMessage").asText());
            }
            return expectedState.test(state);
        });
    }

    static void awaitClosed(ApiClient api, String tradeId) throws InterruptedException {
        MixedVersionNetwork.await("the closed trade leaves the open trades", STEP_TIMEOUT,
                () -> findTrade(api, tradeId).isEmpty());
    }
}
