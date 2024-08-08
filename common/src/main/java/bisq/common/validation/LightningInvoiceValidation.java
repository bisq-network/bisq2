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

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class LightningInvoiceValidation {
    // There is no hard upper size limit, but 4096 seems to be common impl. limit
    // Min length is about 200-300 chars
    // Here about 190 chars are considered min lenght
    // https://bitcoin.stackexchange.com/questions/107930/what-are-the-minimum-and-maximum-lengths-of-a-lightning-invoice-address
    private static final Pattern LN_BECH32_PATTERN = Pattern.compile("^(lnbc)[a-z0-9]{150,4096}$");

    public static boolean validateInvoice(@NotNull String invoice) {
        return LN_BECH32_PATTERN.matcher(invoice).matches();
    }
}
