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

package bisq.presentation.parser;

import bisq.common.asset.Asset;
import bisq.common.asset.CryptoAsset;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountParser {
    public static Monetary parse(String value, String code) {
        if (Asset.isFiat(code)) {
            return Fiat.parse(normalize(value, Fiat.PRECISION), code);
        } else {
            CryptoAsset cryptoAsset = CryptoAssetRepository.findOrCreateCustom(code);
            return Coin.parse(normalize(value, cryptoAsset.getPrecision()), code);
        }
    }

    /**
     * Brings a number string in the user input format (grouping whitespaces, comma or point as
     * decimal separator) into the format the Monetary parse methods expect and drops the fraction
     * digits the asset cannot represent. The cut follows the precision of the asset: cutting every
     * asset at 8 digits dropped the 4 lowest digits of an XMR amount, which has a precision of 12.
     */
    private static String normalize(String value, int precision) {
        String cleaned = StringUtils.removeAllWhitespaces(value).replace(",", ".");
        BigDecimal parsed = new BigDecimal(cleaned);
        // Only ever reduce the scale, and leave input which needs no cut untouched. Expanding input
        // like 1E1000000000 would build a huge number; it is out of range anyway and the Monetary
        // parse methods reject it without expanding it.
        return parsed.scale() > precision
                ? parsed.setScale(precision, RoundingMode.DOWN).toPlainString()
                : cleaned;
    }
}
