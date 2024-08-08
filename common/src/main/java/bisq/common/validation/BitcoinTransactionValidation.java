package bisq.common.validation;

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

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Utility class for Bitcoin identifying if a given string matches the pattern related to a BTC characteristic.
 * Initially thought to be used for addresses and transaction IDs.
 *
 * @author Rodrigo Varela
 */
public class BitcoinTransactionValidation {

    private static final Pattern TXID_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    /**
     * Checks if the given string is a Bitcoin transaction ID.
     *
     * @param txId The string to be checked.
     * @return True if it is a Bitcoin transaction ID, false otherwise.
     */
    public static boolean validateTransactionId(@NotNull String txId) {
        return TXID_PATTERN.matcher(txId).matches();
    }
}
