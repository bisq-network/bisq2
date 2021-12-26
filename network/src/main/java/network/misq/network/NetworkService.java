/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;
import network.misq.common.util.NetworkUtils;
import network.misq.network.http.HttpService;
import network.misq.network.http.common.BaseHttpClient;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.ServiceNodesByTransport;
import network.misq.network.p2p.State;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.broadcast.BroadcastResult;
import network.misq.network.p2p.services.confidential.ConfidentialService;
import network.misq.network.p2p.services.data.DataService;
import network.misq.network.p2p.services.data.NetworkPayload;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.security.KeyPairRepository;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * High level API for network access to p2p network as well to http services (over Tor). If user has only I2P selected
 * for p2p network a tor instance will be still bootstrapped for usage for the http requests. Only if user has
 * clearNet enabled clearNet is used for https.
 */
@Slf4j
public class NetworkService {
    // NETWORK_IO_POOL must be used only inside network module.
    // The maximumPoolSize depends on the number of expected connections and nodes. Each node has 1 blocking IO thread 
    // at ServerSocket.accept. Each connection has 1 blocking IO thread at InputStream.read and 1 thread at 
    // OutputStream.write which is only short term blocking while writing. Beside that there is 1 blocking IO thread at 
    // ConnectionHandshake which is active while handshake is in progress.
    // If Tor and I2P are used there are additional threads at startup. 
    // The PeerGroupService usually has about 12 connections, so that's 24 threads + 12 at sending messages. 
    // If a user has 10 offers with dedicated nodes and 5 connections open, its another 100 threads + 50 at sending 
    // messages. 100-200 threads might be a usual scenario, but it could also peak much higher, so we will give 
    // maximumPoolSize sufficient headroom and use a rather short keepAliveTimeInSec.
    public static final ThreadPoolExecutor NETWORK_IO_POOL = ExecutorFactory.getThreadPoolExecutor("NETWORK_IO_POOL",
            10,
            50000,
            10,
            new SynchronousQueue<>());

    public static record Config(String baseDir,
                                Transport.Config transportConfig,
                                Set<Transport.Type> supportedTransportTypes,
                                ServiceNode.Config serviceNodeConfig,
                                Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                Map<Transport.Type, List<Address>> seedAddressesByTransport,
                                Optional<String> socks5ProxyAddress) {
    }

    @Getter
    private final HttpService httpService;
    @Getter
    private final Optional<String> socks5ProxyAddress; // Optional proxy address of external tor instance 
    @Getter
    private final Set<Transport.Type> supportedTransportTypes;
    @Getter
    private final ServiceNodesByTransport serviceNodesByTransport;
    @Getter
    public State state = State.INIT;

    public NetworkService(Config config, KeyPairRepository keyPairRepository) {
        httpService = new HttpService();
        socks5ProxyAddress = config.socks5ProxyAddress;
        supportedTransportTypes = config.supportedTransportTypes();
        serviceNodesByTransport = new ServiceNodesByTransport(config.transportConfig(),
                supportedTransportTypes,
                config.serviceNodeConfig(),
                config.peerGroupServiceConfigByTransport,
                config.seedAddressesByTransport(),
                new DataService.Config(config.baseDir()),
                keyPairRepository);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> bootstrap() {
        return bootstrap(NetworkUtils.findFreeSystemPort());
    }

    public CompletableFuture<Boolean> bootstrap(int port) {
        return bootstrap(port, null);
    }

    public CompletableFuture<Boolean> bootstrap(int port, @Nullable BiConsumer<Boolean, Throwable> resultHandler) {
        setState(State.BOOTSTRAPPING);
        return serviceNodesByTransport.bootstrap(port, resultHandler)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        setState(State.BOOTSTRAPPED);
                    }
                });
    }

    public CompletableFuture<Void> shutdown() {
        setState(State.SHUTDOWN_STARTED);
        CountDownLatch latch = new CountDownLatch(2);
        return CompletableFuture.runAsync(() -> {
            serviceNodesByTransport.shutdown().whenComplete((v, t) -> latch.countDown());
            httpService.shutdown().whenComplete((v, t) -> latch.countDown());
            try {
                if (!latch.await(1, TimeUnit.SECONDS)) {
                    log.error("Shutdown interrupted by timeout");
                }
            } catch (InterruptedException e) {
                log.error("Shutdown interrupted", e);
            } finally {
                setState(State.SHUTDOWN_COMPLETE);
            }
        });
    }

    public CompletableFuture<ConfidentialService.Result> confidentialSend(Message message, 
                                                                          NetworkId peerNetworkId, 
                                                                          KeyPair myKeyPair,
                                                                          String connectionId) {
        return serviceNodesByTransport.confidentialSend(message, peerNetworkId, myKeyPair, connectionId);
    }

    public CompletableFuture<List<BroadcastResult>> addNetworkPayload(NetworkPayload networkPayload, KeyPair keyPair) {
        return serviceNodesByTransport.addNetworkPayload(networkPayload, keyPair);
    }

    public void addMessageListener(Node.Listener listener) {
        serviceNodesByTransport.addMessageListener(listener);
    }

    public void removeMessageListener(Node.Listener listener) {
        serviceNodesByTransport.removeMessageListener(listener);
    }

    public CompletableFuture<BaseHttpClient> getHttpClient(String url, String userAgent, Transport.Type transportType) {
        return httpService.getHttpClient(url, userAgent, transportType, serviceNodesByTransport.getSocksProxy(), socks5ProxyAddress);
    }

    public Map<Transport.Type, Map<String, Address>> findMyAddresses() {
        return serviceNodesByTransport.findMyAddresses();
    }

    public Optional<Map<String, Address>> findMyAddresses(Transport.Type transport) {
        return serviceNodesByTransport.findMyAddresses(transport);
    }

    public Optional<Address> findMyAddresses(Transport.Type transport, String nodeId) {
        return serviceNodesByTransport.findMyAddresses(transport, nodeId);
    }

    public Optional<Address> findMyDefaultAddresses(Transport.Type transport) {
        return serviceNodesByTransport.findMyAddresses(transport, Node.DEFAULT_NODE_ID);
    }

    public Optional<Node> findDefaultNode(Transport.Type transport) {
        return findNode(transport, Node.DEFAULT_NODE_ID);
    }

    public Optional<Node> findNode(Transport.Type transport, String nodeId) {
        return serviceNodesByTransport.findNode(transport, nodeId);
    }

    public Optional<ServiceNode> findServiceNode(Transport.Type transport) {
        return serviceNodesByTransport.findServiceNode(transport);
    }

    private void setState(State state) {
        checkArgument(this.state.ordinal() < state.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", state, this.state);
        this.state = state;
    }
}
