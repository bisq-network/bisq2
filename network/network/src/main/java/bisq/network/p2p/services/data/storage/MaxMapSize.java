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

package bisq.network.p2p.services.data.storage;

import lombok.Getter;

/**
 * Upper bound for the number of entries a store keeps for a given data type. The value is what goes on the wire.
 */
@Getter
public enum MaxMapSize {
    SIZE_100(100),
    SIZE_1000(1000),
    SIZE_5000(5000),
    SIZE_10_000(10_000),
    SIZE_50_000(50_000);

    private final int value;

    MaxMapSize(int value) {
        this.value = value;
    }
}
