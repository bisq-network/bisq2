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

import bisq.common.network.Address;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.handshake.InboundHandshakeHandler;
import bisq.network.p2p.node.handshake.OutboundHandshakeHandler;
import bisq.security.keys.KeyBundle;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import io.netty.channel.Channel;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;

public interface TransportService {
    enum TransportState {
        NEW,
        INITIALIZE,
        INITIALIZED,
        STOPPING,
        TERMINATED
    }

    static TransportService create(TransportType transportType, TransportConfig config) {
        return switch (transportType) {
            case TOR -> new TorTransportService(config);
            case I2P -> new I2PTransportService(config);
            case CLEAR -> new ClearNetTransportService(config);
        };
    }

    void initialize();

    CompletableFuture<Boolean> shutdown();

    default CompletableFuture<Address> createNettyServer(NetworkId networkId,
                                                         KeyBundle keyBundle) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<Address> startNettyServer(NetworkId networkId,
                                                       KeyBundle keyBundle,
                                                       Supplier<InboundHandshakeHandler> handshakeHandlerSupplier) {
        return CompletableFuture.completedFuture(null);
    }

    ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle, String nodeId);

    Socket getSocket(Address address, String nodeId) throws IOException;

    default CompletableFuture<Channel> connect(Address address, Supplier<OutboundHandshakeHandler> handshakeHandlerSupplier) {
        return null;
    }

    default Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.empty();
    }

    CompletableFuture<Boolean> isPeerOnlineAsync(Address address, String nodeId);

    default void setTransportState(TransportState newTransportState) {
        if (newTransportState == getTransportState().get()) {
            return;
        }
        checkArgument(getTransportState().get().ordinal() < newTransportState.ordinal(),
                "New getState() %s must have a higher ordinal as the current getState() %s", newTransportState, getTransportState().get());
        getTransportState().set(newTransportState);
        getTimestampByTransportState().put(newTransportState, System.currentTimeMillis());
    }

    Observable<TransportState> getTransportState();

    ObservableHashMap<TransportState, Long> getTimestampByTransportState();

    ObservableHashMap<NetworkId, Long> getInitializeServerSocketTimestampByNetworkId();

    ObservableHashMap<NetworkId, Long> getInitializedServerSocketTimestampByNetworkId();

    CompletableFuture<Address> evaluateMyAddress(NetworkId networkId, KeyBundle keyBundle);
}
