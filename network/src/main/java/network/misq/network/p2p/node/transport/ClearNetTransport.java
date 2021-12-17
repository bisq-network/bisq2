package network.misq.network.p2p.node.transport;

import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.node.Address;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


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
        if (initializeCalled) {
            return CompletableFuture.completedFuture(true);
        }
        initializeCalled = true;

        log.debug("Initialize");
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        new Thread(() -> {
           /* try {
                Thread.sleep(1); // simulate tor delay
            } catch (InterruptedException ignore) {
            }*/
            future.complete(true);
        }).start();

        return future;
    }

    @Override
    public CompletableFuture<ServerSocketResult> getServerSocket(int port, String nodeId) {
        CompletableFuture<ServerSocketResult> future = new CompletableFuture<>();
        log.debug("Create serverSocket at port {}", port);
      /*  try {
            Thread.sleep(1); // simulate tor delay
        } catch (InterruptedException ignore) {
        }*/

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Address address = Address.localHost(port);
            log.debug("ServerSocket created at port {}", port);
            future.complete(new ServerSocketResult(nodeId, serverSocket, address));
        } catch (IOException e) {
            log.error("{}. Server port {}", e, port);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        log.debug("Create new Socket to {}", address);
        return new Socket(address.getHost(), address.getPort());
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        return Optional.empty();
    }
}
