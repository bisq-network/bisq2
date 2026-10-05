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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The amounts which the data for a bitcoin payment requests from the payer: the amount of a BOLT 11 Lightning invoice
 * and the amount parameter of a BIP 21 bitcoin URI. A wallet which pays from such data, for example from its QR code,
 * pays these amounts. A BOLT 12 offer or invoice requests an amount which this class cannot read.
 * See docs/specifications/trade/bisq-easy-bitcoin-payment-data.md.
 */
public final class RequestedAmountValidation {
    // A mainnet BOLT 11 invoice: "lnbc", an optional amount, the separator "1" and the bech32 data part, which does not
    // contain "1", "b", "i" or "o". The amount is a number of bitcoin with an optional multiplier. The pattern only
    // looks ahead at the data part, so that an invoice right after the separator of another one is read too.
    private static final Pattern LIGHTNING_INVOICE = Pattern.compile("lnbc([0-9]*)([munp]?)1(?=[02-9ac-hj-np-z])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BOLT_12_OFFER_OR_INVOICE = Pattern.compile("ln[oi]1[02-9ac-hj-np-z]+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BITCOIN_URI_AMOUNT = Pattern.compile("[?&]amount=([^&]*)", Pattern.CASE_INSENSITIVE);
    // Whitespace, control and format characters, which a wallet can remove from the data, and "+", which form decoding
    // turns into a space
    private static final Pattern REMOVABLE_CHARACTERS = Pattern.compile("[\\s\\p{Z}\\p{Cc}\\p{Cf}+]");
    private static final int MAX_DECODING_ROUNDS = 3;
    private static final Pattern BITCOIN_AMOUNT = Pattern.compile("[0-9]+(\\.[0-9]*)?|\\.[0-9]+");
    private static final BigDecimal SATOSHI_PER_BITCOIN = BigDecimal.valueOf(100_000_000L);
    private static final BigInteger MILLISATOSHI_PER_SATOSHI = BigInteger.valueOf(1000);

    private RequestedAmountValidation() {
    }

    /**
     * Whether every amount which the payment data requests is the given amount. Data which requests no amount, like a
     * bitcoin address or an invoice without amount, lets the payer enter the amount. An amount which cannot be read
     * counts as another amount. A wallet can percent-decode the data and remove whitespace from it, in any order,
     * before it reads the amount, so each of these readings must request only the given amount.
     */
    public static boolean requestsOnlyAmount(String paymentData, long amountInSatoshi) {
        return getReadings(paymentData).stream()
                .allMatch(reading -> requestsOnlyAmountAsRead(reading, amountInSatoshi));
    }

    public static void checkRequestsOnlyAmount(String paymentData, long amountInSatoshi) {
        checkArgument(requestsOnlyAmount(paymentData, amountInSatoshi),
                "Bitcoin payment data must not request another amount than the trade amount");
    }

    // The data as written and percent-decoded up to MAX_DECODING_ROUNDS times, with the removable characters kept or
    // removed before and after each decoding
    private static Set<String> getReadings(String paymentData) {
        Set<String> readings = new LinkedHashSet<>();
        readings.add(paymentData);
        readings.add(withoutRemovableCharacters(paymentData));
        for (int round = 0; round < MAX_DECODING_ROUNDS; round++) {
            Set<String> decodedReadings = new LinkedHashSet<>();
            readings.forEach(reading -> {
                String decoded = percentDecode(reading);
                decodedReadings.add(decoded);
                decodedReadings.add(withoutRemovableCharacters(decoded));
            });
            readings.addAll(decodedReadings);
        }
        return readings;
    }

    private static String withoutRemovableCharacters(String value) {
        return REMOVABLE_CHARACTERS.matcher(value).replaceAll("");
    }

    private static boolean requestsOnlyAmountAsRead(String paymentData, long amountInSatoshi) {
        if (BOLT_12_OFFER_OR_INVOICE.matcher(paymentData).find()) {
            return false;
        }
        BigInteger amountInMillisatoshi = BigInteger.valueOf(amountInSatoshi).multiply(MILLISATOSHI_PER_SATOSHI);
        Matcher invoice = LIGHTNING_INVOICE.matcher(paymentData);
        while (invoice.find()) {
            if (!isInvoiceAmountOrNone(invoice.group(1), invoice.group(2), amountInMillisatoshi)) {
                return false;
            }
        }
        Matcher uriAmount = BITCOIN_URI_AMOUNT.matcher(paymentData);
        while (uriAmount.find()) {
            if (!isBitcoinAmount(uriAmount.group(1), amountInSatoshi)) {
                return false;
            }
        }
        return true;
    }

    // BOLT 11: the number has no leading zeros, and with the multiplier p (a tenth of a millisatoshi) it ends with 0.
    private static boolean isInvoiceAmountOrNone(String number, String multiplier, BigInteger amountInMillisatoshi) {
        if (number.isEmpty()) {
            return multiplier.isEmpty();
        }
        if (number.startsWith("0")) {
            return false;
        }
        BigInteger value = new BigInteger(number);
        String lowerCaseMultiplier = multiplier.toLowerCase(Locale.ROOT);
        if (lowerCaseMultiplier.equals("p")) {
            return value.mod(BigInteger.TEN).signum() == 0 &&
                    value.divide(BigInteger.TEN).equals(amountInMillisatoshi);
        }
        long millisatoshiPerUnit = switch (lowerCaseMultiplier) {
            case "m" -> 100_000_000L;
            case "u" -> 100_000L;
            case "n" -> 100L;
            default -> 100_000_000_000L;
        };
        return value.multiply(BigInteger.valueOf(millisatoshiPerUnit)).equals(amountInMillisatoshi);
    }

    // BIP 21: the amount is a decimal number of bitcoin
    private static boolean isBitcoinAmount(String value, long amountInSatoshi) {
        return BITCOIN_AMOUNT.matcher(value).matches() &&
                new BigDecimal(value).multiply(SATOSHI_PER_BITCOIN).compareTo(BigDecimal.valueOf(amountInSatoshi)) == 0;
    }

    // Decodes each %XX as a byte of UTF-8, as for a URI. Other characters, also "+", stay as they are.
    private static String percentDecode(String value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int index = 0;
        while (index < value.length()) {
            if (value.charAt(index) == '%' && index + 2 < value.length() &&
                    Character.digit(value.charAt(index + 1), 16) >= 0 &&
                    Character.digit(value.charAt(index + 2), 16) >= 0) {
                bytes.write(Integer.parseInt(value.substring(index + 1, index + 3), 16));
                index += 3;
            } else {
                int codePoint = value.codePointAt(index);
                bytes.writeBytes(new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8));
                index += Character.charCount(codePoint);
            }
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
