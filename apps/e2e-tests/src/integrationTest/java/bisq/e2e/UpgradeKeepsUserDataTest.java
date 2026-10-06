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

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * User data of a node of the released version survives its upgrade in place to this checkout: open trades in two
 * states, a closed trade, settings, the list of ignored users, and user-defined payment accounts with the selected one
 * (upgrade-payment-accounts-from-api). This version skips stored entries which fail a new check, without telling the
 * user, so a lost entry shows only here. After the upgrade, the open trades continue to the end. This is the part of
 * trade-upgrade-open-trades of the release test plan which the API of v2.1.13 can create. Its API creates only
 * user-defined accounts, the accounts of Bisq Connect users.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class UpgradeKeepsUserDataTest {
    // Settings with values other than the defaults. The language stays English, because the trade steps compare
    // formatted amounts of both sides.
    private static final Map<String, JsonNode> CHANGED_SETTINGS = new LinkedHashMap<>();

    static {
        CHANGED_SETTINGS.put("bisqEasyTradeRulesConfirmed", JSON.getNodeFactory().booleanNode(true));
        CHANGED_SETTINGS.put("closeMyOfferWhenTaken", JSON.getNodeFactory().booleanNode(false));
        CHANGED_SETTINGS.put("maxTradePriceDeviation", JSON.getNodeFactory().numberNode(0.07));
        CHANGED_SETTINGS.put("numDaysAfterRedactingTradeData", JSON.getNodeFactory().numberNode(45));
        CHANGED_SETTINGS.put("useAnimations", JSON.getNodeFactory().booleanNode(false));
        CHANGED_SETTINGS.put("supportedLanguageCodes", JSON.createArrayNode().add("en").add("de"));
    }

    // An account whose data has the maximum length of both versions, with line breaks and characters outside ASCII
    private static final int MAX_ACCOUNT_DATA_LENGTH = 1000;
    private static final String LONG_ACCOUNT_NAME = "e2e account for long data";
    private static final String LONG_ACCOUNT_DATA = longAccountData();
    private static final String SHORT_ACCOUNT_NAME = "e2e Konto Z\u00fcrich \u20ac";
    private static final String SHORT_ACCOUNT_DATA = "IBAN on request in the trade chat";

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("upgrade-keeps-user-data");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void userDataOfAnOldNodeSurvivesItsUpgrade() throws Exception {
        ApiClient upgradedNode = network.oldApi;
        ApiClient peer = network.newApi;
        long baseAmount = Trades.baseAmount(network.priceServer);

        // A closed trade in the history
        String closedTradeId = Trades.runTrade(peer, upgradedNode, "BUY", network.priceServer);

        // An open trade in which the old node took an offer of the peer. It stops after the take.
        String offerOfPeer = Trades.createOffer(peer, "SELL");
        OffersAcrossVersionsTest.awaitOffers(upgradedNode, List.of(offerOfPeer),
                "the old node sees the offer of the peer");
        String tradeAfterTake = upgradedNode.takeOffer(offerOfPeer, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN",
                "ZELLE");
        Trades.awaitTradeState(peer, tradeAfterTake, "the peer has the trade", state -> true);
        Trades.awaitTradeState(upgradedNode, tradeAfterTake, "the old node gets the response of the maker",
                state -> state.startsWith("TAKER_RECEIVED_TAKE_OFFER_RESPONSE"));

        // An open trade in which the old node is the seller maker, and the buyer confirmed the fiat payment
        String offerOfOld = Trades.createOffer(upgradedNode, "SELL");
        OffersAcrossVersionsTest.awaitOffers(peer, List.of(offerOfOld), "the peer sees the offer of the old node");
        String tradeAfterFiatSent = peer.takeOffer(offerOfOld, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(upgradedNode, tradeAfterFiatSent, "the old node has the trade", state -> true);
        upgradedNode.tradeEvent(tradeAfterFiatSent, "SELLER_SENDS_PAYMENT_ACCOUNT",
                "e2e payment account of the seller");
        peer.tradeEvent(tradeAfterFiatSent, "BUYER_SEND_BITCOIN_PAYMENT_DATA", Trades.BTC_ADDRESS);
        Trades.awaitTradeState(upgradedNode, tradeAfterFiatSent, "the old node gets the Bitcoin address",
                state -> state.contains("SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS"));
        peer.tradeEvent(tradeAfterFiatSent, "BUYER_CONFIRM_FIAT_SENT", null);
        Trades.awaitTradeState(upgradedNode, tradeAfterFiatSent, "the old node learns that the fiat was sent",
                state -> state.equals("SELLER_RECEIVED_FIAT_SENT_CONFIRMATION"));

        // Settings, and a user whom the old node ignores. The old node must know the profile before it can ignore it.
        CHANGED_SETTINGS.forEach(upgradedNode::changeSetting);
        JsonNode settingsBefore = upgradedNode.settings();
        ApiClient stranger = network.startExtraApiNode("stranger", true,
                List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort()));
        String strangerProfileId = stranger.userProfileId();
        MixedVersionNetwork.await("the old node ignores the profile of the stranger", Duration.ofMinutes(2), () -> {
            upgradedNode.ignoreUserProfile(strangerProfileId);
            return true;
        });

        // Payment accounts, as a Bisq Connect user creates them, and the selected one
        upgradedNode.addUserDefinedAccountWithOldApi(LONG_ACCOUNT_NAME, LONG_ACCOUNT_DATA);
        upgradedNode.addUserDefinedAccountWithOldApi(SHORT_ACCOUNT_NAME, SHORT_ACCOUNT_DATA);
        upgradedNode.selectUserDefinedAccountWithOldApi(SHORT_ACCOUNT_NAME, SHORT_ACCOUNT_DATA);
        Map<String, String> accountsBefore = upgradedNode.userDefinedAccounts();
        assertEquals(Map.of(LONG_ACCOUNT_NAME, LONG_ACCOUNT_DATA, SHORT_ACCOUNT_NAME, SHORT_ACCOUNT_DATA),
                accountsBefore, "The old node must have the accounts");
        assertEquals(Optional.of(SHORT_ACCOUNT_NAME), upgradedNode.selectedUserDefinedAccountName(),
                "The old node must have selected the account");

        Map<String, String> statesBefore = new LinkedHashMap<>();
        for (String tradeId : List.of(tradeAfterTake, tradeAfterFiatSent)) {
            statesBefore.put(tradeId, upgradedNode.tradeProperties().get(tradeId).path("tradeState").asText());
        }
        List<String> ignoredBefore = upgradedNode.ignoredUserProfileIds();
        assertTrue(ignoredBefore.contains(strangerProfileId), "The old node must ignore the stranger");

        // The user quits v2.1.13 and starts the new version on the same data directory. The new version asks for the
        // acceptance of its current terms (TacVersion).
        network.stopOldNode();
        network.startOldNodeWithNewVersion();
        upgradedNode.acceptTermsAndConditions();

        statesBefore.forEach((tradeId, state) -> {
            JsonNode properties = upgradedNode.tradeProperties().get(tradeId);
            assertTrue(properties != null, "The upgraded node lost the open trade " + tradeId);
            assertEquals(state, properties.path("tradeState").asText(), "state of the open trade " + tradeId);
        });
        MixedVersionNetwork.await("the upgraded node has the closed trade in its history", Trades.STEP_TIMEOUT,
                () -> upgradedNode.closedTrades().stream()
                        .anyMatch(item -> item.path("trade").path("id").asText().equals(closedTradeId)));
        JsonNode settingsAfter = upgradedNode.settings();
        for (String field : CHANGED_SETTINGS.keySet()) {
            assertEquals(comparable(settingsBefore.path(field)), comparable(settingsAfter.path(field)),
                    "setting " + field);
        }
        assertEquals(new HashSet<>(ignoredBefore), new HashSet<>(upgradedNode.ignoredUserProfileIds()),
                "ignored users");
        assertEquals(accountsBefore, upgradedNode.userDefinedAccounts(), "user-defined payment accounts");
        assertEquals(Optional.of(SHORT_ACCOUNT_NAME), upgradedNode.selectedUserDefinedAccountName(),
                "selected payment account");

        // The open trades continue to the end
        Trades.completeAfterTake(peer, upgradedNode, "SELL", tradeAfterTake, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
        Trades.completeAfterFiatSent(peer, upgradedNode, tradeAfterFiatSent, baseAmount, Trades.QUOTE_AMOUNT);
    }

    private static String longAccountData() {
        String start = "Bank: Z\u00fcrich Kantonalbank \uD83C\uDFE6\nHolder: e2e \u00dcn\u00efc\u00f6d\u00e9\nNotes: ";
        return start + "x".repeat(MAX_ACCOUNT_DATA_LENGTH - start.length());
    }

    // A set of the settings is a JSON array whose order the versions may choose differently
    private static Object comparable(JsonNode value) {
        if (!value.isArray()) {
            return value;
        }
        Set<String> elements = new HashSet<>();
        value.forEach(element -> elements.add(element.asText()));
        return elements;
    }
}
