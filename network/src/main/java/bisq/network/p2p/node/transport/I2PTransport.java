package bisq.network.p2p.node.transport;

import bisq.common.timer.Scheduler;
import bisq.common.util.NetworkUtils;
import bisq.i2p.I2pClient;
import bisq.i2p.I2pEmbeddedRouter;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.ConnectionException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.io.File.separator;

@Slf4j
public class I2PTransport implements Transport {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements Transport.Config {
        public static Config from(String baseDir, com.typesafe.config.Config config) {
            return new Config(baseDir,
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
        private final String baseDir;
        private final boolean extendedI2pLogging;

        public Config(String baseDir, int socketTimeout, int inboundKBytesPerSecond, int outboundKBytesPerSecond,
                      int bandwidthSharePercentage, String i2cpHost, int i2cpPort, boolean embeddedRouter,
                      boolean extendedI2pLogging) {
            this.baseDir = baseDir;
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

    private I2PTransport.Config config;

    public I2PTransport(Transport.Config config) {
        // Demonstrate potential usage of specific config.
        // Would be likely passed to i2p router not handled here...

        // Failed to get config generic...
        this.config = (I2PTransport.Config) config;

        i2pDirPath = config.getBaseDir() + separator + "i2p";
        log.info("I2PTransport using i2pDirPath: {}", i2pDirPath);
    }


    @Override
    public boolean initialize() {
        if (initializeCalled) {
            return true;
        }
        initializeCalled = true;
        log.debug("Initialize");

        //If embedded router, start it already ...
        if(config.isEmbeddedRouter()) {
            if(!I2pEmbeddedRouter.hasBeenInitialized()) {
                I2pEmbeddedRouter.getI2pEmbeddedRouter(i2pDirPath,
                        config.getInboundKBytesPerSecond(),
                        config.getOutboundKBytesPerSecond(),
                        config.getBandwidthSharePercentage(),
                        config.isExtendedI2pLogging());
            }
            while(!I2pEmbeddedRouter.isRouterRunning()){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            i2pClient = I2pClient.getI2pClient(i2pDirPath,
                    config.getI2cpHost(),
                    config.getI2cpPort(),
                    config.getSocketTimeout(),
                    config.isEmbeddedRouter());
        }
        else {
            i2pClient = I2pClient.getI2pClient(i2pDirPath,
                    config.getI2cpHost(),
                    config.getI2cpPort(),
                    config.getSocketTimeout(),
                    config.isEmbeddedRouter());
        }


        return true;
    }


    @Override
    public ServerSocketResult getServerSocket(int port, String nodeId) {
        log.debug("Create serverSocket");
        try {
            sessionId = nodeId + port;
            //TODO: Investigate why not using port passed as parameter and if no port, find one?
            //Pass parameters to connect with Local instance
            int i2pPort = port;
            if(!config.isEmbeddedRouter()) {
                i2pPort = config.getI2cpPort();
            }
            ServerSocket serverSocket = i2pClient.getServerSocket(sessionId, config.getI2cpHost(), i2pPort);
            String destination = i2pClient.getMyDestination(sessionId);
            // Port is irrelevant for I2P
            Address address = new Address(destination, port);
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
            log.info("I2P socket to {} created. Took {} ms", address, System.currentTimeMillis() - ts);
            return socket;
        } catch (IOException exception) {
            log.error(exception.toString(), exception);
            throw exception;
        }
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        initializeCalled = false;
        if (i2pClient == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(i2pClient::shutdown, NetworkService.NETWORK_IO_POOL);
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
