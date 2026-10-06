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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.common.util.StringUtils;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsMakerTrade;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * The seller's wallet pays the amount which the bitcoin payment data of the buyer requests
 * (docs/specifications/trade/bisq-easy-bitcoin-payment-data.md). The trade amount is 0.001 BTC.
 */
class BisqEasyBtcAddressMessageHandlerTest {
    // Invoices of the BOLT 11 specification: 20m is 0.02 BTC, the second has no amount
    private static final String INVOICE_OF_20_MILLI_BITCOIN = "lnbc20m1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqhp58yjmdan79s6qqdhdzgynm4zwqd5d7xmw5fk98klysy043l2ahrqsfpp3qjmp7lwpagxun9pygexvgpjdc4jdj85fr9yq20q82gphp2nflc7jtzrcazrra7wwgzxqc8u7754cdlpfrmccae92qgzqvzq2ps8pqqqqqqpqqqqq9qqqvpeuqafqxu92d8lr6fvg0r5gv0heeeqgcrqlnm6jhphu9y00rrhy4grqszsvpcgpy9qqqqqqgqqqqq7qqzqj9n4evl6mr5aj9f58zp6fyjzup6ywn3x6sk8akg5v4tgn2q8g4fhx05wf6juaxu9760yp46454gpg5mtzgerlzezqcqvjnhjh8z3g2qqdhhwkj";
    private static final String INVOICE_WITHOUT_AMOUNT = "lnbc1pvjluezsp5zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zygspp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq9qrsgq357wnc5r2ueh7ck6q93dj32dlqnls087fxdwk8qakdyafkq3yap9us6v52vjjsrvywa6rt52cm9r9zqt8r2t7mlcwspyetp5h2tztugp9lfyql";
    private static final String ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4";

    private final Identity sellerIdentity = identity("seller");
    private final Identity buyerIdentity = identity("buyer");

    @Test
    void invoiceForAnotherAmountIsRejected() {
        BisqEasyTrade trade = sellersTrade(BitcoinPaymentRail.LN);

        assertThrows(IllegalArgumentException.class, () -> handle(trade, INVOICE_OF_20_MILLI_BITCOIN));

        assertTrue(trade.getBitcoinPaymentData().isEmpty());
    }

    @Test
    void invoiceWithoutAmountIsAccepted() {
        BisqEasyTrade trade = sellersTrade(BitcoinPaymentRail.LN);

        assertDoesNotThrow(() -> handle(trade, INVOICE_WITHOUT_AMOUNT));

        assertEquals(Optional.of(INVOICE_WITHOUT_AMOUNT), trade.getBitcoinPaymentData());
    }

    @Test
    void bitcoinUriWithAnotherAmountIsRejected() {
        BisqEasyTrade trade = sellersTrade(BitcoinPaymentRail.MAIN_CHAIN);

        assertThrows(IllegalArgumentException.class, () -> handle(trade, "bitcoin:" + ADDRESS + "?amount=0.01"));

        assertTrue(trade.getBitcoinPaymentData().isEmpty());
    }

    @Test
    void addressIsAccepted() {
        BisqEasyTrade trade = sellersTrade(BitcoinPaymentRail.MAIN_CHAIN);

        assertDoesNotThrow(() -> handle(trade, ADDRESS));

        assertEquals(Optional.of(ADDRESS), trade.getBitcoinPaymentData());
    }

    private BisqEasyTrade sellersTrade(BitcoinPaymentRail paymentRail) {
        BisqEasyOffer offer = offer("offer-id", sellerIdentity.getNetworkId(), QUOTE_AMOUNT);
        return sellerAsMakerTrade(contract(offer, buyerIdentity.getNetworkId(), Optional.empty(), paymentRail),
                sellerIdentity);
    }

    private void handle(BisqEasyTrade trade, String bitcoinPaymentData) {
        BisqEasyBtcAddressMessage message = new BisqEasyBtcAddressMessage(StringUtils.createUid(),
                trade.getId(),
                BisqEasyProtocol.VERSION,
                buyerIdentity.getNetworkId(),
                sellerIdentity.getNetworkId(),
                bitcoinPaymentData,
                trade.getOffer());

        new BisqEasyBtcAddressMessageHandler(mock(ServiceProvider.class, RETURNS_DEEP_STUBS), trade).handle(message);
    }
}
