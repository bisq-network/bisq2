package bisq.network.p2p.node.transport;

import bisq.common.util.NetworkUtils;
import bisq.i2p.I2pClient;
import bisq.i2p.I2pEmbeddedRouter;
import bisq.network.NetworkService;
import bisq.network.common.TransportConfig;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.ConnectionException;
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
import java.util.concurrent.TimeUnit;

@Slf4j
public class I2PTransportService implements TransportService {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements TransportConfig {
        public static Config from(Path dataDir, com.typesafe.config.Config config) {
            return new Config(dataDir,
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")),
                    config.getInt("inboundKBytesPerSecond"),
                    config.getInt("outboundKBytesPerSecond"),
                    config.getInt("bandwidthSharePercentage"),
                    config.getString("i2cpHost"),
                    config.getInt("i2cpPort"),
                    config.getBoolean("embeddedRouter"),
                    config.getBoolean("extendedI2pLogging"));
        }

        private final int socketTimeout;
        private final int inboundKBytesPerSecond;
        private final int outboundKBytesPerSecond;
        private final int bandwidthSharePercentage;
        private final int i2cpPort;
        private final String i2cpHost;
        private boolean embeddedRouter;
        private final Path dataDir;
        private final boolean extendedI2pLogging;

        public Config(Path dataDir, int socketTimeout, int inboundKBytesPerSecond, int outboundKBytesPerSecond,
                      int bandwidthSharePercentage, String i2cpHost, int i2cpPort, boolean embeddedRouter,
                      boolean extendedI2pLogging) {
            this.dataDir = dataDir;
            this.socketTimeout = socketTimeout;
            this.inboundKBytesPerSecond = inboundKBytesPerSecond;
            this.outboundKBytesPerSecond = outboundKBytesPerSecond;
            this.bandwidthSharePercentage = bandwidthSharePercentage;
            this.i2cpHost = i2cpHost;
            this.i2cpPort = i2cpPort;
            this.embeddedRouter = embeddedRouter;
            this.extendedI2pLogging = extendedI2pLogging;
        }
    }

    private final String i2pDirPath;
    private I2pClient i2pClient;
    private boolean initializeCalled;
    private String sessionId;
    @Getter
    private final BootstrapInfo bootstrapInfo = new BootstrapInfo();
    private int numSocketsCreated = 0;
    private I2PTransportService.Config config;

    public I2PTransportService(TransportConfig config) {
        // Demonstrate potential usage of specific config.
        // Would be likely passed to i2p router not handled here...

        // Failed to get config generic...
        this.config = (I2PTransportService.Config) config;

        i2pDirPath = config.getDataDir().toAbsolutePath().toString();
        log.info("I2PTransport using i2pDirPath: {}", i2pDirPath);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (initializeCalled) {
            return CompletableFuture.completedFuture(true);
        }
        initializeCalled = true;
        log.debug("Initialize");

        bootstrapInfo.getBootstrapState().set(BootstrapState.BOOTSTRAP_TO_NETWORK);
        bootstrapInfo.getBootstrapDetails().set("Start bootstrapping");

        //If embedded router, start it already ...
        boolean isEmbeddedRouter;
        isEmbeddedRouter = isEmbeddedRouter();

        if (isEmbeddedRouter) {
            if (!I2pEmbeddedRouter.isInitialized()) {
                I2pEmbeddedRouter.getI2pEmbeddedRouter(i2pDirPath,
                        config.getInboundKBytesPerSecond(),
                        config.getOutboundKBytesPerSecond(),
                        config.getBandwidthSharePercentage(),
                        config.isExtendedI2pLogging());
            }
            while (!I2pEmbeddedRouter.isRouterRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            i2pClient = getClient(isEmbeddedRouter);
        } else {
            i2pClient = getClient(isEmbeddedRouter);
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        initializeCalled = false;
        if (i2pClient == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(i2pClient::shutdown, NetworkService.NETWORK_IO_POOL)
                .thenApply(nil -> true);
    }

    private boolean isEmbeddedRouter() {
        if (config.isEmbeddedRouter()) {
            //Is there a running router? If so ignore the config for embeeded router if exists
            if (NetworkUtils.isPortInUse("127.0.0.1", 7654)) {
                log.info("Embedded router parameter set to true, but there's a service running on 127.0.0.1:7654...");
                return false;
            }
            return true;
        }
        return false;
    }

    private I2pClient getClient(boolean isEmbeddedRouter) {
        return I2pClient.getI2pClient(i2pDirPath,
                config.getI2cpHost(),
                config.getI2cpPort(),
                config.getSocketTimeout(),
                isEmbeddedRouter);
    }


    @Override
    public ServerSocketResult getServerSocket(int port, String nodeId) {
        log.debug("Create serverSocket");
        try {
            bootstrapInfo.getBootstrapState().set(BootstrapState.START_PUBLISH_SERVICE);
            // 25%-50% we attribute to the publishing of the hidden service. Takes usually 5-10 sec.
            bootstrapInfo.getBootstrapProgress().set(0.25);
            bootstrapInfo.getBootstrapDetails().set("Create I2P service for node ID '" + nodeId + "'");

            sessionId = nodeId;
            //TODO: Investigate why not using port passed as parameter and if no port, find one?
            //Pass parameters to connect with Local instance
            int i2pPort = port;
            if (!isEmbeddedRouter()) {
                i2pPort = config.getI2cpPort();
            }
            ServerSocket serverSocket = i2pClient.getServerSocket(sessionId, config.getI2cpHost(), i2pPort);
            String destination = i2pClient.getMyDestination(sessionId);
            // Port is irrelevant for I2P
            Address address = new Address(destination, port);

            bootstrapInfo.getBootstrapState().set(BootstrapState.SERVICE_PUBLISHED);
            bootstrapInfo.getBootstrapProgress().set(0.5);
            bootstrapInfo.getBootstrapDetails().set("My I2P destination: " + address);


            log.debug("ServerSocket created. SessionId={}, destination={}", sessionId, destination);
            return new ServerSocketResult(nodeId, serverSocket, address);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new ConnectionException(exception);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        try {
            //todo check usage of sessionId
            log.debug("Create new Socket to {} with sessionId={}", address, sessionId);
            long ts = System.currentTimeMillis();
            Socket socket = i2pClient.getSocket(address.getHost(), sessionId);
            numSocketsCreated++;
            bootstrapInfo.getBootstrapState().set(BootstrapState.CONNECTED_TO_PEERS);
            bootstrapInfo.getBootstrapProgress().set(Math.min(1, 0.5 + numSocketsCreated / 10d));
            bootstrapInfo.getBootstrapDetails().set("Connected to " + numSocketsCreated + " peer(s)");
            log.info("I2P socket to {} created. Took {} ms", address, System.currentTimeMillis() - ts);
            return socket;
        } catch (IOException exception) {
            log.error(exception.toString(), exception);
            throw exception;
        }
    }

    @Override
    public boolean isPeerOnline(Address address) {
        throw new UnsupportedOperationException("isPeerOnline needs to be implemented for I2P.");
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        try {
            //todo
            String myDestination = i2pClient.getMyDestination(sessionId);
            return Optional.of(new Address(myDestination, -1));
        } catch (IOException exception) {
            log.error(exception.toString(), exception);
            return Optional.empty();
        }
    }
}
