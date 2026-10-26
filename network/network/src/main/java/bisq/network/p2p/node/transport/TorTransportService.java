package bisq.network.p2p.node.transport;

import bisq.common.network.Address;
import bisq.common.network.TorAddress;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.tor.TorService;
import bisq.network.tor.TorTransportConfig;
import bisq.network.tor.onionservice.OnionServiceKey;
import bisq.network.tor.process.TorDirectories;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.TorKeyPair;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public class TorTransportService implements TransportService {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements TransportConfig {
        public static Config from(Path dataDirPath, com.typesafe.config.Config config) {
            return new Config(dataDirPath,
                    config.hasPath("defaultNodePort") ? config.getInt("defaultNodePort") : -1,
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")),
                    config.getInt("sendMessageThrottleTime"),
                    config.getInt("receiveMessageThrottleTime"),
                    TorTransportConfig.from(dataDirPath, config));
        }

        private final Path dataDirPath;
        private final int defaultNodePort;
        private final int socketTimeout; // in ms
        private final int sendMessageThrottleTime;
        private final int receiveMessageThrottleTime;
        private final TorTransportConfig torTransportConfig;

        public Config(Path dataDirPath,
                      int defaultNodePort,
                      int socketTimeout,
                      int sendMessageThrottleTime,
                      int receiveMessageThrottleTime,
                      TorTransportConfig torTransportConfig) {
            this.dataDirPath = dataDirPath;
            this.defaultNodePort = defaultNodePort;
            this.socketTimeout = socketTimeout;
            this.sendMessageThrottleTime = sendMessageThrottleTime;
            this.receiveMessageThrottleTime = receiveMessageThrottleTime;
            this.torTransportConfig = torTransportConfig;
        }
    }

    // The directory of the embedded Tor below the data directory of the application
    public static final String TOR_DIR_NAME = "tor";

    private static TorService torService;

    private final int socketTimeout;
    @Getter
    public final Observable<TransportState> transportState = new Observable<>(TransportState.NEW);
    @Getter
    public final ObservableHashMap<TransportState, Long> timestampByTransportState = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializeServerSocketTimestampByNetworkId = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializedServerSocketTimestampByNetworkId = new ObservableHashMap<>();

    public TorTransportService(TransportConfig config) {
        socketTimeout = config.getSocketTimeout();
        if (torService == null) {
            setTransportState(TransportState.NEW);
            torService = new TorService(((Config) config).getTorTransportConfig(), new BisqTorPlatformServices());
        }
    }

    @Override
    public void initialize() {
        log.info("Initialize Tor");
        long ts = System.currentTimeMillis();
        setTransportState(TransportState.INITIALIZE);
        torService.initialize().join();
        setTransportState(TransportState.INITIALIZED);
        log.info("Initializing Tor took {} ms", System.currentTimeMillis() - ts);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        setTransportState(TransportState.STOPPING);
        return torService.shutdown()
                .whenComplete((result, throwable) -> setTransportState(TransportState.TERMINATED));
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle, String nodeId) {
        try {
            Optional<Address> optionalAddress = networkId.getAddressByTransportTypeMap().getAddress(TransportType.TOR);
            checkArgument(optionalAddress.isPresent(), "networkId.getAddressByTransportTypeMap().getAddress(TransportType.TOR) must not be empty");
            int port = optionalAddress.map(Address::getPort).orElseThrow();
            initializeServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());

            TorKeyPair torKeyPair = keyBundle.getTorKeyPair();
            String onionAddress = torKeyPair.getOnionAddress();
            TorAddress address = new TorAddress(onionAddress, port);
            ServerSocket serverSocket = torService.publishOnionServiceAndCreateServerSocket(port, toOnionServiceKey(torKeyPair)).get();
            initializedServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
            return new ServerSocketResult(serverSocket, address);
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at getServerSocket method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
            throw new ConnectionException(e);
        } catch (ExecutionException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address, String nodeId) throws IOException {
        if (address instanceof TorAddress torAddress) {
            long ts = System.currentTimeMillis();
            log.info("Start creating tor socket to {}", torAddress);
            // Blocking call. Takes 5-15 sec usually. The node ID isolates the circuits of the nodes of one user.
            Socket socket = torService.getSocket(torAddress.getHost(), torAddress.getPort(), nodeId, socketTimeout);
            log.info("Tor socket creation to {} took {} ms", torAddress, System.currentTimeMillis() - ts);
            return socket;
        } else {
            throw new IllegalArgumentException("Address is not a TorAddress");
        }
    }

    @Override
    public CompletableFuture<Boolean> isPeerOnlineAsync(Address address, String nodeId) {
        if (address instanceof TorAddress torAddress) {
            return torService.isOnionServiceOnlineAsync(torAddress.getHost());
        } else {
            throw new IllegalArgumentException("Address is not a TorAddress");
        }
    }

    @Override
    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.of(torService.getSocks5Proxy());
    }

    public boolean isUseExternalTor() {
        return torService.isUseExternalTor();
    }

    /**
     * @return the version which the running Tor reports. With an external Tor it is the Tor of the user, so it can
     * differ from the Tor of the bundle. Empty before the transport has initialized and when an external Tor does not
     * tell its version.
     */
    public Optional<String> getTorVersion() {
        return torService.getTorVersion();
    }

    public CompletableFuture<String> publishOnionService(int localPort, int onionServicePort, TorKeyPair torKeyPair) {
        return torService.publishOnionService(localPort, onionServicePort, toOnionServiceKey(torKeyPair));
    }

    /**
     * @return the address on which a server which {@link #publishOnionService} publishes has to listen, because Tor,
     * or the relay of the sandbox in front of Tor, connects there. It belongs to the running Tor, so it is known once
     * the transport has initialized.
     */
    public InetAddress getOnionServiceBindAddress() {
        return torService.getOnionServiceBindAddress();
    }

    /**
     * @return the log of the embedded Tor of the application with this data directory: Tor logs to its standard
     * output, which the Tor library writes to this file. The Tor library names it.
     */
    public static Path getTorLogPath(Path appDataDirPath) {
        return new TorDirectories(appDataDirPath.resolve(TOR_DIR_NAME)).getStdoutLogPath();
    }

    /**
     * @return the earlier log of the embedded Tor of the application with this data directory, which the Tor library
     * moves aside when the log exceeds its size limit. The Tor library names it.
     */
    public static Path getRotatedTorLogPath(Path appDataDirPath) {
        return new TorDirectories(appDataDirPath.resolve(TOR_DIR_NAME)).getRotatedStdoutLogPath();
    }

    private static OnionServiceKey toOnionServiceKey(TorKeyPair torKeyPair) {
        return new OnionServiceKey(torKeyPair.getOnionAddress(), torKeyPair.getBase64SecretScalar());
    }
}
