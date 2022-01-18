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

package bisq.offer;

import bisq.account.settlement.Settlement;
import bisq.common.monetary.Monetary;

import java.util.List;

/**
 * One side of a swap trade (e.g. bid, ask)
 *
 * @param monetary          The monetary value. Can be Fiat or Coin which carries the value, the currency
 *                          code and the smallestUnitExponent
 * @param settlementMethods The supported settlementMethods (e.g. if user supports payment in SEPA and
 *                          Revolut). The order in the list can be used as priority.
 */
public record SwapSide(Monetary monetary, List<? extends Settlement<? extends Settlement.Method>> settlementMethods) {
    public long amount() {
        return monetary.getValue();
    }

    public String code() {
        return monetary.getCode();
    }
}
