package bisq.network.p2p.node.transport;

import bisq.common.network.Address;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyBundle;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static bisq.common.facades.FacadeProvider.getLocalhostFacade;


@Slf4j
public class ClearNetTransportService implements TransportService {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements TransportConfig {
        public static Config from(Path dataDir, com.typesafe.config.Config config) {

            return new Config(dataDir,
                    config.hasPath("defaultNodePort") ? config.getInt("defaultNodePort") : -1,
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("defaultNodeSocketTimeout")),
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("userNodeSocketTimeout")),
                    config.getInt("devModeDelayInMs"),
                    config.getInt("sendMessageThrottleTime"),
                    config.getInt("receiveMessageThrottleTime"),
                    config.getInt("connectTimeoutMs")
            );
        }

        private final Path dataDir;
        private final int defaultNodePort;
        private final int defaultNodeSocketTimeout;
        private final int userNodeSocketTimeout;
        private final int devModeDelayInMs;
        private final int sendMessageThrottleTime;
        private final int receiveMessageThrottleTime;
        private final int connectTimeoutMs;

        public Config(Path dataDir,
                      int defaultNodePort,
                      int defaultNodeSocketTimeout,
                      int userNodeSocketTimeout,
                      int devModeDelayInMs,
                      int sendMessageThrottleTime,
                      int receiveMessageThrottleTime,
                      int connectTimeoutMs) {
            this.dataDir = dataDir;
            this.defaultNodePort = defaultNodePort;
            this.defaultNodeSocketTimeout = defaultNodeSocketTimeout;
            this.userNodeSocketTimeout = userNodeSocketTimeout;
            this.devModeDelayInMs = devModeDelayInMs;
            this.sendMessageThrottleTime = sendMessageThrottleTime;
            this.receiveMessageThrottleTime = receiveMessageThrottleTime;
            this.connectTimeoutMs = connectTimeoutMs;
        }
    }

    private final int devModeDelayInMs;
    private final int connectTimeoutMs;
    private boolean initializeCalled;
    @Getter
    public final Observable<TransportState> transportState = new Observable<>(TransportState.NEW);
    @Getter
    public final ObservableHashMap<TransportState, Long> timestampByTransportState = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializeServerSocketTimestampByNetworkId = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializedServerSocketTimestampByNetworkId = new ObservableHashMap<>();

    public ClearNetTransportService(TransportConfig config) {
        devModeDelayInMs = config.getDevModeDelayInMs();
        connectTimeoutMs = ((Config) config).getConnectTimeoutMs();
        setTransportState(TransportState.NEW);
    }

    @Override
    public void initialize() {
        if (initializeCalled) {
            return;
        }
        setTransportState(TransportState.INITIALIZE);
        initializeCalled = true;
        maybeSimulateDelay();
        setTransportState(TransportState.INITIALIZED);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        setTransportState(TransportState.STOPPING);
        return CompletableFuture.supplyAsync(() -> true,
                        CompletableFuture.delayedExecutor(devModeDelayInMs, TimeUnit.MILLISECONDS))
                .whenComplete((result, throwable) -> setTransportState(TransportState.TERMINATED));
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle) {
        int port = networkId.getAddressByTransportTypeMap().get(TransportType.CLEAR).getPort();
        initializeServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
        log.info("Create serverSocket at port {}", port);

        maybeSimulateDelay();
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Address address = getLocalhostFacade().toMyLocalhost(port);
            log.debug("ServerSocket created at port {}", port);
            initializedServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
            return new ServerSocketResult(serverSocket, address);
        } catch (IOException e) {
            log.error("{}. Server port {}", e, port);
            throw new CompletionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        address = getLocalhostFacade().toPeersLocalhost(address);

        log.debug("Create new Socket to {}", address);
        maybeSimulateDelay();
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(address.getHost(), address.getPort()), connectTimeoutMs);

        return socket;
    }

    @Override
    public boolean isPeerOnline(Address address) {
        try (Socket ignored = getSocket(address)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void maybeSimulateDelay() {
        if (devModeDelayInMs > 0) {
            try {
                Thread.sleep(devModeDelayInMs);
            } catch (Throwable t) {
                log.error("Exception", t);
            }
        }
    }
}
