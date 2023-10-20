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
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TransportService {

    static TransportService create(TransportType transportType, TransportConfig config) {
        switch (transportType) {
            case TOR:
                return new TorTransportService(config);
            case I2P:
                return new I2PTransportService(config);
            case CLEAR:
                return new ClearNetTransportService(config);
            default:
                throw new RuntimeException("Unhandled transportType");
        }
    }

    void initialize();

    CompletableFuture<Boolean> shutdown();

    ServerSocketResult getServerSocket(int port, String nodeId);

    Socket getSocket(Address address) throws IOException;

    default Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.empty();
    }

    BootstrapInfo getBootstrapInfo();

    Optional<Address> getServerAddress(String serverId);

    boolean isPeerOnline(Address address);
}
