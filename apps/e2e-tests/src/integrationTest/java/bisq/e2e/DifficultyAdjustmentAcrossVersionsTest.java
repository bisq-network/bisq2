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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

/**
 * A difficulty adjustment of a security manager across the versions (difficulty-adjustment-across-versions). During a
 * spam attack, the security manager multiplies the difficulty of the proofs of work of all messages. The cost of a
 * message changed in this version (MessageCostPolicy), and docs/specifications/network/message-cost.md states that both
 * versions compute the same cost for the messages of normal traffic. The factor multiplies any difference. A receiver
 * which expects a higher difficulty than the sender paid rejects the message and closes the connection, which the log
 * scan reports (connection-authorization-failed).
 * <ul>
 *     <li>The security manager publishes a factor of 100. Nodes of both versions store it.</li>
 *     <li>The nodes which ran before the adjustment know the earlier factor of their peers until the next exchange of
 *     the network load. A receiver then accepts the difficulty of its previous network load. These nodes trade in
 *     both directions, also with a take offer request through the mailbox of an offline maker.</li>
 *     <li>Nodes which start after the adjustment, one of each version, learn the factor of their peers when they
 *     connect. They trade with each other in both directions.</li>
 *     <li>The security manager removes the adjustment, and the nodes trade again.</li>
 * </ul>
 * The test does not observe the difficulty of a single proof of work, because no API or log line shows it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class DifficultyAdjustmentAcrossVersionsTest {
    private static final String SECURITY_MANAGER = "security-manager";
    // Below the cap of the difficulty for the messages of a trade, so that a difference of the cost still matters
    private static final String FACTOR = "100";
    // Nodes in dev mode log each change of their store of difficulty adjustments
    private static final String ADJUSTMENT_ADDED = "Method: add success; map entry: AuthorizedDifficultyAdjustmentData";
    private static final String ADJUSTMENT_REMOVED =
            "Method: remove success; map entry: AuthorizedDifficultyAdjustmentData";

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("difficulty-adjustment-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void bothVersionsExchangeMessagesWithADifficultyAdjustment() throws Exception {
        network.startAuthorizedDataPublisher(SECURITY_MANAGER);
        List<String> runningNodes = List.of(network.oldNode.getName(), network.newNode.getName(),
                network.oldSeed.getName(), network.newSeed.getName());

        network.runPublisherCommand(SECURITY_MANAGER, "difficulty " + FACTOR);
        awaitLogLineOnNodes(runningNodes, ADJUSTMENT_ADDED, "stores the difficulty adjustment");

        // The nodes which ran before the adjustment trade in both directions
        Trades.runTrade(network.oldApi, network.newApi, "SELL", network.priceServer);
        Trades.runTrade(network.newApi, network.oldApi, "BUY", network.priceServer);

        // A take offer request through the mailbox of the offline maker of this version
        String offerId = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerId), "the old taker sees the offer");
        network.stopNewNode();
        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = network.oldApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        network.startNewNode();
        Trades.completeAfterTake(network.newApi, network.oldApi, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);

        // Nodes which start after the adjustment trade with each other
        List<Integer> seeds = List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort());
        ApiClient lateOld = network.startExtraApiNode("late-old", false, seeds);
        ApiClient lateNew = network.startExtraApiNode("late-new", true, seeds);
        awaitLogLineOnNodes(List.of("late-old", "late-new"), ADJUSTMENT_ADDED, "gets the difficulty adjustment");
        Trades.runTrade(lateOld, lateNew, "SELL", network.priceServer);
        Trades.runTrade(lateNew, lateOld, "SELL", network.priceServer);

        // Without the adjustment, the nodes trade again
        network.runPublisherCommand(SECURITY_MANAGER, "remove-difficulty");
        awaitLogLineOnNodes(List.of(network.oldNode.getName(), "late-old", "late-new"), ADJUSTMENT_REMOVED,
                "removes the difficulty adjustment");
        Trades.runTrade(lateOld, network.newApi, "SELL", network.priceServer);
        Trades.runTrade(lateNew, network.oldApi, "SELL", network.priceServer);
    }

    private void awaitLogLineOnNodes(List<String> nodeNames, String text, String description)
            throws InterruptedException {
        for (String nodeName : nodeNames) {
            MixedVersionNetwork.await(nodeName + " " + description, Trades.STEP_TIMEOUT,
                    () -> network.countLogLines(nodeName, text) > 0);
        }
    }
}
