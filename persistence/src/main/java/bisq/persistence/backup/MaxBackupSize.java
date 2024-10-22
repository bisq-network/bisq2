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

package bisq.persistence.backup;

import bisq.common.data.ByteUnit;
import bisq.persistence.DbSubDirectory;
import lombok.Getter;

@Getter
public enum MaxBackupSize {
    ZERO(0),
    TEN_MB(ByteUnit.MB.toBytes(10)),
    HUNDRED_MB(ByteUnit.MB.toBytes(100));

    public static MaxBackupSize from(DbSubDirectory dbSubDirectory) {
        return switch (dbSubDirectory) {
            case NETWORK_DB -> ZERO;
            case CACHE -> ZERO;
            case SETTINGS -> TEN_MB;
            case PRIVATE -> HUNDRED_MB;
            case WALLETS -> HUNDRED_MB;
        };
    }

    private final double sizeInBytes;

    MaxBackupSize(double sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }
}
