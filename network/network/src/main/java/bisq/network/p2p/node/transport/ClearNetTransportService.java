package bisq.network.p2p.node.transport;

import bisq.common.timer.Scheduler;
import bisq.network.common.Address;
import bisq.network.common.TransportConfig;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyBundle;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
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
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("defaultNodeSocketTimeout")),
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("userNodeSocketTimeout")),
                    config.getInt("devModeDelayInMs"),
                    config.getInt("sendMessageThrottleTime"),
                    config.getInt("receiveMessageThrottleTime")
            );
        }

        private final Path dataDir;
        private final int defaultNodePort;
        private final int defaultNodeSocketTimeout;
        private final int userNodeSocketTimeout;
        private final int devModeDelayInMs;
        private final int sendMessageThrottleTime;
        private final int receiveMessageThrottleTime;

        public Config(Path dataDir,
                      int defaultNodePort,
                      int defaultNodeSocketTimeout,
                      int userNodeSocketTimeout,
                      int devModeDelayInMs,
                      int sendMessageThrottleTime,
                      int receiveMessageThrottleTime) {
            this.dataDir = dataDir;
            this.defaultNodePort = defaultNodePort;
            this.defaultNodeSocketTimeout = defaultNodeSocketTimeout;
            this.userNodeSocketTimeout = userNodeSocketTimeout;
            this.devModeDelayInMs = devModeDelayInMs;
            this.sendMessageThrottleTime = sendMessageThrottleTime;
            this.receiveMessageThrottleTime = receiveMessageThrottleTime;
        }
    }

    private final int devModeDelayInMs;
    private int numSocketsCreated = 0;
    @Getter
    private final BootstrapInfo bootstrapInfo = new BootstrapInfo();
    private boolean initializeCalled;
    private Scheduler startBootstrapProgressUpdater;

    public ClearNetTransportService(TransportConfig config) {
        devModeDelayInMs = config.getDevModeDelayInMs();
    }

    @Override
    public void initialize() {
        if (initializeCalled) {
            return;
        }
        initializeCalled = true;
        maybeSimulateDelay();
        bootstrapInfo.getBootstrapState().set(BootstrapState.BOOTSTRAP_TO_NETWORK);
        startBootstrapProgressUpdater = Scheduler.run(() -> updateStartBootstrapProgress(bootstrapInfo))
                .host(this)
                .runnableName("updateStartBootstrapProgress")
                .periodically(1000);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (startBootstrapProgressUpdater != null) {
            startBootstrapProgressUpdater.stop();
            startBootstrapProgressUpdater = null;
        }
        return CompletableFuture.supplyAsync(() -> true,
                CompletableFuture.delayedExecutor(devModeDelayInMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle) {
        int port = networkId.getAddressByTransportTypeMap().get(TransportType.CLEAR).getPort();
        log.info("Create serverSocket at port {}", port);

        if (startBootstrapProgressUpdater != null) {
            startBootstrapProgressUpdater.stop();
            startBootstrapProgressUpdater = null;
        }
        bootstrapInfo.getBootstrapState().set(BootstrapState.START_PUBLISH_SERVICE);
        bootstrapInfo.getBootstrapProgress().set(0.25);
        bootstrapInfo.getBootstrapDetails().set("Start creating server");

        maybeSimulateDelay();
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Address address = Address.localHost(port);
            log.debug("ServerSocket created at port {}", port);

            bootstrapInfo.getBootstrapState().set(BootstrapState.SERVICE_PUBLISHED);
            bootstrapInfo.getBootstrapProgress().set(0.5);
            bootstrapInfo.getBootstrapDetails().set("Server created: " + address);

            return new ServerSocketResult(serverSocket, address);
        } catch (IOException e) {
            log.error("{}. Server port {}", e, port);
            throw new CompletionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        log.debug("Create new Socket to {}", address);
        maybeSimulateDelay();
        Socket socket = new Socket(address.getHost(), address.getPort());
        numSocketsCreated++;

        bootstrapInfo.getBootstrapState().set(BootstrapState.CONNECTED_TO_PEERS);
        bootstrapInfo.getBootstrapProgress().set(Math.min(1, 0.5 + numSocketsCreated / 10d));
        bootstrapInfo.getBootstrapDetails().set("Connected to " + numSocketsCreated + " peers");

        return socket;
    }

    @Override
    public boolean isPeerOnline(Address address) {
        try (Socket ignored = getSocket(address)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void maybeSimulateDelay() {
        if (devModeDelayInMs > 0) {
            try {
                Thread.sleep(devModeDelayInMs);
            } catch (Throwable t) {
                log.error("Exception", t);
            }
        }
    }
}
