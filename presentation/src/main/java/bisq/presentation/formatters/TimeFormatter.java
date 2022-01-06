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

package bisq.presentation.formatters;

public class TimeFormatter {
    public static String formatTime(long duration) {
        //todo
        if (duration < 1000) {
            return duration + " ms";
        } else {
            long sec = duration / 1000;
            long ms = duration % 1000;
            if (ms == 0) {
                return sec + " sec";
            } else {
                return sec + " sec, " + ms + " ms";
            }
        }
    }
}
