package network.misq.network.p2p.node.transport;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;
import network.misq.common.util.NetworkUtils;
import network.misq.i2p.SamClient;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.io.File.separator;

// Start I2P
// Enable SAM at http://127.0.0.1:7657/configclients
// Takes about 1-2 minutes until its ready
@Slf4j
public class I2PTransport implements Transport {
    private static I2PTransport INSTANCE;

    private final String i2pDirPath;
    private SamClient samClient;
    private final ExecutorService serverSocketExecutor = ExecutorFactory.getSingleThreadExecutor("I2pNetworkProxy.ServerSocket");
    private boolean initializeCalled;

    public static I2PTransport getInstance(Config config) {
        if (INSTANCE == null) {
            INSTANCE = new I2PTransport(config);
        }
        return INSTANCE;
    }

    private I2PTransport(Config config) {
        i2pDirPath = config.baseDirPath() + separator + "i2p";
    }

    public CompletableFuture<Boolean> initialize() {
        if (initializeCalled) {
            return CompletableFuture.completedFuture(true);
        }
        initializeCalled = true;

        log.debug("Initialize");
        try {
            samClient = SamClient.getSamClient(i2pDirPath);
            return CompletableFuture.completedFuture(true);
        } catch (Exception exception) {
            log.error(exception.toString(), exception);
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public CompletableFuture<ServerSocketResult> getServerSocket(int port, String nodeId) {
        CompletableFuture<ServerSocketResult> future = new CompletableFuture<>();
        log.debug("Create serverSocket");
        serverSocketExecutor.execute(() -> {
            try {
                ServerSocket serverSocket = samClient.getServerSocket(nodeId, NetworkUtils.findFreeSystemPort());
                String destination = samClient.getMyDestination(nodeId);
                Address address = new Address(destination, -1);
                log.debug("Create new Socket to {}", address);
                log.debug("ServerSocket created for address {}", address);
                future.complete(new ServerSocketResult(nodeId, serverSocket, address));
            } catch (Exception exception) {
                log.error(exception.toString(), exception);
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        try {
            log.debug("Create new Socket to {}", address);
            //todo pass session nodeId
            Socket socket = samClient.connect(address.getHost(), Node.DEFAULT_NODE_ID + "Alice");
            log.debug("Created new Socket");
            return socket;
        } catch (IOException exception) {
            log.error(exception.toString(), exception);
            throw exception;
        }
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            if (samClient != null) {
                samClient.shutdown();
            }
            initializeCalled = false;
        });
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        try {
            String myDestination = samClient.getMyDestination(serverId);
            return Optional.of(new Address(myDestination, -1));
        } catch (IOException exception) {
            log.error(exception.toString(), exception);
            return Optional.empty();
        }
    }
}
