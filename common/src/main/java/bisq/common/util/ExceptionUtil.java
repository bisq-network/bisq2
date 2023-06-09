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

package bisq.common.util;

public class ExceptionUtil {

    /**
     * @param throwable The throwable we want to get a meaningful message from.
     * @return Returns either the message of the cause if available, otherwise the cause as string, if cause is not available
     * * it returns the message if available, otherwise the throwable as string.
     */
    public static String getMostMeaningfulMessage(Throwable throwable) {
        if (throwable.getCause() != null) {
            if (throwable.getCause().getMessage() != null) {
                return throwable.getCause().getMessage();
            } else {
                return throwable.getCause().toString();
            }
        } else if (throwable.getMessage() != null) {
            return throwable.getMessage();
        } else {
            return throwable.toString();
        }
    }
}