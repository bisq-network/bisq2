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

package bisq.common.validation;

import org.junit.jupiter.api.Test;

import static bisq.common.validation.RequestedAmountValidation.requestsOnlyAmount;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestedAmountValidationTest {
    // 0.001 BTC
    private static final long AMOUNT = 100_000;
    // Invoices of the BOLT 11 specification: 20m is 0.02 BTC, the second has no amount
    private static final String INVOICE_OF_20_MILLI_BITCOIN = "lnbc20m1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqhp58yjmdan79s6qqdhdzgynm4zwqd5d7xmw5fk98klysy043l2ahrqsfpp3qjmp7lwpagxun9pygexvgpjdc4jdj85fr9yq20q82gphp2nflc7jtzrcazrra7wwgzxqc8u7754cdlpfrmccae92qgzqvzq2ps8pqqqqqqpqqqqq9qqqvpeuqafqxu92d8lr6fvg0r5gv0heeeqgcrqlnm6jhphu9y00rrhy4grqszsvpcgpy9qqqqqqgqqqqq7qqzqj9n4evl6mr5aj9f58zp6fyjzup6ywn3x6sk8akg5v4tgn2q8g4fhx05wf6juaxu9760yp46454gpg5mtzgerlzezqcqvjnhjh8z3g2qqdhhwkj";
    private static final String INVOICE_WITHOUT_AMOUNT = "lnbc1pvjluezsp5zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zygspp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq9qrsgq357wnc5r2ueh7ck6q93dj32dlqnls087fxdwk8qakdyafkq3yap9us6v52vjjsrvywa6rt52cm9r9zqt8r2t7mlcwspyetp5h2tztugp9lfyql";
    private static final String ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4";

    @Test
    void dataWithoutAmountRequestsNoOtherAmount() {
        assertTrue(requestsOnlyAmount(ADDRESS, AMOUNT));
        assertTrue(requestsOnlyAmount("bitcoin:" + ADDRESS + "?label=Bisq", AMOUNT));
        assertTrue(requestsOnlyAmount(INVOICE_WITHOUT_AMOUNT, AMOUNT));
        assertTrue(requestsOnlyAmount("lightning:" + INVOICE_WITHOUT_AMOUNT.toUpperCase(), AMOUNT));
        assertTrue(requestsOnlyAmount("someone@example.com", AMOUNT));
        assertTrue(requestsOnlyAmount("bitcoin:" + ADDRESS + "?label=Bisq%20Easy", AMOUNT));
        assertTrue(requestsOnlyAmount("bitcoin:" + ADDRESS + "?label=Bisq+Easy", AMOUNT));
    }

    @Test
    void invoiceForTheAmountIsAccepted() {
        assertTrue(requestsOnlyAmount(INVOICE_OF_20_MILLI_BITCOIN, 2_000_000));
        // The amount with each multiplier, for 0.001 BTC
        assertTrue(requestsOnlyAmount("lnbc1m1pvjluez", AMOUNT));
        assertTrue(requestsOnlyAmount("lnbc1000u1pvjluez", AMOUNT));
        assertTrue(requestsOnlyAmount("lnbc1000000n1pvjluez", AMOUNT));
        assertTrue(requestsOnlyAmount("lnbc1000000000p1pvjluez", AMOUNT));
        assertTrue(requestsOnlyAmount("LNBC1M1PVJLUEZ", AMOUNT));
    }

    @Test
    void invoiceForAnotherAmountIsRejected() {
        assertFalse(requestsOnlyAmount(INVOICE_OF_20_MILLI_BITCOIN, AMOUNT));
        assertFalse(requestsOnlyAmount("lightning:" + INVOICE_OF_20_MILLI_BITCOIN, AMOUNT));
        assertFalse(requestsOnlyAmount("LIGHTNING:" + INVOICE_OF_20_MILLI_BITCOIN.toUpperCase(), AMOUNT));
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?amount=0.001&lightning=" + INVOICE_OF_20_MILLI_BITCOIN,
                AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc1pvjluez " + INVOICE_OF_20_MILLI_BITCOIN, AMOUNT));
        // An invoice right after the separator of another one
        assertFalse(requestsOnlyAmount("lnbc1" + INVOICE_OF_20_MILLI_BITCOIN, AMOUNT));
        // Less than the amount, or not a whole satoshi
        assertFalse(requestsOnlyAmount("lnbc999u1pvjluez", AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc1000000001p1pvjluez", AMOUNT));
    }

    // A wallet decodes a bitcoin URI before it reads its parameters, and some wallets remove whitespace from the data,
    // before or after decoding
    @Test
    void encodedOrSplitInvoiceForAnotherAmountIsRejected() {
        String dataPart = INVOICE_OF_20_MILLI_BITCOIN.substring("lnbc20m1".length());
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?lightning=lnbc2%30m1" + dataPart, AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc20m%31" + dataPart, AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc2%2530m1" + dataPart, AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc 20m1" + dataPart, AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc\n20m1" + dataPart, AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc\u200b20m1" + dataPart, AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc2\u001f0m1" + dataPart, AMOUNT));
        // Whitespace removed before decoding, and "+", which form decoding turns into a space
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?lightning=lnbc2%3 0m1" + dataPart, AMOUNT));
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?lightning=lnbc2+0m1" + dataPart, AMOUNT));
        assertTrue(requestsOnlyAmount("bitcoin:" + ADDRESS + "?lightning=lnbc1%30%30%30u1" + dataPart, AMOUNT));
    }

    // BOLT 12 offers and invoices carry their amount in the data part
    @Test
    void bolt12OfferOrInvoiceIsRejected() {
        assertFalse(requestsOnlyAmount("lno1pqps7sjqpgtyzm3qv4uxzmtsd3jjqer9wd3hy6tsw35k7msjzfpy7nz5yqcnygrfdej82um5wf5k2uckyypwa3eyt44h6txtxquqh7lz5djge4afgfjn7k4rgrkuag0jsd5xvxg", AMOUNT));
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?lno=lno1pqps7sjqpgtyzm3qv4uxzmtsd3jjqer9wd3hy6tsw35k7msjzfpy7nz5yqcnygrfdej82um5wf5k2uckyypwa3eyt44h6txtxquqh7lz5djge4afgfjn7k4rgrkuag0jsd5xvxg", AMOUNT));
        assertFalse(requestsOnlyAmount("lni1qqgqvpnlwgdhq7kpcs8mv5r4dmusx6vkljqxyw3r2yfm7uh6y8yy6qszmt87gwcrlcuw2x80x9", AMOUNT));
    }

    @Test
    void invoiceAmountWhichCannotBeReadIsRejected() {
        // A multiplier without number, a leading zero, a p amount which is not a whole millisatoshi
        assertFalse(requestsOnlyAmount("lnbcm1pvjluez", AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc01m1pvjluez", AMOUNT));
        assertFalse(requestsOnlyAmount("lnbc1000000005p1pvjluez", AMOUNT));
    }

    @Test
    void bitcoinUriWithTheAmountIsAccepted() {
        assertTrue(requestsOnlyAmount("bitcoin:" + ADDRESS + "?amount=0.001", AMOUNT));
        assertTrue(requestsOnlyAmount("bitcoin:" + ADDRESS + "?label=Bisq&amount=0.00100000", AMOUNT));
        assertTrue(requestsOnlyAmount("BITCOIN:" + ADDRESS.toUpperCase() + "?AMOUNT=.001", AMOUNT));
    }

    @Test
    void bitcoinUriWithAnotherAmountIsRejected() {
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?amount=0.01", AMOUNT));
        assertFalse(requestsOnlyAmount("BITCOIN:" + ADDRESS.toUpperCase() + "?AMOUNT=0.01", AMOUNT));
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?amount=0.001&amount=0.01", AMOUNT));
        // Not a whole satoshi, or no decimal number
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?amount=0.0010000001", AMOUNT));
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?amount=1e-3", AMOUNT));
        assertFalse(requestsOnlyAmount("bitcoin:" + ADDRESS + "?amount=", AMOUNT));
        // An encoded parameter name, also encoded twice
        assertFalse(requestsOnlyAmount("bitcoin:1BoatSLRHtKNngkdXEeobR76b53LETtpyT?%61mount=0.01", AMOUNT));
        assertFalse(requestsOnlyAmount("bitcoin:1BoatSLRHtKNngkdXEeobR76b53LETtpyT?%2561mount=0.01", AMOUNT));
    }
}
