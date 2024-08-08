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

import java.util.regex.Pattern;

/**
 * Utility class for Bitcoin identifying if a given string matches the pattern related to a BTC characteristic.
 * Initially thought to be used for addresses and transaction IDs.
 *
 * @author Rodrigo Varela
 */
public class BitcoinAddressValidation {

    // Regular expressions for different Bitcoin address formats
    private static final Pattern P2PKH_PATTERN = Pattern.compile("^[1][A-Za-z0-9]{26,34}$");
    private static final Pattern P2SH_PATTERN = Pattern.compile("^[3][A-Za-z0-9]{26,34}$");
    private static final Pattern BECH32_PATTERN = Pattern.compile("^(bc1|tb1)[a-z0-9]{25,39}$");

    /**
     * Checks if the given string is a Bitcoin address.
     *
     * @param address The string to be checked.
     * @return True if it is a Bitcoin address, false otherwise.
     */
    public static boolean validateAddress(String address) {
        return isValidP2PKH(address) ||
                isValidP2SH(address) ||
                isValidBech32(address);
    }

    private static boolean isValidBech32(String address) {
        return BECH32_PATTERN.matcher(address).matches();
    }

    private static boolean isValidP2SH(String address) {
        return P2SH_PATTERN.matcher(address).matches();
    }

    private static boolean isValidP2PKH(String address) {
        return P2PKH_PATTERN.matcher(address).matches();
    }
}
