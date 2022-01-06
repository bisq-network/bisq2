package bisq.network.p2p.node.transport;

import bisq.common.util.FileUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.ConnectionException;
import bisq.tor.Constants;
import bisq.tor.OnionAddress;
import bisq.tor.Tor;
import bisq.tor.TorServerSocket;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.io.File.separator;


@Slf4j
public class TorTransport implements Transport {
    public final static int DEFAULT_PORT = 9999;
    private static TorTransport INSTANCE;

    private final String torDirPath;
    private final Tor tor;

    public static TorTransport getInstance(Config config) {
        if (INSTANCE == null) {
            INSTANCE = new TorTransport(config);
        }
        return INSTANCE;
    }

    public TorTransport(Config config) {
        torDirPath = config.baseDir() + separator + "tor";
        log.info("TorTransport using torDirPath: {}", torDirPath);
        // We get a singleton instance per application (torDirPath)
        tor = Tor.getTor(torDirPath);
    }

    @Override
    public Boolean initialize() {
        log.info("Initialize Tor");
        long ts = System.currentTimeMillis();
        try {
            tor.start();
            log.info("Tor initialized after {} ms", System.currentTimeMillis() - ts);
            return true;
        } catch (Exception e) {
            log.error("tor.startAsync failed", e);
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
            log.info("Tor hidden service Ready. Took {} ms. Onion address={}", System.currentTimeMillis() - ts, onionAddress);
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

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.of(tor.getSocks5Proxy(null));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        if (tor == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(tor::shutdown, NetworkService.NETWORK_IO_POOL);
    }

    //todo move to torify lib
    @Override
    public Optional<Address> getServerAddress(String serverId) {
        String fileName = torDirPath + separator + Constants.HS_DIR + separator + serverId + separator + "hostname";
        if (new File(fileName).exists()) {
            try {
                String host = FileUtils.readAsString(fileName);
                return Optional.of(new Address(host, TorTransport.DEFAULT_PORT));
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        }

        return Optional.empty();
    }
}
