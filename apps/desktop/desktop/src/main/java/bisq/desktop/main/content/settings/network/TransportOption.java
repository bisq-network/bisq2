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

package bisq.desktop.main.content.settings.network;

import bisq.common.network.TransportType;
import lombok.Getter;

import java.util.Set;

public enum TransportOption {
    TOR_AND_I2P(Set.of(TransportType.TOR, TransportType.I2P)),
    TOR(Set.of(TransportType.TOR)),
    I2P(Set.of(TransportType.I2P)),
    CLEAR(Set.of(TransportType.CLEAR));

    @Getter
    private final Set<TransportType> transportTypes;

    TransportOption(Set<TransportType> transportTypes) {
        this.transportTypes = transportTypes;
    }
}
