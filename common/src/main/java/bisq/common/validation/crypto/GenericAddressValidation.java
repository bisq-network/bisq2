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

package bisq.common.validation.crypto;

import bisq.common.validation.RegexValidation;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * If we do not have a specifically provided validation implementation we use that generic one.
 * <p>
 * - Most addresses use:
 * - Base58 (Bitcoin, Litecoin, etc.)
 * - Bech32 (starts with bc1, ltc1, etc.)
 * - Hex (e.g., Ethereum: 0x-prefixed 40 hex chars)
 * - Length Range: Usually 25–80 characters, depending on encoding.
 * - Alphanumeric (Ripple, Stellar, etc.)
 * <p>
 * We do not check:
 * - Checksum validation
 * - Prefix enforcement
 * - Network discrimination
 */
public class GenericAddressValidation implements RegexValidation {
    // supports Ethereum-like and base58 formats
    @Getter
    public final Pattern pattern = Pattern.compile("^(0x)?[a-zA-Z0-9]{25,80}$");
    @Getter
    public final String i18nKey = "validation.address.invalid";
    private static GenericAddressValidation instance;

    public static GenericAddressValidation getInstance() {
        if (instance == null) {
            instance = new GenericAddressValidation();
        }
        return instance;
    }
}
