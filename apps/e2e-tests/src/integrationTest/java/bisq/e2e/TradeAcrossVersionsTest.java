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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Bisq Easy trade runs to the end between a node of the released version and a node of this checkout, in all four
 * combinations of maker version and direction. Both sides must agree on the contract and reach the final state. The
 * trades run without a mediator, which dev mode allows.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class TradeAcrossVersionsTest {
    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("trade-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void newSellerMakerTradesWithOldBuyerTaker() throws Exception {
        trade(network.newApi, network.oldApi, "SELL");
    }

    @Test
    void oldSellerMakerTradesWithNewBuyerTaker() throws Exception {
        trade(network.oldApi, network.newApi, "SELL");
    }

    @Test
    void newBuyerMakerTradesWithOldSellerTaker() throws Exception {
        trade(network.newApi, network.oldApi, "BUY");
    }

    @Test
    void oldBuyerMakerTradesWithNewSellerTaker() throws Exception {
        trade(network.oldApi, network.newApi, "BUY");
    }

    private void trade(ApiClient maker, ApiClient taker, String makerDirection) throws Exception {
        String offerId = Trades.createOffer(maker, makerDirection);
        OffersAcrossVersionsTest.awaitOffers(taker, List.of(offerId), "the taker sees the offer of the maker");

        long baseAmount = Trades.baseAmount(network.priceServer);
        String tradeId = taker.takeOffer(offerId, baseAmount, Trades.QUOTE_AMOUNT, "MAIN_CHAIN", "ZELLE");

        ApiClient seller = makerDirection.equals("SELL") ? maker : taker;
        ApiClient buyer = seller == maker ? taker : maker;
        Trades.awaitTradeState(maker, tradeId, "the maker has the trade", state -> true);
        Trades.awaitTradeState(taker, tradeId, "the taker has the trade", state -> true);

        seller.tradeEvent(tradeId, "SELLER_SENDS_PAYMENT_ACCOUNT", "e2e payment account of the seller");
        buyer.tradeEvent(tradeId, "BUYER_SEND_BITCOIN_PAYMENT_DATA", Trades.BTC_ADDRESS);
        Trades.awaitTradeState(buyer, tradeId, "the buyer receives the account data",
                state -> state.contains("BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA"));
        Trades.awaitTradeState(seller, tradeId, "the seller receives the Bitcoin address",
                state -> state.contains("SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS"));

        buyer.tradeEvent(tradeId, "BUYER_CONFIRM_FIAT_SENT", null);
        Trades.awaitTradeState(seller, tradeId, "the seller learns that the fiat was sent",
                state -> state.equals("SELLER_RECEIVED_FIAT_SENT_CONFIRMATION"));

        completeAfterFiatSent(buyer, seller, tradeId, baseAmount);
    }

    // The steps from the confirmation of the fiat receipt to the closure, and the comparison of the contract
    static void completeAfterFiatSent(ApiClient buyer, ApiClient seller, String tradeId, long baseAmount) throws Exception {
        seller.tradeEvent(tradeId, "SELLER_CONFIRM_FIAT_RECEIPT", null);
        Trades.awaitTradeState(buyer, tradeId, "the buyer learns that the fiat was received",
                state -> state.equals("BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION"));

        seller.tradeEvent(tradeId, "SELLER_CONFIRM_BTC_SENT", Trades.TX_ID);
        Trades.awaitTradeState(buyer, tradeId, "the buyer learns that the Bitcoin was sent",
                state -> state.equals("BUYER_RECEIVED_BTC_SENT_CONFIRMATION"));

        buyer.tradeEvent(tradeId, "BTC_CONFIRMED", null);
        seller.tradeEvent(tradeId, "BTC_CONFIRMED", null);
        Trades.awaitTradeState(buyer, tradeId, "the buyer completes the trade", state -> state.equals("BTC_CONFIRMED"));
        Trades.awaitTradeState(seller, tradeId, "the seller completes the trade", state -> state.equals("BTC_CONFIRMED"));
        JsonNode tradeOfBuyer = Trades.findTrade(buyer, tradeId).orElseThrow();
        JsonNode tradeOfSeller = Trades.findTrade(seller, tradeId).orElseThrow();

        // Both sides use the same contract. Its hash is the base of the signatures of both sides, so a difference here
        // would also break a mediation.
        for (String field : List.of("price", "formattedPrice", "formattedBaseAmount", "formattedQuoteAmount")) {
            assertEquals(tradeOfBuyer.path(field), tradeOfSeller.path(field), field + " differs between buyer and seller");
        }
        JsonNode contractOfBuyer = tradeOfBuyer.path("trade").path("contract");
        JsonNode contractOfSeller = tradeOfSeller.path("trade").path("contract");
        for (String field : List.of("baseSideAmount", "quoteSideAmount", "agreedPriceSpec", "marketPrice",
                "baseSidePaymentMethodSpec", "quoteSidePaymentMethodSpec", "takeOfferDate")) {
            assertEquals(contractOfBuyer.path(field), contractOfSeller.path(field), "contract field " + field + " differs");
        }
        assertEquals(baseAmount, contractOfBuyer.path("baseSideAmount").asLong(), "Bitcoin amount of the contract");
        assertEquals(Trades.QUOTE_AMOUNT, contractOfBuyer.path("quoteSideAmount").asLong(), "fiat amount of the contract");

        buyer.tradeEvent(tradeId, "CLOSE_TRADE", null);
        seller.tradeEvent(tradeId, "CLOSE_TRADE", null);
        Trades.awaitClosed(buyer, tradeId);
        Trades.awaitClosed(seller, tradeId);
    }
}
