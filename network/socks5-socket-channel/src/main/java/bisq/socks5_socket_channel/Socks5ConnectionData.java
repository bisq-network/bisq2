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

package bisq.socks5_socket_channel;

import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
public class Socks5ConnectionData {
    private final InetSocketAddress socks5ProxySocketAddress;
    private final String destinationHostName;
    private final int destinationPort;

    public Socks5ConnectionData(InetSocketAddress socks5ProxySocketAddress, String destinationHostName, int destinationPort) {
        this.socks5ProxySocketAddress = socks5ProxySocketAddress;
        this.destinationHostName = destinationHostName;
        this.destinationPort = destinationPort;
    }
}
