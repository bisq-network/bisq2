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

import bisq.common.encoding.BitcoinURIScheme;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for Bitcoin identifying if a given string matches the pattern related to a BTC characteristic.
 * Initially thought to be used for addresses and transaction IDs.
 *
 * @author Rodrigo Varela
 */
public class BitcoinAddressValidation {
    public static final int MIN_LENGTH = 25;
    public static final int MAX_LENGTH = 62;
    private static final Pattern BASE58_PATTERN = Pattern.compile("^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$");
    private static final Pattern BECH32_PATTERN = Pattern.compile("^(?i)bc1[0-9a-z]{6,87}$");

    /**
     * Checks if the given string is a Bitcoin address.
     *
     * @param address The string to be checked.
     * @return True if it is a Bitcoin address, false otherwise.
     */
    public static boolean isValid(String address) {
        return BASE58_PATTERN.matcher(address).matches() ||
                BECH32_PATTERN.matcher(address).matches();
    }

    /**
     * Normalizes the payment data of a scanned QR code (rule 5 of
     * docs/specifications/trade/bisq-easy-bitcoin-payment-data.md). QR codes are more compact in upper case, so many
     * wallets encode the data in upper case. Bech32 addresses and Lightning invoices are case-insensitive and shown in
     * lower case. Base58 addresses are case-sensitive and keep their case.
     */
    public static String normalizeScannedPaymentData(String scannedData) {
        String paymentData = BitcoinURIScheme.isBitcoinUriScheme(scannedData)
                ? BitcoinURIScheme.extractBitcoinAddress("bitcoin:" + scannedData.substring("bitcoin:".length()))
                : scannedData;
        return BASE58_PATTERN.matcher(paymentData).matches()
                ? paymentData
                : paymentData.toLowerCase(Locale.ROOT);
    }

    public static String canonicalize(String address) {
        String extracted = BitcoinURIScheme.extractBitcoinAddress(address);
        // bech32 is case-insensitive and stored lowercase on-chain; base58 is case-sensitive
        return BECH32_PATTERN.matcher(extracted).matches()
                ? extracted.toLowerCase(Locale.ROOT)
                : extracted;
    }
}
