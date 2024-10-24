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

package bisq.common.network;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AndroidEmulatorLocalhostFacade implements LocalhostFacade {
    public Address toMyLocalhost(int port) {
        log.info("The android app is running in the emulator. We convert our localhost " +
                "address to `10.0.2.15`");
        return new Address("10.0.2.15", port);
    }

    public Address toPeersLocalhost(Address address) {
        if (address.isLocalhost()) {
            log.info("The android app is running in the emulator. We convert the target localhost " +
                    "address `127.0.0.1` to `10.0.2.2`");
            return new Address("10.0.2.2", address.getPort());
        } else {
            return address;
        }
    }
}
