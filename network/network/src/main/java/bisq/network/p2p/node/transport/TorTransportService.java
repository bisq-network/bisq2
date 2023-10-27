package bisq.network.p2p.node.transport;

import bisq.common.timer.Scheduler;
import bisq.network.common.Address;
import bisq.network.common.TransportConfig;
import bisq.network.p2p.node.ConnectionException;
import bisq.tor.TorService;
import bisq.tor.TorTransportConfig;
import bisq.tor.onionservice.CreateOnionServiceResponse;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
public class TorTransportService implements TransportService {
    public final static int DEFAULT_PORT = 9999;

    private static TorService torService;

    @Getter
    private final BootstrapInfo bootstrapInfo = new BootstrapInfo();
    private Scheduler startBootstrapProgressUpdater;
    private int numSocketsCreated = 0;

    public TorTransportService(TransportConfig config) {
        if (torService == null) {
            torService = new TorService((TorTransportConfig) config);
            bootstrapInfo.getBootstrapState().set(BootstrapState.BOOTSTRAP_TO_NETWORK);
            startBootstrapProgressUpdater = Scheduler.run(() -> updateStartBootstrapProgress(bootstrapInfo)).periodically(1000);
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
        log.info("Shutdown tor.");
        if (startBootstrapProgressUpdater != null) {
            startBootstrapProgressUpdater.stop();
            startBootstrapProgressUpdater = null;
        }
        torService.shutdown().join();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public ServerSocketResult getServerSocket(int port, String nodeId) {
        try {
            bootstrapInfo.getBootstrapState().set(BootstrapState.START_PUBLISH_SERVICE);
            // 25%-50% we attribute to the publishing of the hidden service. Takes usually 5-10 sec.
            bootstrapInfo.getBootstrapProgress().set(0.25);
            bootstrapInfo.getBootstrapDetails().set("Create Onion service for node ID '" + nodeId + "'");

            CreateOnionServiceResponse response = torService.createOnionService(port, nodeId).get(2, TimeUnit.MINUTES);

            bootstrapInfo.getBootstrapState().set(BootstrapState.SERVICE_PUBLISHED);
            bootstrapInfo.getBootstrapProgress().set(0.5);
            bootstrapInfo.getBootstrapDetails().set("My Onion service address: " + response.getOnionAddress().toString());

            return new ServerSocketResult(response);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            throw new ConnectionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        long ts = System.currentTimeMillis();

        Socket socket = torService.getSocket(null); // Blocking call. Takes 5-15 sec usually.
        socket.connect(new InetSocketAddress(address.getHost(), address.getPort()));
        numSocketsCreated++;
        bootstrapInfo.getBootstrapState().set(BootstrapState.CONNECTED_TO_PEERS);
        bootstrapInfo.getBootstrapProgress().set(Math.min(1, 0.5 + numSocketsCreated / 10d));
        bootstrapInfo.getBootstrapDetails().set("Connected to " + numSocketsCreated + " peer(s)");

        log.info("Tor socket to {} created. Took {} ms", address, System.currentTimeMillis() - ts);
        return socket;
    }

    @Override
    public boolean isPeerOnline(Address address) {
        return torService.isOnionServiceOnline(address.getHost());
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.of(torService.getSocks5Proxy(null));
    }

    @Override
    public Optional<Address> getServerAddress(String nodeId) {
        return torService.getOnionAddressForNode(nodeId).map(onionAddress -> new Address(onionAddress.getHost(), TorTransportService.DEFAULT_PORT));
    }
}
