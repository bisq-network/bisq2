package bisq.network.p2p.node.transport;

import bisq.common.network.Address;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
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

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@Slf4j
public class TorTransportService implements TransportService {
    private static TorService torService;

    @Getter
    public final Observable<TransportState> transportState = new Observable<>(TransportState.NEW);
    @Getter
    public final ObservableHashMap<TransportState, Long> timestampByTransportState = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializeServerSocketTimestampByNetworkId = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializedServerSocketTimestampByNetworkId = new ObservableHashMap<>();
    @Nullable
    private Pin transportStatePin;

    public TorTransportService(TransportConfig config) {
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
        if (transportStatePin != null) {
            transportStatePin.unbind();
            transportStatePin = null;
        }
        return torService.shutdown()
                .whenComplete((result, throwable) -> setTransportState(TransportState.TERMINATED));
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle) {
        try {
            int port = networkId.getAddressByTransportTypeMap().get(TransportType.TOR).getPort();
            initializeServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());

            TorKeyPair torKeyPair = keyBundle.getTorKeyPair();
            String onionAddress = torKeyPair.getOnionAddress();
            Address address = new Address(onionAddress, port);
            ServerSocket serverSocket = torService.publishOnionServiceAndCreateServerSocket(port, torKeyPair).get();
            initializedServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
            return new ServerSocketResult(serverSocket, address);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new ConnectionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        long ts = System.currentTimeMillis();
        log.info("Start creating tor socket to {}", address);
        Socket socket = torService.getSocket(null); // Blocking call. Takes 5-15 sec usually.
        InetSocketAddress inetSocketAddress = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());
        try {
            socket.connect(inetSocketAddress);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
        log.info("Tor socket creation to {} took {} ms", address, System.currentTimeMillis() - ts);
        return socket;
    }

    @Override
    public boolean isPeerOnline(Address address) {
        return torService.isOnionServiceOnline(address.getHost());
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.of(torService.getSocks5Proxy(null));
    }

    public Observable<Boolean> getUseExternalTor() {
        return torService.getUseExternalTor();
    }

    // We wait until tor is initialized and publish the onion address
    public CompletableFuture<String> publishOnionService(int localPort, int onionServicePort, TorKeyPair torKeyPair) {
        CompletableFuture<String> future = new CompletableFuture<>();
        transportStatePin = getTransportState().addObserver(state -> {
            if (TransportState.INITIALIZED == state) {
                torService.publishOnionService(localPort, onionServicePort, torKeyPair)
                        .whenComplete((onionAddress, throwable) -> {
                            if (throwable == null) {
                                future.complete(onionAddress);
                            } else {
                                future.completeExceptionally(throwable);
                            }
                        });
                if (transportStatePin != null) {
                    transportStatePin.unbind();
                    transportStatePin = null;
                }
            }
        });
        return future;
    }
}
