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
public class ClearNetTransport implements Transport {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements TransportConfig {
        public static Config from(Path dataDir, com.typesafe.config.Config config) {
            return new Config(dataDir, (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")));
        }

        private final int socketTimeout;
        private final Path dataDir;

        public Config(Path dataDir, int socketTimeout) {
            this.dataDir = dataDir;
            this.socketTimeout = socketTimeout;
        }
    }

    private final TransportConfig config;
    private boolean initializeCalled;

    public ClearNetTransport(TransportConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (initializeCalled) {
            return CompletableFuture.completedFuture(true);
        }
        //Uninterruptibles.sleepUninterruptibly(Duration.of(2, ChronoUnit.SECONDS));
        initializeCalled = true;
        log.debug("Initialize");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public ServerSocketResult getServerSocket(int port, String nodeId) {
        log.info("Create serverSocket at port {}", port);
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Address address = Address.localHost(port);
            log.debug("ServerSocket created at port {}", port);
            return new ServerSocketResult(nodeId, serverSocket, address);
        } catch (IOException e) {
            log.error("{}. Server port {}", e, port);
            throw new CompletionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        log.debug("Create new Socket to {}", address);
        return new Socket(address.getHost(), address.getPort());
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
        }, CompletableFuture.delayedExecutor(20, TimeUnit.MILLISECONDS));
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
