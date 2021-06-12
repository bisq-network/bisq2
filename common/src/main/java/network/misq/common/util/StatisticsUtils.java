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

package network.misq.common.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatisticsUtils {
    public static long medianFrom(Long[] list) {
        if (list.length == 0) {
            return 0L;
        }

        int middle = list.length / 2;
        long median;
        if (list.length % 2 == 1) {
            median = list[middle];
        } else {
            median = MathUtils.roundDoubleToLong((list[middle - 1] + list[middle]) / 2.0);
        }
        return median;
    }
}
