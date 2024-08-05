package bisq.common.validation;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/*
 * Copyright 2024 Rodrigo Varela
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Utility class for Bitcoin identifying if a given string matches the pattern related to a BTC characteristic.
 * Initially thought to be used for addresses and transaction IDs.
 *
 * @author Rodrigo Varela
 */
public class BitcoinDataValidation {

    // Regular expressions for different Bitcoin address formats
    private static final Pattern P2PKH_PATTERN = Pattern.compile("^[1][A-Za-z0-9]{26,34}$");
    private static final Pattern P2SH_PATTERN = Pattern.compile("^[3][A-Za-z0-9]{26,34}$");
    private static final Pattern BECH32_PATTERN = Pattern.compile("^(bc1|tb1)[a-z0-9]{25,39}$");
    private static final Pattern TXID_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    /**
     * Checks if the given string is a Bitcoin address.
     *
     * @param address The string to be checked.
     * @return True if it is a Bitcoin address, false otherwise.
     */
    public static boolean validateWalletAddressHash(@NotNull String address) {
        return !address.isBlank() && (P2PKH_PATTERN.matcher(address).matches() ||
                P2SH_PATTERN.matcher(address).matches() ||
                BECH32_PATTERN.matcher(address).matches());
    }

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
