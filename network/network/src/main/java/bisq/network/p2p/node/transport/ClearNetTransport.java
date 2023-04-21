package bisq.network.p2p.node.transport;

import bisq.network.p2p.node.Address;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;


@Slf4j
public class ClearNetTransport implements Transport {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements Transport.Config {
        public static Config from(String baseDir, com.typesafe.config.Config config) {
            return new Config(baseDir, (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")));
        }

        private final int socketTimeout;
        private final String baseDir;

        public Config(String baseDir, int socketTimeout) {
            this.baseDir = baseDir;
            this.socketTimeout = socketTimeout;
        }
    }

    private final Transport.Config config;
    private boolean initializeCalled;

    public ClearNetTransport(Transport.Config config) {
        this.config = config;
    }

    @Override
    public boolean initialize() {
        if (initializeCalled) {
            return true;
        }
        //Uninterruptibles.sleepUninterruptibly(Duration.of(2, ChronoUnit.SECONDS));
        initializeCalled = true;
        log.debug("Initialize");
        return true;
    }

    @Override
    public CompletableFuture<ServerSocketChannelResult> getServerSocketChannel(int port, String nodeId) {
        log.info("Create serverSocket at port {}", port);
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress socketAddress = new InetSocketAddress(
                    InetAddress.getLocalHost(),
                    port
            );
            serverSocketChannel.socket().bind(socketAddress);

            Address address = Address.localHost(port);
            log.debug("ServerSocket created at port {}", port);
            return CompletableFuture.completedFuture(
                    new ServerSocketChannelResult(nodeId, serverSocketChannel, address)
            );
        } catch (IOException e) {
            log.error("{}. Server port {}", e, port);
            throw new CompletionException(e);
        }
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
    public boolean isAddressAvailable(Address address) {
        return true;
    }
}
