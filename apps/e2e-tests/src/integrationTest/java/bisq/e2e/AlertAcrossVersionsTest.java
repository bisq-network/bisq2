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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Emergency alerts of a security manager across the versions. The alerts are for the desktop app, as the security
 * manager tab of the desktop app publishes them. The api-app uses the app type of the desktop app as well. The
 * authorized data publisher of the source set authorizedDataPublisher publishes the alerts as a node of this checkout.
 * <p>
 * An alert which requires this version for trading (alert-enforcement-v2113 of the release test plan). With this
 * alert, the security manager moves the users to this version before the cutoff date of signature V1.
 * <ul>
 *     <li>A node of the released version does not take an offer while the alert exists. Its API answers with 400.</li>
 *     <li>A taker of this checkout does not take the offer of a maker of the released version, which would acknowledge
 *     the take offer request and drop it. Its API answers with 400 and the error code
 *     MAKER_VERSION_BELOW_MIN_REQUIRED (docs/specifications/trade/trade-restrictions.md).</li>
 *     <li>Nodes of this checkout know the alert and trade with each other to the end.</li>
 *     <li>After the security manager removes the alert, the node of the released version trades again.</li>
 * </ul>
 * An alert which halts trading (alert-halt-trading-both-versions), the tool of the security manager for an emergency.
 * <ul>
 *     <li>Nodes of both versions refuse to take an offer and to continue an open trade.</li>
 *     <li>After the security manager removes the alert, the open trade continues, and both versions take offers again.
 *     </li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class AlertAcrossVersionsTest {
    private static final String SECURITY_MANAGER = "security-manager";
    private static final String REQUIRED_VERSION = "2.1.14";
    // The lines which nodes in dev mode log when their store of alerts added or removed an entry
    private static final String ALERT_ADDED = "Method: add success; map entry: AuthorizedAlertData";
    private static final String ALERT_REMOVED = "Method: remove success; map entry: AuthorizedAlertData";
    // A node logs the change of its store before it notifies the service of alerts
    private static final Duration ALERT_APPLIED = Duration.ofSeconds(3);

    private MixedVersionNetwork network;
    private ApiClient securityManager;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("alert-across-versions");
        securityManager = network.startAuthorizedDataPublisher(SECURITY_MANAGER);
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    @Order(1)
    void alertWhichRequiresThisVersionStopsTradingOnlyOnTheReleasedVersion() throws Exception {
        long baseAmount = Trades.baseAmount(network.priceServer);

        // The offers exist before the alert, so that only the alert can stop the takes
        String offerOfNewMaker = Trades.createOffer(network.newApi, "SELL");
        String offerOfOldMaker = Trades.createOffer(network.oldApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerOfNewMaker), "the old node sees the offer");
        OffersAcrossVersionsTest.awaitOffers(securityManager, List.of(offerOfNewMaker), "the new node sees the offer");
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerOfOldMaker), "the new node sees the offer");

        publishAlert("require " + REQUIRED_VERSION,
                alert -> alert.path("minVersion").asText().equals(REQUIRED_VERSION));

        // The old node does not take an offer
        ApiClient.ApiException refusal = assertThrows(ApiClient.ApiException.class,
                () -> network.oldApi.takeOffer(offerOfNewMaker, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                        "ZELLE"),
                "The old node must not take an offer while the alert requires a newer version");
        assertEquals(400, refusal.status, refusal.getMessage());
        assertTrue(refusal.getMessage().contains("For trading you need to have version " + REQUIRED_VERSION),
                refusal.getMessage());
        assertTrue(network.oldApi.openTrades().isEmpty(), "The old node must not create a trade");

        // The old maker would acknowledge the take offer request of a taker of this version and drop it, without an
        // answer. So the taker of this version does not take its offer.
        ApiClient.ApiException makerRefusal = assertThrows(ApiClient.ApiException.class,
                () -> network.newApi.takeOffer(offerOfOldMaker, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                        "ZELLE"),
                "The new node must not take the offer of the old maker while the alert requires a newer version");
        assertEquals(400, makerRefusal.status, makerRefusal.getMessage());
        assertTrue(makerRefusal.getMessage().contains("MAKER_VERSION_BELOW_MIN_REQUIRED"), makerRefusal.getMessage());
        assertTrue(network.newApi.openTrades().isEmpty(), "The new node must not create a trade");

        // Nodes of this version trade with each other to the end
        String tradeId = securityManager.takeOffer(offerOfNewMaker, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                "ZELLE");
        Trades.completeAfterTake(network.newApi, securityManager, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);

        // After the removal of the alert, the old node trades again
        removeAlert();
        String offerAfterRemoval = Trades.createOffer(network.newApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerAfterRemoval), "the old node sees the offer");
        String tradeAfterRemoval = network.oldApi.takeOffer(offerAfterRemoval, baseAmount, Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.completeAfterTake(network.newApi, network.oldApi, "SELL", tradeAfterRemoval, baseAmount,
                Trades.QUOTE_AMOUNT, Trades.BTC_ADDRESS);
    }

    @Test
    @Order(2)
    void alertWhichHaltsTradingStopsTradingOnBothVersions() throws Exception {
        long baseAmount = Trades.baseAmount(network.priceServer);

        // An open trade between the versions and an offer of each version exist before the alert
        String offerForOpenTrade = Trades.createOffer(network.oldApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerForOpenTrade), "the new node sees the offer");
        String openTrade = network.newApi.takeOffer(offerForOpenTrade, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                "ZELLE");
        Trades.awaitTradeState(network.newApi, openTrade, "the old maker accepts the take",
                state -> state.contains("RECEIVED"));
        String offerOfNewMaker = Trades.createOffer(network.newApi, "SELL");
        String offerOfOldMaker = Trades.createOffer(network.oldApi, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerOfNewMaker), "the old node sees the offer");
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerOfOldMaker), "the new node sees the offer");

        publishAlert("halt", alert -> alert.path("haltTrading").asBoolean(false));

        // Neither version takes an offer
        ApiClient.ApiException refusalOfOld = assertThrows(ApiClient.ApiException.class,
                () -> network.oldApi.takeOffer(offerOfNewMaker, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                        "ZELLE"),
                "The old node must not take an offer while trading is halted");
        assertEquals(400, refusalOfOld.status, refusalOfOld.getMessage());
        assertTrue(refusalOfOld.getMessage().contains("Trading is on halt"), refusalOfOld.getMessage());
        ApiClient.ApiException refusalOfNew = assertThrows(ApiClient.ApiException.class,
                () -> network.newApi.takeOffer(offerOfOldMaker, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                        "ZELLE"),
                "The new node must not take an offer while trading is halted");
        assertEquals(400, refusalOfNew.status, refusalOfNew.getMessage());
        assertTrue(refusalOfNew.getMessage().contains("HALT_TRADING"), refusalOfNew.getMessage());

        // Neither version continues the open trade
        assertThrows(ApiClient.ApiException.class,
                () -> network.oldApi.tradeEvent(openTrade, "SELLER_SENDS_PAYMENT_ACCOUNT", "e2e payment account"),
                "The old seller must not continue the trade while trading is halted");
        ApiClient.ApiException stepOfNew = assertThrows(ApiClient.ApiException.class,
                () -> network.newApi.tradeEvent(openTrade, "BUYER_SEND_BITCOIN_PAYMENT_DATA", Trades.BTC_ADDRESS),
                "The new buyer must not continue the trade while trading is halted");
        assertTrue(stepOfNew.getMessage().contains("HALT_TRADING"), stepOfNew.getMessage());

        // After the removal of the alert, the open trade continues, and both versions take offers again
        removeAlert();
        Trades.completeAfterTake(network.oldApi, network.newApi, "SELL", openTrade, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
        String tradeOfOldTaker = network.oldApi.takeOffer(offerOfNewMaker, baseAmount, Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.completeAfterTake(network.newApi, network.oldApi, "SELL", tradeOfOldTaker, baseAmount,
                Trades.QUOTE_AMOUNT, Trades.BTC_ADDRESS);
        String tradeOfNewTaker = network.newApi.takeOffer(offerOfOldMaker, baseAmount, Trades.QUOTE_AMOUNT,
                "MAIN_CHAIN", "ZELLE");
        Trades.completeAfterTake(network.oldApi, network.newApi, "SELL", tradeOfNewTaker, baseAmount,
                Trades.QUOTE_AMOUNT, Trades.BTC_ADDRESS);
    }

    // Publishes an alert and waits until the old node stores it and the new node reports it
    private void publishAlert(String command, Predicate<JsonNode> isReportedAlert) throws Exception {
        long storedBefore = network.countLogLines(network.oldNode.getName(), ALERT_ADDED);
        network.runPublisherCommand(SECURITY_MANAGER, command);
        MixedVersionNetwork.await("the old node stores the alert", Trades.STEP_TIMEOUT,
                () -> network.countLogLines(network.oldNode.getName(), ALERT_ADDED) > storedBefore);
        MixedVersionNetwork.await("the new node knows the alert", Trades.STEP_TIMEOUT,
                () -> isReportedAlert.test(network.newApi.tradeRestrictingAlert()));
        Thread.sleep(ALERT_APPLIED.toMillis());
    }

    // Removes the last alert and waits until the old node removes it and the new node no longer reports it
    private void removeAlert() throws Exception {
        long removedBefore = network.countLogLines(network.oldNode.getName(), ALERT_REMOVED);
        network.runPublisherCommand(SECURITY_MANAGER, "remove");
        MixedVersionNetwork.await("the old node removes the alert", Trades.STEP_TIMEOUT,
                () -> network.countLogLines(network.oldNode.getName(), ALERT_REMOVED) > removedBefore);
        MixedVersionNetwork.await("the new node removes the alert", Trades.STEP_TIMEOUT,
                () -> network.newApi.tradeRestrictingAlert().isNull());
        Thread.sleep(ALERT_APPLIED.toMillis());
    }
}
