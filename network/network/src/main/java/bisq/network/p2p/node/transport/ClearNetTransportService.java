package bisq.network.p2p.node.transport;

import bisq.network.common.TransportConfig;
import bisq.network.p2p.node.Address;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;


@Slf4j
public class ClearNetTransportService implements TransportService {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements TransportConfig {
        public static Config from(Path dataDir, com.typesafe.config.Config config) {

            return new Config(dataDir,
                    config.hasPath("defaultNodePort") ? config.getInt("defaultNodePort") : -1,
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")));
        }

        private final int defaultNodePort;
        private final int socketTimeout;
        private final Path dataDir;

        public Config(Path dataDir, int defaultNodePort, int socketTimeout) {
            this.dataDir = dataDir;
            this.defaultNodePort = defaultNodePort;
            this.socketTimeout = socketTimeout;
        }
    }

    private final TransportConfig config;
    private int numSocketsCreated = 0;
    @Getter
    private final BootstrapInfo bootstrapInfo = new BootstrapInfo();
    private boolean initializeCalled;

    public ClearNetTransportService(TransportConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (initializeCalled) {
            return CompletableFuture.completedFuture(true);
        }

        bootstrapInfo.getBootstrapState().set(BootstrapState.BOOTSTRAP_TO_NETWORK);

        initializeCalled = true;
        return CompletableFuture.completedFuture(true);

        // Simulate delay
        /*return CompletableFuture.supplyAsync(() -> {
            initializeCalled = true;
            return true;
        }, CompletableFuture.delayedExecutor(20, TimeUnit.MILLISECONDS));*/
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        // Simulate delay
        return CompletableFuture.supplyAsync(() -> true,
                CompletableFuture.delayedExecutor(20, TimeUnit.MILLISECONDS));
    }

    @Override
    public ServerSocketResult getServerSocket(int port, String nodeId) {
        log.info("Create serverSocket at port {}", port);

        bootstrapInfo.getBootstrapState().set(BootstrapState.START_PUBLISH_SERVICE);
        bootstrapInfo.getBootstrapProgress().set(0.25);
        bootstrapInfo.getBootstrapDetails().set("Start creating server");

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Address address = Address.localHost(port);
            log.debug("ServerSocket created at port {}", port);

            bootstrapInfo.getBootstrapState().set(BootstrapState.SERVICE_PUBLISHED);
            bootstrapInfo.getBootstrapProgress().set(0.5);
            bootstrapInfo.getBootstrapDetails().set("Server created: " + address);

            return new ServerSocketResult(nodeId, serverSocket, address);
        } catch (IOException e) {
            log.error("{}. Server port {}", e, port);
            throw new CompletionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        log.debug("Create new Socket to {}", address);
        Socket socket = new Socket(address.getHost(), address.getPort());
        numSocketsCreated++;

        bootstrapInfo.getBootstrapState().set(BootstrapState.CONNECTED_TO_PEERS);
        bootstrapInfo.getBootstrapProgress().set(Math.min(1, 0.5 + numSocketsCreated / 10d));
        bootstrapInfo.getBootstrapDetails().set("Connected to " + numSocketsCreated + " peers");

        return socket;
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        return Optional.empty();
    }

    @Override
    public boolean isPeerOnline(Address address) {
        try (Socket ignored = getSocket(address)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
