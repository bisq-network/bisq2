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

package network.misq.offer;

import network.misq.account.Transfer;
import network.misq.common.monetary.Monetary;
import network.misq.contract.AssetTransfer;

import java.util.List;

/**
 * @param monetary          The monetary value of the asset. Can be Fiat or Coin which carries the value, the currency
 *                          code and the smallestUnitExponent
 * @param transferTypes     The supported transferTypes for that asset (e.g. if user supports payment in SEPA and
 *                          Revolut). The order in the list can be used as priority.
 * @param assetTransferType The way how the transfer is execute. Either manual or automated (e.g. due wallet
 *                          integration or payment processor API)
 */
public record Asset(Monetary monetary,
                    List<? extends Transfer<? extends Transfer.Type>> transferTypes,
                    AssetTransfer.Type assetTransferType) {
    public long amount() {
        return monetary.getValue();
    }

    public String currencyCode() {
        return monetary.getCurrencyCode();
    }

    public int smallestUnitExponent() {
        return monetary.getSmallestUnitExponent();
    }
}
