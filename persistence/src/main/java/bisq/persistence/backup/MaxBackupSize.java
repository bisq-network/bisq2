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

import bisq.persistence.DbSubDirectory;
import lombok.Getter;

@Getter
public enum MaxBackupSize {
    HUNDRED_MB(100),
    TEN_MB(10),
    ZERO(0);

    public static MaxBackupSize from(DbSubDirectory dbSubDirectory) {
        return switch (dbSubDirectory) {
            case NETWORK_DB -> ZERO;
            case CACHE -> ZERO;
            case SETTINGS -> TEN_MB;
            case PRIVATE -> HUNDRED_MB;
            case WALLETS -> HUNDRED_MB;
        };
    }

    private final int sizeInMB;

    MaxBackupSize(int sizeInMB) {
        this.sizeInMB = sizeInMB;
    }
}
