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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A security manager publishes an emergency alert which requires this version for trading (alert-enforcement-v2113 of
 * the release test plan). With this alert, the security manager moves the users to this version before the cutoff date
 * of signature V1. The alert is for the desktop app, as the security manager tab of the desktop app publishes it. The
 * api-app uses the app type of the desktop app as well. The alert publisher of the source set alertPublisher publishes
 * the alert as a node of this checkout.
 * <ul>
 *     <li>A node of the released version does not take an offer while the alert exists. Its API answers with 400.</li>
 *     <li>A maker of the released version gets the take offer request of a taker of this checkout, but it does not
 *     create the trade and does not answer. The take is lost: the trade of the taker stays in its first state.</li>
 *     <li>Nodes of this checkout know the alert and trade with each other to the end.</li>
 *     <li>After the security manager removes the alert, the node of the released version trades again.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class AlertAcrossVersionsTest {
    private static final String SECURITY_MANAGER = "security-manager";
    private static final String REQUIRED_VERSION = "2.1.14";
    // The lines which AlertPublisherApp logs when the broadcast of a command is done
    private static final String PUBLISHED = "E2E alert published";
    private static final String REMOVED = "E2E alert removed";
    // The lines which nodes in dev mode log when their store of alerts added or removed an entry
    private static final String ALERT_ADDED = "Method: add success; map entry: AuthorizedAlertData";
    private static final String ALERT_REMOVED = "Method: remove success; map entry: AuthorizedAlertData";
    // A node logs the change of its store before it notifies the service of alerts
    private static final Duration ALERT_APPLIED = Duration.ofSeconds(3);
    // A maker answers a take offer request on 127.0.0.1 within a few seconds
    private static final Duration NO_ANSWER = Duration.ofSeconds(20);

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("alert-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void alertWhichRequiresThisVersionStopsTradingOnlyOnTheReleasedVersion() throws Exception {
        ApiClient securityManager = network.startAlertPublisher(SECURITY_MANAGER);
        long baseAmount = Trades.baseAmount(network.priceServer);

        // The offers exist before the alert, so that only the alert can stop the takes
        String offerOfNewMaker = Trades.createOffer(network.newApi, "SELL");
        String offerOfOldMaker = Trades.createOffer(network.oldApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerOfNewMaker), "the old node sees the offer");
        OffersAcrossVersionsTest.awaitOffers(securityManager, List.of(offerOfNewMaker), "the new node sees the offer");
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerOfOldMaker), "the new node sees the offer");

        network.sendCommand(SECURITY_MANAGER, "require " + REQUIRED_VERSION);
        awaitLogLine(SECURITY_MANAGER, PUBLISHED, "the security manager publishes the alert");
        awaitLogLine(network.oldNode.getName(), ALERT_ADDED, "the old node stores the alert");
        MixedVersionNetwork.await("the new node knows the alert", Trades.STEP_TIMEOUT,
                () -> network.newApi.tradeRestrictingAlert().path("minVersion").asText().equals(REQUIRED_VERSION));
        Thread.sleep(ALERT_APPLIED.toMillis());

        // The old node does not take an offer
        ApiClient.ApiException refusal = assertThrows(ApiClient.ApiException.class,
                () -> network.oldApi.takeOffer(offerOfNewMaker, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                        "ZELLE"),
                "The old node must not take an offer while the alert requires a newer version");
        assertEquals(400, refusal.status, refusal.getMessage());
        assertTrue(refusal.getMessage().contains("For trading you need to have version " + REQUIRED_VERSION),
                refusal.getMessage());
        assertTrue(network.oldApi.openTrades().isEmpty(), "The old node must not create a trade");

        // The old maker gets the take offer request of a taker of this version, but it creates no trade and does not
        // answer. Its executor drops the exception of the check of the alert, so it logs nothing.
        String lostTradeId = network.newApi.takeOffer(offerOfOldMaker, baseAmount, Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(network.newApi, lostTradeId, "the new taker has the trade", state -> true);
        String firstState = network.newApi.tradeProperties().get(lostTradeId).path("tradeState").asText();
        Thread.sleep(NO_ANSWER.toMillis());
        assertTrue(Trades.findTrade(network.oldApi, lostTradeId).isEmpty(),
                "The old maker must not create the trade while the alert requires a newer version");
        assertEquals(firstState, network.newApi.tradeProperties().get(lostTradeId).path("tradeState").asText(),
                "The taker must not get an answer from the old maker");

        // Nodes of this version trade with each other to the end
        String tradeId = securityManager.takeOffer(offerOfNewMaker, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                "ZELLE");
        Trades.completeAfterTake(network.newApi, securityManager, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);

        // After the removal of the alert, the old node trades again
        network.sendCommand(SECURITY_MANAGER, "remove");
        awaitLogLine(SECURITY_MANAGER, REMOVED, "the security manager removes the alert");
        awaitLogLine(network.oldNode.getName(), ALERT_REMOVED, "the old node removes the alert");
        MixedVersionNetwork.await("the new node removes the alert", Trades.STEP_TIMEOUT,
                () -> network.newApi.tradeRestrictingAlert().isNull());
        Thread.sleep(ALERT_APPLIED.toMillis());
        String offerAfterRemoval = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerAfterRemoval), "the old node sees the offer");
        String tradeAfterRemoval = network.oldApi.takeOffer(offerAfterRemoval, baseAmount, Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.completeAfterTake(network.newApi, network.oldApi, "SELL", tradeAfterRemoval, baseAmount,
                Trades.QUOTE_AMOUNT, Trades.BTC_ADDRESS);
    }

    private void awaitLogLine(String nodeName, String text, String description) throws InterruptedException {
        MixedVersionNetwork.await(description, Trades.STEP_TIMEOUT, () -> {
            try {
                return network.logOfLastSession(nodeName).stream().anyMatch(line -> line.contains(text));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
