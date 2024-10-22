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

package bisq.network.common;

/**
 * We do not use a protobuf enum for Type as it is used as key in a protobuf map and that does not support enums.
 */
public enum TransportType {
    TOR,
    I2P,
    CLEAR;

    public static TransportType from(Address address) {
        if (address.isClearNetAddress()) {
            return TransportType.CLEAR;
        } else if (address.isTorAddress()) {
            return TransportType.TOR;
        } else if (address.isI2pAddress()) {
            return TransportType.I2P;
        } else {
            throw new IllegalArgumentException("Could not resolve transportType from address " + address);
        }
    }
}
