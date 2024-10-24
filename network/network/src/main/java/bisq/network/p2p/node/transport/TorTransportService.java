package bisq.network.p2p.node.transport;

import bisq.common.timer.Scheduler;
import bisq.common.network.Address;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.ConnectionException;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.TorKeyPair;
import bisq.tor.TorService;
import bisq.tor.TorTransportConfig;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
public class TorTransportService implements TransportService {
    private static TorService torService;

    @Getter
    private final BootstrapInfo bootstrapInfo = new BootstrapInfo();
    private Scheduler startBootstrapProgressUpdater;
    private int numSocketsCreated = 0;

    public TorTransportService(TransportConfig config) {
        if (torService == null) {
            torService = new TorService((TorTransportConfig) config);
            bootstrapInfo.getBootstrapState().set(BootstrapState.BOOTSTRAP_TO_NETWORK);
            startBootstrapProgressUpdater = Scheduler.run(() -> updateStartBootstrapProgress(bootstrapInfo))
                    .host(this)
                    .runnableName("updateStartBootstrapProgress")
                    .periodically(1000);
            bootstrapInfo.getBootstrapDetails().set("Start bootstrapping");
            torService.getBootstrapEvent().addObserver(bootstrapEvent -> {
                if (bootstrapEvent != null) {
                    int bootstrapEventProgress = bootstrapEvent.getProgress();
                    // First 25% we attribute to the bootstrap to the Tor network. Takes usually about 3 sec.
                    if (bootstrapInfo.getBootstrapProgress().get() < 0.25) {
                        if (startBootstrapProgressUpdater != null) {
                            startBootstrapProgressUpdater.stop();
                            startBootstrapProgressUpdater = null;
                        }
                        bootstrapInfo.getBootstrapProgress().set(bootstrapEventProgress / 400d);
                        bootstrapInfo.getBootstrapDetails().set("Tor bootstrap event: " + bootstrapEvent.getTag());
                    }
                }
            });
        }
    }

    @Override
    public void initialize() {
        log.info("Initialize Tor");
        torService.initialize().join();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (startBootstrapProgressUpdater != null) {
            startBootstrapProgressUpdater.stop();
            startBootstrapProgressUpdater = null;
        }
        return torService.shutdown();
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle) {
        try {
            int port = networkId.getAddressByTransportTypeMap().get(TransportType.TOR).getPort();
            bootstrapInfo.getBootstrapState().set(BootstrapState.START_PUBLISH_SERVICE);
            // 25%-50% we attribute to the publishing of the hidden service. Takes usually 5-10 sec.
            bootstrapInfo.getBootstrapProgress().set(0.25);
            bootstrapInfo.getBootstrapDetails().set("Create Onion service for node ID '" + networkId + "'");

            TorKeyPair torKeyPair = keyBundle.getTorKeyPair();
            ServerSocket serverSocket = torService.createOnionService(port, torKeyPair)
                    .get(2, TimeUnit.MINUTES);

            bootstrapInfo.getBootstrapState().set(BootstrapState.SERVICE_PUBLISHED);
            bootstrapInfo.getBootstrapProgress().set(0.5);

            String onionAddress = torKeyPair.getOnionAddress();
            bootstrapInfo.getBootstrapDetails().set("My Onion service address: " + onionAddress);

            Address address = new Address(onionAddress, port);
            return new ServerSocketResult(serverSocket, address);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
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
        numSocketsCreated++;
        bootstrapInfo.getBootstrapState().set(BootstrapState.CONNECTED_TO_PEERS);
        bootstrapInfo.getBootstrapProgress().set(Math.min(1, 0.5 + numSocketsCreated / 10d));
        bootstrapInfo.getBootstrapDetails().set("Connected to " + numSocketsCreated + " peer(s)");
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
}
