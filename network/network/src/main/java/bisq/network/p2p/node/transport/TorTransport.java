package bisq.network.p2p.node.transport;

import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.ConnectionException;
import bisq.tor.OnionAddress;
import bisq.tor.Tor;
import bisq.tor.TorServerSocket;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;


@Slf4j
public class TorTransport implements Transport {
    public final static int DEFAULT_PORT = 9999;

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

    private final String torDirPath;
    private final Tor tor;

    public TorTransport(Transport.Config config) {
        torDirPath = config.getBaseDir() + separator + "tor";
        // We get a singleton instance per application (torDirPath)
        tor = Tor.getTor(torDirPath);
    }

    @Override
    public boolean initialize() {
        log.info("Initialize Tor");
        try {
            checkArgument(tor.start(), "Tor start failed");
            return true;
        } catch (Exception e) {
            log.error("tor.start failed", e);
            throw new ConnectionException(e);
        }
    }

    @Override
    public ServerSocketResult getServerSocket(int port, String nodeId) {
        log.info("Start hidden service with port {} and nodeId {}", port, nodeId);
        long ts = System.currentTimeMillis();
        try {
            TorServerSocket torServerSocket = tor.getTorServerSocket();
            OnionAddress onionAddress = torServerSocket.bind(port, nodeId);
            log.info("Tor hidden service Ready. Took {} ms. Onion address={}; nodeId={}",
                    System.currentTimeMillis() - ts, onionAddress, nodeId);
            return new ServerSocketResult(nodeId, torServerSocket, new Address(onionAddress.getHost(), onionAddress.getPort()));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new ConnectionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        long ts = System.currentTimeMillis();
        Socket socket = tor.getSocket(null); // Blocking call. Takes 5-15 sec usually.
        socket.connect(new InetSocketAddress(address.getHost(), address.getPort()));
        log.info("Tor socket to {} created. Took {} ms", address, System.currentTimeMillis() - ts);
        return socket;
    }

    @Override
    public boolean isAddressAvailable(Address address) {
        return tor.isHiddenServiceAvailable(address.getHost());
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.of(tor.getSocks5Proxy(null));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        log.info("Shutdown tor.");
        if (tor == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(tor::shutdown, NetworkService.NETWORK_IO_POOL);
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        return tor.getHostName(serverId).map(hostName -> new Address(hostName, TorTransport.DEFAULT_PORT));
    }
}
