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

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A client of Bisq Connect, like the mobile app, which paired with a node of the released version keeps working after
 * the node upgrades in place to this checkout, without pairing again (connect-upgrade-existing-pairing of the release
 * test plan). The node runs with the security settings of its version: v2.1.13 requires a paired client and has no
 * sessions. This checkout also requires a session, which lives only in memory and expires.
 * <ul>
 *     <li>After the upgrade, the node does not know the session of the client any more. The client gets a new session
 *     with the credentials of its pairing, and uses the offerbook, the trade chat and the next trade steps.</li>
 *     <li>The node promotes the full standard permission set of the client once.</li>
 *     <li>The node refuses a session for an empty secret (api-app-data-from-mobile-build).</li>
 *     <li>With a time to live of one minute, an expired session gets HTTP 401, and the client gets a new one.</li>
 * </ul>
 * A client which pairs with a node of this checkout trades with a node of the released version in both directions
 * (connect-new-pairing-and-trade of the release test plan).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class ConnectPairingAcrossUpgradeTest {
    private static final String NODE = "connect";
    private static final String CLIENT_NAME = "e2e-mobile-app";
    private static final String NODE_OF_THIS_VERSION = "connect-new";
    private static final String TEXT_BEFORE = "A message of the app before the upgrade";
    private static final String TEXT_AFTER = "A message of the app after the upgrade";

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("connect-pairing-across-upgrade");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void clientWhichPairedWithTheOldVersionWorksAfterTheUpgradeWithoutPairingAgain() throws Exception {
        ApiClient app = network.startExtraApiNodeForPairedClient(NODE, false,
                List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort()), CLIENT_NAME);
        String clientId = app.clientId();

        // The user of the app has an open trade with a peer of this checkout, and chats in it
        String offerId = Trades.createOffer(app, "SELL");
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerId), "the peer sees the offer of the app user");
        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = network.newApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(app, tradeId, "the app user has the trade", state -> true);
        String channelId = TradeChats.channelId(app, tradeId);
        app.sendTradeChatMessage(channelId, TEXT_BEFORE, null);
        TradeChats.awaitMessage(network.newApi, channelId, TEXT_BEFORE, "the peer gets the message of the app");

        // The user upgrades the desktop in place. The new version knows the client, but not its session.
        network.upgradeExtraNode(NODE);
        ApiClient.ApiException unknownSession = assertThrows(ApiClient.ApiException.class, app::settings,
                "The upgraded node must not accept the session of the old version");
        assertEquals(401, unknownSession.status, unknownSession.getMessage());
        app.renewSession();
        // The new version asks for the acceptance of its current terms (TacVersion), which the user does in the app
        app.acceptTermsAndConditions();
        assertEquals(1, permissionPromotionsOf(clientId),
                "The upgraded node must promote the full standard permission set of the client once");

        // The app works without pairing again: the offerbook, the trade chat, and the next trade steps to the end
        app.offers("USD");
        TradeChats.awaitMessage(app, channelId, TEXT_BEFORE, "the app still has its message");
        app.sendTradeChatMessage(channelId, TEXT_AFTER, null);
        TradeChats.awaitMessage(network.newApi, channelId, TEXT_AFTER, "the peer gets the message of the app");
        Trades.completeAfterTake(app, network.newApi, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);

        // The node gives no session for an empty secret
        ApiClient.ApiException emptySecret = assertThrows(ApiClient.ApiException.class,
                () -> app.requestSession(clientId, ""), "The node must refuse an empty client secret");
        assertTrue(emptySecret.status == 400 || emptySecret.status == 401, emptySecret.getMessage());

        // With a time to live of one minute, the session expires, and the app gets a new one. The node promoted the
        // permissions before, so it does not promote them again.
        network.restartExtraNode(NODE, List.of("-Dapplication.api.server.security.session.ttlInMinutes=1"));
        app.renewSession();
        app.settings();
        assertEquals(0, permissionPromotionsOf(clientId), "The node must promote the permissions only once");
        Thread.sleep(Duration.ofSeconds(75).toMillis());
        ApiClient.ApiException expiredSession = assertThrows(ApiClient.ApiException.class, app::settings,
                "The node must not accept an expired session");
        assertEquals(401, expiredSession.status, expiredSession.getMessage());
        app.renewSession();
        app.settings();
    }

    @Test
    void clientWhichPairedWithThisVersionTradesWithTheReleasedVersion() throws Exception {
        ApiClient app = network.startExtraApiNodeForPairedClient(NODE_OF_THIS_VERSION, true,
                List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort()), CLIENT_NAME);
        // The node of this version grants the full standard permission set at the pairing, so it promotes nothing
        assertEquals(0, network.countLogLines(NODE_OF_THIS_VERSION, "Promoting full standard permission set"),
                "The node must not promote the permissions of a client which paired with this version");

        // The user of the app is the maker, then the taker of an offer of the released version
        Trades.runTrade(app, network.oldApi, "SELL", network.priceServer);
        Trades.runTrade(network.oldApi, app, "SELL", network.priceServer);
    }

    // How often the node logged the promotion of the permission set of the client since its last start
    private long permissionPromotionsOf(String clientId) throws IOException {
        return network.logOfLastSession(NODE).stream()
                .filter(line -> line.contains("Promoting full standard permission set of client " + clientId))
                .count();
    }
}
