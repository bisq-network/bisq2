package network.misq.network.p2p.node.transport;

import lombok.extern.slf4j.Slf4j;
import network.misq.network.NetworkService;
import network.misq.network.p2p.node.Address;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static network.misq.common.threading.ExecutorFactory.newSingleThreadExecutor;


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

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
                    if (initializeCalled) {
                        return true;
                    }
                    initializeCalled = true;
                    log.debug("Initialize");
                    return true;
                }, NetworkService.NETWORK_IO_POOL)
                .thenApplyAsync(result -> result, newSingleThreadExecutor("ClearNetTransport.initialize"));
    }

    @Override
    public CompletableFuture<ServerSocketResult> getServerSocket(int port, String nodeId) {
        log.debug("Create serverSocket at port {}", port);
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        ServerSocket serverSocket = new ServerSocket(port);
                        Address address = Address.localHost(port);
                        log.debug("ServerSocket created at port {}", port);
                        return new ServerSocketResult(nodeId, serverSocket, address);
                    } catch (IOException e) {
                        log.error("{}. Server port {}", e, port);
                        throw new CompletionException(e);
                    }
                }, NetworkService.NETWORK_IO_POOL)
                .thenApplyAsync(result -> result, newSingleThreadExecutor("ClearNetTransport.getServerSocket"));
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        log.debug("Create new Socket to {}", address);
        return new Socket(address.getHost(), address.getPort());
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> initializeCalled = false, 
                newSingleThreadExecutor("ClearNetTransport.shutdown"));
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        return Optional.empty();
    }
}
