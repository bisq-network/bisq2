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
import bisq.security.keys.KeyBundle;
import bisq.security.keys.TorKeyPair;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public class TorTransportService implements TransportService {
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
            torService = new TorService((TorTransportConfig) config);
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
            ServerSocket serverSocket = torService.publishOnionServiceAndCreateServerSocket(port, torKeyPair).get();
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
            Socket socket = torService.getSocket(null); // Blocking call. Takes 5-15 sec usually.
            socket.setSoTimeout(socketTimeout);
            InetSocketAddress inetSocketAddress = InetSocketAddress.createUnresolved(torAddress.getHost(), torAddress.getPort());
            try {
                socket.connect(inetSocketAddress);
            } catch (IOException e) {
                socket.close();
                throw e;
            }
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
        return Optional.of(torService.getSocks5Proxy(null));
    }

    public Observable<Boolean> getUseExternalTor() {
        return torService.getUseExternalTor();
    }

    public CompletableFuture<String> publishOnionService(int localPort, int onionServicePort, TorKeyPair torKeyPair) {
        return torService.publishOnionService(localPort, onionServicePort, torKeyPair);
    }
}
