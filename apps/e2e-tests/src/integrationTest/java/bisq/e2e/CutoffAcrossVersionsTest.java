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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The network passes the cutoff date of signature V1 (docs/specifications/network/storage-request-signatures.md). All
 * nodes run with a fake clock (libfaketime) which starts a few minutes before the date. The test checks the rules which
 * users notice:
 * <ul>
 *     <li>Rule 5: from the cutoff date on, nodes of this version reject the new data of nodes of the old version,
 *     which has no signature V2.</li>
 *     <li>Rule 7: nodes of the old version still get the data of nodes of this version.</li>
 *     <li>Rule 9: nodes of this version remove the data without signature V2 which they stored before the cutoff date,
 *     a node which starts after the date when it loads its stores, and a node which runs across the date with the task
 *     which removes expired entries. Here this node is a node which a user upgraded from the old version before the
 *     date.</li>
 *     <li>After the date, a trade between two nodes of this version works also when the take offer request goes
 *     through the mailbox, and an offer which a node of this version removes disappears on the nodes of both
 *     versions.</li>
 * </ul>
 * The upgraded node also removes the data of this version which it stored with the old version, without signature V2
 * (open point "Copies without signature V2 of requests of this version"). The test does not check whether this data
 * comes back. The task which removes expired entries runs every 10 minutes, so the test takes about 11 minutes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class CutoffAcrossVersionsTest {
    // SignedRequest.SIGNATURE_V1_CUTOFF_DATE of this version. The test uses the nodes as black boxes, so it repeats the
    // date here.
    private static final Instant CUTOFF = Instant.parse("2026-11-25T00:00:00Z");
    // The clocks of the nodes start this long before the cutoff date: time to start the network and to publish data
    private static final Duration LEAD = Duration.ofMinutes(3);
    // Time for a broadcast to reach all nodes of the network on localhost
    private static final Duration PROPAGATION = Duration.ofSeconds(15);
    // The period of PruneExpiredEntriesService. It runs once when a node starts, and then after each period.
    private static final Duration PRUNE_PERIOD = Duration.ofMinutes(10);
    // Time for the start of the node until its first run of the task, and for the run itself
    private static final Duration PRUNE_MARGIN = Duration.ofMinutes(2);

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        assumeTrue(E2eSettings.libfaketime().isPresent(),
                "The test needs libfaketime for a fake clock of the nodes. See tools/compat/README.md.");
        Duration clockOffset = Duration.between(Instant.now(), CUTOFF.minus(LEAD));
        network = new MixedVersionNetwork("cutoff-across-versions", clockOffset);
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void nodesOfThisVersionRejectAndRemoveDataOfTheOldVersionFromTheCutoffOn() throws Exception {
        assertTrue(network.nodeTimeMillis() < CUTOFF.minusSeconds(60).toEpochMilli(),
                "The network started too late to publish data before the cutoff date. Increase LEAD.");

        // Before the cutoff date, the data of both versions reaches all nodes. A user of the old version gets the
        // offers and upgrades to this version before the cutoff date. The stores of the old version hold all data
        // without signature V2, also the offer of this version. The upgraded node runs across the cutoff date. Its task
        // which removes expired entries runs again one period after the upgrade.
        String oldOfferBefore = Trades.createOffer(network.oldApi, "SELL");
        String newOfferBefore = Trades.createOffer(network.newApi, "BUY");
        ApiClient upgradedNode = network.startExtraApiNode("upgraded", false,
                List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort()));
        OffersAcrossVersionsTest.awaitOffers(upgradedNode, List.of(oldOfferBefore, newOfferBefore),
                "the node of the old version gets both offers before its upgrade");
        Instant upgradedNodeStarted = Instant.now();
        network.upgradeExtraNode("upgraded");
        for (ApiClient api : List.of(network.oldApi, network.newApi, upgradedNode)) {
            OffersAcrossVersionsTest.awaitOffers(api, List.of(oldOfferBefore, newOfferBefore),
                    "all nodes have both offers before the cutoff date");
        }

        long waitMillis = CUTOFF.toEpochMilli() - network.nodeTimeMillis() + 5_000;
        assertTrue(waitMillis > 0, "The data before the cutoff date was published too late. Increase LEAD.");
        Thread.sleep(waitMillis);

        // Rule 5 and rule 7
        String oldOfferAfter = Trades.createOffer(network.oldApi, "SELL");
        String newOfferAfter = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(oldOfferAfter, newOfferAfter),
                "the old node has its own new offer and gets the new offer of the new node");
        OffersAcrossVersionsTest.awaitOffers(upgradedNode, List.of(newOfferAfter),
                "the upgraded node gets the new offer of the new node");
        Thread.sleep(PROPAGATION.toMillis());
        for (ApiClient api : List.of(network.newApi, upgradedNode)) {
            assertTrue(OffersAcrossVersionsTest.findOffer(api, oldOfferAfter).isEmpty(),
                    "A node of this version must not accept an offer without signature V2 after the cutoff date");
        }

        // A trade between two nodes of this version works after the cutoff date also through the mailbox, which is the
        // usual path of a take offer request: the maker is offline when the upgraded node takes its offer, and gets the
        // request with signature V2 from its mailbox when it starts again.
        String offerToTake = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(upgradedNode, List.of(offerToTake),
                "the upgraded node sees the offer to take");
        network.stopNewNode();
        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = upgradedNode.takeOffer(offerToTake, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(upgradedNode, tradeId, "the upgraded taker has the trade", state -> true);
        network.startNewNode();

        // Rule 9 when a node loads its stores. The own offers of the node have signature V2 and stay.
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(newOfferBefore, newOfferAfter),
                "the restarted new node keeps its own offers");
        Thread.sleep(PROPAGATION.toMillis());
        assertTrue(OffersAcrossVersionsTest.findOffer(network.newApi, oldOfferBefore).isEmpty(),
                "The restarted node must remove the offer without signature V2 which it stored before the cutoff date");
        assertTrue(OffersAcrossVersionsTest.findOffer(network.newApi, oldOfferAfter).isEmpty(),
                "The restarted node must not get the new offer of the old node with an inventory response");

        Trades.completeAfterTake(network.newApi, upgradedNode, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);

        // An offer which a node of this version removes after the cutoff date disappears on the nodes of both versions
        String offerToRemove = Trades.createOffer(network.newApi, "BUY");
        for (ApiClient api : List.of(network.oldApi, upgradedNode)) {
            OffersAcrossVersionsTest.awaitOffers(api, List.of(offerToRemove), "the node sees the offer to remove");
        }
        network.newApi.removeOffer(offerToRemove);
        for (ApiClient api : List.of(network.oldApi, upgradedNode)) {
            MixedVersionNetwork.await("the removed offer disappears on the node", Trades.STEP_TIMEOUT,
                    () -> OffersAcrossVersionsTest.findOffer(api, offerToRemove).isEmpty());
        }

        // Rule 9 while a node runs: the task which removes expired entries removes the offer, and the offerbook of the
        // node drops it. The node got the new offer after the cutoff date, so this offer has signature V2 and stays.
        Duration untilSecondPruneRun = Duration.between(Instant.now(),
                upgradedNodeStarted.plus(PRUNE_PERIOD).plus(PRUNE_MARGIN));
        MixedVersionNetwork.await("the upgraded node removes the offer without signature V2 which it stored before " +
                        "the cutoff date", untilSecondPruneRun,
                () -> OffersAcrossVersionsTest.findOffer(upgradedNode, oldOfferBefore).isEmpty());
        assertTrue(OffersAcrossVersionsTest.findOffer(upgradedNode, newOfferAfter).isPresent(),
                "The upgraded node must keep the offers with signature V2");
    }
}
