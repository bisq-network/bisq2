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

package bisq.common.encoding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

// See: https://en.bitcoin.it/wiki/BIP_0021
public class BitcoinURIScheme {
    public static String extractBitcoinAddress(String input) {
        input = normalizePrefix(input);
        String[] tokens = input.split("bitcoin:");
        if (tokens.length == 1) {
            return input;
        } else {
            String[] withParams = tokens[1].split("\\?");
            return withParams[0];
        }
    }

    public static boolean isBitcoinUriScheme(String input) {
        return input.toLowerCase(Locale.ROOT).startsWith("bitcoin:");
    }

    public static String normalizePrefix(String input) {
        if (input.startsWith("BITCOIN:")) {
            input = input.replace("BITCOIN:", "bitcoin:");
        }
        return input;
    }

    /**
     * Build a BIP21 bitcoin: URI from address and optional amount/label.
     * Examples:
     * bitcoin:bc1qxyz...
     * bitcoin:bc1qxyz...?amount=0.001
     * bitcoin:bc1qxyz...?label=Savings
     * bitcoin:bc1qxyz...?amount=0.001&label=Savings
     */
    public static String buildBitcoinUri(String address, Optional<String> amount, Optional<String> label) {
        StringBuilder uri = new StringBuilder("bitcoin:").append(address);

        List<String> params = new ArrayList<>(2);
        amount.filter(s -> !s.isBlank())
                .ifPresent(value -> params.add("amount=" + value));
        label.filter(s -> !s.isBlank())
                .ifPresent(value -> params.add(
                        "label=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));

        if (!params.isEmpty()) {
            uri.append('?').append(String.join("&", params));
        }
        return uri.toString();
    }
}
