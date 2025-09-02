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
import bisq.common.util.CompletableFutureUtils;
import bisq.network.i2p.I2PClient;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.transport.i2p.I2PRouterFacade;
import bisq.network.p2p.node.transport.i2p.RouterMode;
import bisq.security.keys.I2PKeyPair;
import bisq.security.keys.KeyBundle;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class I2PTransportService implements TransportService {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements TransportConfig {
        public static Config from(Path dataDir, com.typesafe.config.Config config) {
            return new Config(dataDir,
                    config.hasPath("defaultNodePort") ? config.getInt("defaultNodePort") : -1,
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")),
                    config.getInt("sendMessageThrottleTime"),
                    config.getInt("receiveMessageThrottleTime"),
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("connectTimeout")),
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("routerStartupTimeout")),
                    config.getInt("inboundKBytesPerSecond"),
                    config.getInt("outboundKBytesPerSecond"),
                    config.getInt("bandwidthSharePercentage"),
                    config.getString("i2cpHost"),
                    config.getInt("i2cpPort"),
                    config.getString("bi2pGrpcHost"),
                    config.getInt("bi2pGrpcPort"),
                    config.getBoolean("embeddedRouter"),
                    config.getConfigList("proxyList").stream()
                            .map(c -> new Address(c.getString("host"), c.getInt("port")))
                            .collect(Collectors.toList()));
        }

        private final int defaultNodePort;
        private final int socketTimeout;
        // How long should the client wait for the peer to accept a new connection (peers serverSocket accept) 
        private final int connectTimeout;
        private final int routerStartupTimeout;
        private final int inboundKBytesPerSecond;
        private final int outboundKBytesPerSecond;
        private final int bandwidthSharePercentage;
        private final int i2cpPort;
        private final String i2cpHost;
        private final String bi2pGrpcHost;
        private final int bi2pGrpcPort;
        private final boolean embeddedRouter;
        private final Path dataDir;
        private final int sendMessageThrottleTime;
        private final int receiveMessageThrottleTime;
        private final List<Address> proxyList;

        public Config(Path dataDir,
                      int defaultNodePort,
                      int socketTimeout,
                      int sendMessageThrottleTime,
                      int receiveMessageThrottleTime,
                      int connectTimeout,
                      int routerStartupTimeout,
                      int inboundKBytesPerSecond,
                      int outboundKBytesPerSecond,
                      int bandwidthSharePercentage,
                      String i2cpHost,
                      int i2cpPort,
                      String bi2pGrpcHost,
                      int bi2pGrpcPort,
                      boolean embeddedRouter,
                      List<Address> proxyList) {
            this.dataDir = dataDir;
            this.defaultNodePort = defaultNodePort;
            this.socketTimeout = socketTimeout;
            this.sendMessageThrottleTime = sendMessageThrottleTime;
            this.receiveMessageThrottleTime = receiveMessageThrottleTime;
            this.connectTimeout = connectTimeout;
            this.routerStartupTimeout = routerStartupTimeout;
            this.inboundKBytesPerSecond = inboundKBytesPerSecond;
            this.outboundKBytesPerSecond = outboundKBytesPerSecond;
            this.bandwidthSharePercentage = bandwidthSharePercentage;
            this.i2cpHost = i2cpHost;
            this.i2cpPort = i2cpPort;
            this.bi2pGrpcHost = bi2pGrpcHost;
            this.bi2pGrpcPort = bi2pGrpcPort;
            this.embeddedRouter = embeddedRouter;
            this.proxyList = proxyList;
        }
    }

    private final I2PRouterFacade i2pRouterFacade;
    @Getter
    private final RouterMode routerMode;
    private volatile I2PClient i2pClient;
    private final I2PTransportService.Config i2pConfig;
    @Getter
    public final Observable<TransportState> transportState = new Observable<>(TransportState.NEW);
    @Getter
    public final ObservableHashMap<TransportState, Long> timestampByTransportState = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializeServerSocketTimestampByNetworkId = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializedServerSocketTimestampByNetworkId = new ObservableHashMap<>();
    private volatile boolean initializeCalled;
    private volatile boolean isShutdownInProgress;

    public I2PTransportService(TransportConfig config) {
        i2pConfig = (I2PTransportService.Config) config;
        i2pRouterFacade = new I2PRouterFacade(i2pConfig);
        log.info("I2PTransport using I2CP {}:{}", i2pConfig.getI2cpHost(), i2pConfig.getI2cpPort());
        setTransportState(TransportState.NEW);
        routerMode = i2pRouterFacade.detectRouterMode();
    }

    @Override
    public void initialize() {
        if (initializeCalled) {
            return;
        }
        setTransportState(TransportState.INITIALIZE);
        initializeCalled = true;
        log.info("Initialize I2P");

        try {
            i2pRouterFacade.initialize(routerMode)
                    .orTimeout(i2pConfig.getRouterStartupTimeout(), TimeUnit.MILLISECONDS)
                    .get();

            Path clientDir = i2pConfig.getDataDir().resolve("client");
            i2pClient = new I2PClient(clientDir,
                    i2pConfig.getI2cpHost(),
                    i2pConfig.getI2cpPort(),
                    i2pConfig.getSocketTimeout(),
                    i2pConfig.getConnectTimeout());
            setTransportState(TransportState.INITIALIZED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Initialization interrupted", e);
            shutdown();
        } catch (Exception e) {
            log.error("Initialization failed", e);
            shutdown();
        }
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (!initializeCalled || isShutdownInProgress) {
            return CompletableFuture.completedFuture(true);
        }
        log.info("Shutdown I2P");
        isShutdownInProgress = true;
        initializeCalled = false;
        setTransportState(TransportState.STOPPING);

        initializeServerSocketTimestampByNetworkId.clear();
        initializedServerSocketTimestampByNetworkId.clear();
        timestampByTransportState.clear();

        Set<CompletableFuture<Boolean>> futures = new HashSet<>();
        futures.add(i2pRouterFacade.shutdown());
        if (i2pClient != null) {
            futures.add(i2pClient.shutdown().orTimeout(3, TimeUnit.SECONDS));
        }
        return CompletableFutureUtils.failureTolerantAllOf(futures)
                .thenApply(list -> list.size() == futures.size())
                .whenComplete((result, throwable) -> setTransportState(TransportState.TERMINATED));
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle, String nodeId) {
        try {
            int port = networkId.getAddressByTransportTypeMap().get(TransportType.I2P).getPort();
            initializeServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
            log.debug("Create serverSocket");
            I2PKeyPair i2PKeyPair = keyBundle.getI2PKeyPair();
            ServerSocket serverSocket = i2pClient.getServerSocket(i2PKeyPair, nodeId);
            String destinationBase64 = i2PKeyPair.getDestinationBase64();
            Address address = new Address(destinationBase64, port);
            initializedServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
            log.info("ServerSocket created. destinationBase32={}, destinationBase64={}", i2PKeyPair.getDestinationBase32(), destinationBase64);
            return new ServerSocketResult(serverSocket, address);
        } catch (Exception exception) {
            if (!isShutdownInProgress) {
                log.error("getServerSocket failed", exception);
            }
            throw new ConnectionException(exception);
        }
    }

    @Override
    public Socket getSocket(Address address, String nodeId) throws IOException {
        try {
            long ts = System.currentTimeMillis();
            // We use base64 in the address host field
            String peersDestinationBase64 = address.getHost();
            Destination peersDestination = new Destination(peersDestinationBase64);
            Socket socket = i2pClient.getSocket(peersDestination, nodeId);
            socket.setSoTimeout(i2pConfig.getSocketTimeout());
            log.info("I2P socket to {} created. Took {} ms", address, System.currentTimeMillis() - ts);
            return socket;
        } catch (NoRouteToHostException exception) {
            if (!isShutdownInProgress) {
                log.info("NoRouteToHostException {}", exception.getMessage());
            }
            throw exception;
        } catch (IOException exception) {
            if (!isShutdownInProgress) {
                log.error("getSocket failed", exception);
            }
            throw exception;
        } catch (Exception exception) {
            if (!isShutdownInProgress) {
                log.error("getSocket failed", exception);
            }
            throw new IOException(exception);
        }
    }

    @Override
    public CompletableFuture<Boolean> isPeerOnlineAsync(Address address, String nodeId) {
        String peersDestinationBase64 = address.getHost();
        try {
            Destination peersDestination = new Destination(peersDestinationBase64);
            return CompletableFuture.supplyAsync(() -> {
                boolean leaseFoundInNetDb = i2pClient.isLeaseFoundInNetDb(peersDestination, nodeId);
                if (!leaseFoundInNetDb) {
                    return false;
                }

                // With default I2P router we cannot request the state, and we get an Optional.Empty.
                // We return true in that case (it is used at sending a confidential message).
                return i2pRouterFacade.isPeerOnline(peersDestination, nodeId)
                        .orElse(true);
            });
        } catch (DataFormatException e) {
            return CompletableFuture.failedFuture(new IOException("Invalid I2P destination", e));
        }
    }
}
