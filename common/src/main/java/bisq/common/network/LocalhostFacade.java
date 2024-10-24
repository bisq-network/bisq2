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

/**
 * For Android running in emulator we need to convert the localhost addresses to the IP addresses used by the emulator.
 * To support that we use a facade which by default use the loop-back address 127.0.0.1.
 */
public interface LocalhostFacade {
    Address toMyLocalhost(int port);

    default Address toPeersLocalhost(Address address) {
        return address;
    }
}
