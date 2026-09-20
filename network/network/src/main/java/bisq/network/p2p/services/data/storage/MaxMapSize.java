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
 * <p>
 * A declared value below 10 000 has no effect on the store today. {@code DataStorageService.getMaxMapSize()} raises
 * whatever a type declares to at least {@link #SIZE_10_000}, with a comment saying it does so until the too low
 * values are fixed, so {@link #SIZE_100} reads as a 100 entry cap while the store holds 10 000. That predates the
 * annotation, which only moved where the value is written, but it means these constants are not yet the truth about
 * the cap.
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
