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

package bisq.network.p2p.node.transport;

import bisq.network.common.TransportConfig;
import bisq.network.p2p.node.Address;
import bisq.tor.onionservice.CreateOnionServiceResponse;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Transport {

    static Transport create(TransportType transportType, TransportConfig config) {
        switch (transportType) {
            case TOR:
                return new TorTransport(config);
            case I2P:
                return new I2PTransport(config);
            case CLEAR:
                return new ClearNetTransport(config);
            default:
                throw new RuntimeException("Unhandled transportType");
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    final class ServerSocketResult {
        private final String nodeId;
        private final ServerSocket serverSocket;
        private final Address address;

        public ServerSocketResult(String nodeId, ServerSocket serverSocket, Address address) {
            this.nodeId = nodeId;
            this.serverSocket = serverSocket;
            this.address = address;
        }

        public ServerSocketResult(CreateOnionServiceResponse response) {
            this(response.getNodeId(), response.getServerSocket(), new Address(response.getOnionAddress()));
        }
    }

    CompletableFuture<Boolean> initialize();

    CompletableFuture<Boolean> shutdown();

    ServerSocketResult getServerSocket(int port, String nodeId);

    Socket getSocket(Address address) throws IOException;

    default Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.empty();
    }

    Optional<Address> getServerAddress(String serverId);

    boolean isPeerOnline(Address address);
}
