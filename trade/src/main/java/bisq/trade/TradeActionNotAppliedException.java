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

package bisq.trade;

/**
 * Thrown by a trade action of the user, for example sending the payment account data, when the protocol did not apply
 * it: the trade has no protocol anymore, is in a final state, or failed while handling the action. A caller must then
 * not send the trade log message of the action, which carries the same data, to the peer.
 */
public class TradeActionNotAppliedException extends IllegalStateException {
    public TradeActionNotAppliedException(String message) {
        super(message);
    }
}
