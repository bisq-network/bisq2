package bisq.network.p2p.node.transport;

import bisq.network.p2p.node.Address;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;


@Slf4j
public class ClearNetTransport implements Transport {
    private static ClearNetTransport INSTANCE;
    private boolean initializeCalled;

    public static ClearNetTransport getInstance(Config config) {
        if (INSTANCE == null) {
            INSTANCE = new ClearNetTransport(config);
        }
        return INSTANCE;
    }

    public ClearNetTransport(Config config) {
    }

    @Override
    public Boolean initialize() {
        if (initializeCalled) {
            return true;
        }
        initializeCalled = true;
        log.debug("Initialize");
        return true;
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
}
