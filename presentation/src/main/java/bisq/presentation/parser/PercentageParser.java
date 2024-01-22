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

import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;

public class PercentageParser {

    /**
     * @param input The string input to parse to a normalized percentage input (1=100%)
     * @return the percentage input if the input was valid, otherwise 0.
     */
    public static double unsafeParse(String input) {
        try {
            return parse(input);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    /**
     * @param input The string input to parse to a normalized percentage input (1=100%)
     * @return the percentage input if the input was valid.
     * @throws NumberFormatException thrown if input was invalid
     */
    public static double parse(String input) throws NumberFormatException {
        try {
            input = StringUtils.removeAllWhitespaces(input);
            input = input.replace("%", "");
            input = input.replace(",", ".");
            return Double.parseDouble(input) / 100d;
        } catch (NumberFormatException e) {
            throw e;
        } catch (Throwable t) {
            throw new NumberFormatException(ExceptionUtil.getRootCauseMessage(t));
        }
    }
}