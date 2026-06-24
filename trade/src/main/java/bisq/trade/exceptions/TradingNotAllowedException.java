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

package bisq.trade.exceptions;

/**
 * Thrown when a trade action is blocked by a security manager alert (trading on halt or a min.
 * version required for trading). This is an expected, user-actionable condition, not a bug, so the
 * UI presents the message as a warning rather than a bug report.
 */
public class TradingNotAllowedException extends RuntimeException {
    public TradingNotAllowedException(String message) {
        super(message);
    }
}
