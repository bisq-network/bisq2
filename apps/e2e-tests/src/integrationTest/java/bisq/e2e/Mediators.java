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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Takes which check the mediator of the trade, shared by the test classes with mediators. A maker accepts a take only
 * if it selected the same mediator as the taker, so each completed take shows that both selected the same one.
 */
final class Mediators {
    // Nodes in dev mode log each new entry of their store of roles
    static final String ROLE_ADDED = "Method: add success; map entry: AuthorizedBondedRole";
    static final String ROLE_REMOVED = "Method: remove success; map entry: AuthorizedBondedRole";
    // Each take selects one of two mediators by the hash of the IDs. The chance that 12 takes miss one is below 0.1 %.
    private static final int MAX_TAKES_FOR_EACH_MEDIATOR = 12;
    // A node which knew another mediator would select it in half of the takes. 4 takes show it with a chance of 94 %.
    private static final int TAKES_FOR_ONE_MEDIATOR = 4;

    private Mediators() {
    }

    static String takeAndCompareMediators(MixedVersionNetwork network,
                                          ApiClient maker,
                                          ApiClient taker,
                                          String direction) throws Exception {
        String offerId = Trades.createOffer(maker, direction);
        OffersAcrossVersionsTest.awaitOffers(taker, List.of(offerId), "the taker sees the offer");
        String tradeId = taker.takeOffer(offerId, Trades.baseAmount(network.priceServer), Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(taker, tradeId, "the maker accepts the take", state -> state.contains("RECEIVED"));
        Trades.awaitTradeState(maker, tradeId, "the maker has the trade", state -> true);
        assertEquals(mediatorOf(taker, tradeId), mediatorOf(maker, tradeId), "mediator of maker and taker");
        return tradeId;
    }

    // The profile ID of the mediator of the trade on the node, or an empty text without mediator
    static String mediatorOf(ApiClient api, String tradeId) {
        return Trades.findTrade(api, tradeId).orElseThrow()
                .path("mediatorUserProfile").path("networkId").path("pubKey").path("id").asText();
    }

    // Takes in all directions between the two nodes, until each of the mediators was selected once
    static void assertEachMediatorIsSelected(MixedVersionNetwork network,
                                             Set<String> mediatorIds,
                                             ApiClient oneNode,
                                             ApiClient otherNode) throws Exception {
        Set<String> selectedMediators = new HashSet<>();
        for (int take = 0; take < MAX_TAKES_FOR_EACH_MEDIATOR && !selectedMediators.containsAll(mediatorIds);
             take++) {
            String tradeId = takeInDirection(network, take, oneNode, otherNode);
            selectedMediators.add(mediatorOf(take % 2 == 0 ? oneNode : otherNode, tradeId));
        }
        assertEquals(mediatorIds, selectedMediators, "The takes must select each mediator at least once");
    }

    static void assertOnlyMediatorIsSelected(MixedVersionNetwork network,
                                             String mediatorId,
                                             ApiClient oneNode,
                                             ApiClient otherNode) throws Exception {
        for (int take = 0; take < TAKES_FOR_ONE_MEDIATOR; take++) {
            String tradeId = takeInDirection(network, take, oneNode, otherNode);
            assertEquals(mediatorId, mediatorOf(take % 2 == 0 ? oneNode : otherNode, tradeId),
                    "The take must select the only mediator which both nodes may select");
        }
    }

    static void awaitLogLines(MixedVersionNetwork network, String nodeName, String text, long count, String description)
            throws InterruptedException {
        MixedVersionNetwork.await(description, Trades.STEP_TIMEOUT,
                () -> network.countLogLines(nodeName, text) >= count);
    }

    // The makers alternate between the nodes, and the directions alternate after two takes
    private static String takeInDirection(MixedVersionNetwork network,
                                          int take,
                                          ApiClient oneNode,
                                          ApiClient otherNode) throws Exception {
        ApiClient maker = take % 2 == 0 ? oneNode : otherNode;
        ApiClient taker = take % 2 == 0 ? otherNode : oneNode;
        return takeAndCompareMediators(network, maker, taker, (take / 2) % 2 == 0 ? "SELL" : "BUY");
    }
}
