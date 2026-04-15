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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input;

import bisq.common.util.MathUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AmountTextInputLayout {
    public static final double WIDTH = 340;
    public static final double PADDING = 10;
    public static final double CODE_WIDTH = 30;
    public static final double CODE_SPACING = 10;

    private static final double MIN_FONT_SIZE_EM = 1.45;
    private static final double MAX_FONT_SIZE_EM = 4;

    private static final Map<Integer, Double> EM_SIZE_BY_TEXT_LENGTH = Map.ofEntries(
            Map.entry(9, 3.99),
            Map.entry(10, 3.55),
            Map.entry(11, 3.20),
            Map.entry(12, 2.90),
            Map.entry(13, 2.65),
            Map.entry(14, 2.45),
            Map.entry(15, 2.30),
            Map.entry(16, 2.15),
            Map.entry(17, 2.00),
            Map.entry(18, 1.90),
            Map.entry(19, 1.80),
            Map.entry(20, 1.68),
            Map.entry(21, 1.59),
            Map.entry(22, 1.50),
            Map.entry(23, 1.45)
    );

    public static double computeFontSize(int length) {
        length = MathUtils.bounded(9, 23, length);
        return EM_SIZE_BY_TEXT_LENGTH.get(length);
    }
}
