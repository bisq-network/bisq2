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

import java.util.List;

import static bisq.e2e.ApiClient.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Bitcoin payment data of the buyer requests no amount or the trade amount
 * (docs/specifications/trade/bisq-easy-bitcoin-payment-data.md). A buyer of the released version can still send data
 * which requests another amount. A seller of this checkout then fails the trade and reports the failure to the buyer
 * (rule 3). A buyer of this checkout does not send such data, and its API answers with 400 (rule 4).
 * <p>
 * The test does not send a bitcoin URI with the trade amount, which rule 1 allows: the sellers of both versions fail a
 * main chain trade whose payment data is longer than 62 characters. Such a URI with a bech32 address of 42 characters
 * is longer, unless the amount has at most four characters, such as 0.01. The trade amount of this test, 0.00230769,
 * gives 68 characters.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class PaymentDataAmountAcrossVersionsTest {
    // A BOLT 11 invoice for 2500u, 250 000 satoshi: the data part of the example of BOLT 11 without amount, with the
    // amount in the prefix and a new checksum. Its signature is not valid, but no node checks it.
    private static final String LN_INVOICE_FOR_ANOTHER_AMOUNT =
            "lnbc2500u1pvjluezsp5zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zygspp5qqqsyqcyq5rqwzqfqqqsyqcyq" +
            "5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq9qrsgq3" +
            "57wnc5r2ueh7ck6q93dj32dlqnls087fxdwk8qakdyafkq3yap9us6v52vjjsrvywa6rt52cm9r9zqt8r2t7mlcwspyetp5h2tzt" +
            "ugpg0aq4t";
    private static final String BITCOIN_URI_FOR_ANOTHER_AMOUNT = "bitcoin:" + Trades.BTC_ADDRESS + "?amount=0.5";
    // The reason of the seller of this checkout
    private static final String ANOTHER_AMOUNT = "must not request another amount";

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("payment-data-amount-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    @Order(1)
    void newSellerFailsTheTradeWhenTheInvoiceOfAnOldBuyerRequestsAnotherAmount() throws Exception {
        expectRejectionLogOfTheNewSeller();
        String tradeId = oldBuyerTakesOfferOfNewSeller("LN");
        network.newApi.tradeEvent(tradeId, "SELLER_SENDS_PAYMENT_ACCOUNT", "e2e payment account of the seller");
        network.oldApi.tradeEvent(tradeId, "BUYER_SEND_BITCOIN_PAYMENT_DATA", LN_INVOICE_FOR_ANOTHER_AMOUNT);
        assertFailedAtSellerAndBuyer(tradeId);
    }

    @Test
    @Order(2)
    void newSellerFailsTheTradeWhenTheBitcoinUriOfAnOldBuyerRequestsAnotherAmount() throws Exception {
        expectRejectionLogOfTheNewSeller();
        String tradeId = oldBuyerTakesOfferOfNewSeller("MAIN_CHAIN");
        network.newApi.tradeEvent(tradeId, "SELLER_SENDS_PAYMENT_ACCOUNT", "e2e payment account of the seller");
        network.oldApi.tradeEvent(tradeId, "BUYER_SEND_BITCOIN_PAYMENT_DATA", BITCOIN_URI_FOR_ANOTHER_AMOUNT);
        assertFailedAtSellerAndBuyer(tradeId);
    }

    @Test
    @Order(3)
    void newSellerAcceptsABitcoinUriWithoutAmountOfAnOldBuyer() throws Exception {
        String tradeId = oldBuyerTakesOfferOfNewSeller("MAIN_CHAIN");
        Trades.completeAfterTake(network.newApi, network.oldApi, "SELL", tradeId,
                Trades.baseAmount(network.priceServer), Trades.QUOTE_AMOUNT, "bitcoin:" + Trades.BTC_ADDRESS);
    }

    @Test
    @Order(4)
    void newBuyerRefusesToSendDataWhichRequestsAnotherAmount() throws Exception {
        String offerId = createOffer(network.oldApi, List.of("MAIN_CHAIN", "LN"));
        OffersAcrossVersionsTest.awaitOffers(network.newApi, List.of(offerId), "the new buyer sees the offer");
        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = network.newApi.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");
        Trades.awaitTradeState(network.oldApi, tradeId, "the old seller has the trade", state -> true);

        ApiClient.ApiException refusal = assertThrows(ApiClient.ApiException.class,
                () -> network.newApi.tradeEvent(tradeId, "BUYER_SEND_BITCOIN_PAYMENT_DATA",
                        BITCOIN_URI_FOR_ANOTHER_AMOUNT),
                "The new buyer must not send a Bitcoin URI for another amount");
        assertEquals(400, refusal.status, refusal.getMessage());

        // The trade continues with data without amount
        Trades.completeAfterTake(network.oldApi, network.newApi, "SELL", tradeId, baseAmount, Trades.QUOTE_AMOUNT,
                Trades.BTC_ADDRESS);
    }

    // A sell offer of the new node with market price and a fixed amount, which the old node takes as buyer
    private String oldBuyerTakesOfferOfNewSeller(String bitcoinPaymentMethod) throws Exception {
        String offerId = createOffer(network.newApi, List.of("MAIN_CHAIN", "LN"));
        OffersAcrossVersionsTest.awaitOffers(network.oldApi, List.of(offerId), "the old buyer sees the offer");
        String tradeId = network.oldApi.takeOffer(offerId, Trades.baseAmount(network.priceServer), Trades.QUOTE_AMOUNT,
                bitcoinPaymentMethod, "ZELLE");
        Trades.awaitTradeState(network.newApi, tradeId, "the new seller has the trade", state -> true);
        Trades.awaitTradeState(network.oldApi, tradeId, "the old buyer has the trade", state -> true);
        return tradeId;
    }

    private static String createOffer(ApiClient seller, List<String> bitcoinPaymentMethods) {
        return seller.createOffer(OffersAcrossVersionsTest.offerRequest(seller.market("USD"), "SELL",
                JSON.createObjectNode().put("type", "QuoteSideFixedAmountSpec").put("amount", Trades.QUOTE_AMOUNT),
                JSON.createObjectNode().put("type", "MarketPriceSpec"),
                bitcoinPaymentMethods, List.of("ZELLE")));
    }

    // The seller of this checkout logs the failed check, as specified
    private void expectRejectionLogOfTheNewSeller() {
        network.expectLogFinding(network.newNode.getName(), "error-line", ANOTHER_AMOUNT);
    }

    // The seller of this checkout fails the trade, and the buyer of the old version learns the reason
    private void assertFailedAtSellerAndBuyer(String tradeId) throws InterruptedException {
        JsonNode atSeller = awaitTradeInState(network.newApi, tradeId, "FAILED", "the new seller fails the trade");
        assertTrue(atSeller.path("errorMessage").asText().contains(ANOTHER_AMOUNT),
                "reason at the seller: " + atSeller.path("errorMessage").asText());
        JsonNode atBuyer = awaitTradeInState(network.oldApi, tradeId, "FAILED_AT_PEER",
                "the old buyer learns that the seller failed the trade");
        assertTrue(atBuyer.path("peersErrorMessage").asText().contains(ANOTHER_AMOUNT),
                "reason at the buyer: " + atBuyer.path("peersErrorMessage").asText());
    }

    // Trades.awaitTradeState fails at once in a failed state, so this test waits for the failed state itself
    private static JsonNode awaitTradeInState(ApiClient api, String tradeId, String state, String description)
            throws InterruptedException {
        JsonNode[] found = new JsonNode[1];
        MixedVersionNetwork.await(description, Trades.STEP_TIMEOUT, () -> {
            JsonNode properties = api.tradeProperties().get(tradeId);
            if (properties != null && properties.path("tradeState").asText().equals(state)) {
                found[0] = properties;
                return true;
            }
            return false;
        });
        return found[0];
    }
}
