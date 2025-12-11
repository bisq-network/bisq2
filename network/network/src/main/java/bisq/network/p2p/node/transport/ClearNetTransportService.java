package bisq.network.p2p.node.transport;

import bisq.common.facades.FacadeProvider;
import bisq.common.network.Address;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.AndroidEmulatorAddressTypeFacade;
import bisq.common.network.clear_net_address_types.ClearNetAddressType;
import bisq.common.network.clear_net_address_types.LANAddressTypeFacade;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.handshake.InboundHandshakeHandler;
import bisq.network.p2p.node.handshake.OutboundHandshakeHandler;
import bisq.network.protobuf.NetworkEnvelope;
import bisq.security.keys.KeyBundle;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static bisq.common.facades.FacadeProvider.getClearNetAddressTypeFacade;
import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;
import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public class ClearNetTransportService implements TransportService {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements TransportConfig {
        public static Config from(Path dataDirPath, com.typesafe.config.Config config) {
            return new Config(dataDirPath,
                    config.hasPath("defaultNodePort") ? config.getInt("defaultNodePort") : -1,
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")),
                    config.getInt("sendMessageThrottleTime"),
                    config.getInt("receiveMessageThrottleTime"),
                    config.getInt("connectTimeoutMs"),
                    config.getEnum(ClearNetAddressType.class, "clearNetAddressType")
            );
        }

        private final Path dataDirPath;
        private final int defaultNodePort;
        private final int socketTimeout;
        private final int sendMessageThrottleTime;
        private final int receiveMessageThrottleTime;
        private final int connectTimeoutMs;
        private final ClearNetAddressType clearNetAddressType;

        public Config(Path dataDirPath,
                      int defaultNodePort,
                      int socketTimeout,
                      int sendMessageThrottleTime,
                      int receiveMessageThrottleTime,
                      int connectTimeoutMs,
                      ClearNetAddressType clearNetAddressType) {
            this.dataDirPath = dataDirPath;
            this.defaultNodePort = defaultNodePort;
            this.socketTimeout = socketTimeout;
            this.sendMessageThrottleTime = sendMessageThrottleTime;
            this.receiveMessageThrottleTime = receiveMessageThrottleTime;
            this.connectTimeoutMs = connectTimeoutMs;
            this.clearNetAddressType = clearNetAddressType;
        }
    }

    private final int socketTimeout;
    private final int connectTimeoutMs;
    private boolean initializeCalled;
    @Getter
    public final Observable<TransportState> transportState = new Observable<>(TransportState.NEW);
    @Getter
    public final ObservableHashMap<TransportState, Long> timestampByTransportState = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializeServerSocketTimestampByNetworkId = new ObservableHashMap<>();
    @Getter
    public final ObservableHashMap<NetworkId, Long> initializedServerSocketTimestampByNetworkId = new ObservableHashMap<>();

    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    public ClearNetTransportService(TransportConfig config) {
        socketTimeout = config.getSocketTimeout();
        connectTimeoutMs = ((Config) config).getConnectTimeoutMs();
        setTransportState(TransportState.NEW);

        switch (((Config) config).getClearNetAddressType()) {
            case LOCAL_HOST -> {
                FacadeProvider.setClearNetAddressTypeFacade(new LocalHostAddressTypeFacade());
            }
            case ANDROID_EMULATOR -> {
                FacadeProvider.setClearNetAddressTypeFacade(new AndroidEmulatorAddressTypeFacade());
            }
            case LAN -> {
                FacadeProvider.setClearNetAddressTypeFacade(new LANAddressTypeFacade());
            }
        }
    }

    @Override
    public void initialize() {
        if (initializeCalled) {
            return;
        }
        setTransportState(TransportState.INITIALIZE);
        initializeCalled = true;
        setTransportState(TransportState.INITIALIZED);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (!initializeCalled) {
            return CompletableFuture.completedFuture(true);
        }
        initializeCalled = false;
        setTransportState(TransportState.STOPPING);
        initializeServerSocketTimestampByNetworkId.clear();
        initializedServerSocketTimestampByNetworkId.clear();
        timestampByTransportState.clear();

        CompletableFuture<Void> bossGroupShutdown = new CompletableFuture<>();
        bossGroup.shutdownGracefully().addListener(future ->
                bossGroupShutdown.complete(null)
        );

        CompletableFuture<Void> workerGroupShutdown = new CompletableFuture<>();
        workerGroup.shutdownGracefully().addListener(future ->
                workerGroupShutdown.complete(null)
        );

        // Return a future that completes when BOTH are done
        return workerGroupShutdown
                .thenCombine(workerGroupShutdown, (b1, b2) -> true)
                .whenComplete((result, throwable) -> setTransportState(TransportState.TERMINATED));
    }

    @Override
    public CompletableFuture<Address> startNettyServer(NetworkId networkId,
                                                       KeyBundle keyBundle,
                                                       Supplier<InboundHandshakeHandler> handshakeHandlerSupplier) {
        int port = networkId.getAddressByTransportTypeMap().get(TransportType.CLEAR).getPort();
        Address address = getClearNetAddressTypeFacade().toMyLocalAddress(port);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler())
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        InboundHandshakeHandler inboundHandshakeHandler = handshakeHandlerSupplier.get();
                        channel.pipeline()
                                .addLast(new LoggingHandler())
                                .addLast(new ProtobufVarint32FrameDecoder())
                                .addLast(new ProtobufDecoder(NetworkEnvelope.getDefaultInstance()))
                                .addLast(new ProtobufVarint32LengthFieldPrepender())
                                .addLast(new ProtobufEncoder())
                                .addLast(inboundHandshakeHandler);
                    }
                });
        CompletableFuture<Address> serverStarted = new CompletableFuture<>();
        bootstrap.bind(port).addListener(future -> {
            if (future instanceof ChannelFuture channelFuture && future.isSuccess()) {
                Channel channel = channelFuture.channel();
                serverStarted.complete(address);
            } else {
                serverStarted.completeExceptionally(future.cause());
            }
        });
        return serverStarted;
    }

    @Override
    public CompletableFuture<Channel> connect(Address address,
                                              Supplier<OutboundHandshakeHandler> handshakeHandlerSupplier) {
        CompletableFuture<Channel> future = new CompletableFuture<>();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        OutboundHandshakeHandler outboundHandshakeHandler = handshakeHandlerSupplier.get();

                        socketChannel.pipeline()
                                .addLast(new LoggingHandler())
                                .addLast(new ProtobufVarint32FrameDecoder())
                                .addLast(new ProtobufDecoder(NetworkEnvelope.getDefaultInstance()))
                                .addLast(new ProtobufVarint32LengthFieldPrepender())
                                .addLast(new ProtobufEncoder())
                                .addLast(outboundHandshakeHandler);
                    }
                });
        ChannelFuture connect = bootstrap.connect(address.getHost(), address.getPort());
        connect.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                future.complete(channelFuture.channel());
            } else {
                future.completeExceptionally(channelFuture.cause());
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Address> evaluateMyAddress(NetworkId networkId, KeyBundle keyBundle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int port = networkId.getAddressByTransportTypeMap().get(TransportType.CLEAR).getPort();
                return getClearNetAddressTypeFacade().toMyLocalAddress(port);
            } catch (Exception exception) {
                throw new ConnectionException(exception);
            }
        });
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle, String nodeId) {
        Optional<Address> optionalAddress = networkId.getAddressByTransportTypeMap().getAddress(TransportType.CLEAR);
        checkArgument(optionalAddress.isPresent(), "networkId.getAddressByTransportTypeMap().getAddress(TransportType.CLEAR) must not be empty");
        int port = optionalAddress.map(Address::getPort).orElseThrow();
        initializeServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
        log.info("Create serverSocket at port {}", port);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Address address = evaluateMyAddress(networkId, keyBundle).get();
            log.debug("ServerSocket created at port {}", port);
            initializedServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
            return new ServerSocketResult(serverSocket, address);
        } catch (Exception e) {
            log.error("Error at getServerSocket. Port {}", port, e);
            throw new CompletionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address, String nodeId) throws IOException {
        if (address instanceof ClearnetAddress clearnetAddress) {
            clearnetAddress = getClearNetAddressTypeFacade().toPeersLocalAddress(clearnetAddress);
            log.debug("Create new Socket to {}", clearnetAddress);
            Socket socket = new Socket();
            socket.setSoTimeout(socketTimeout);
            socket.connect(new InetSocketAddress(clearnetAddress.getHost(), clearnetAddress.getPort()), connectTimeoutMs);
            return socket;
        } else {
            throw new IllegalArgumentException("Address is not a ClearnetAddress");
        }
    }

    @Override
    public CompletableFuture<Boolean> isPeerOnlineAsync(Address address, String nodeId) {
        if (address instanceof ClearnetAddress clearnetAddress) {
            return CompletableFuture.supplyAsync(() -> {
                try (Socket ignored = getSocket(clearnetAddress, nodeId)) {
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }, commonForkJoinPool());
        } else {
            throw new IllegalArgumentException("Address is not a ClearnetAddress");
        }
    }
}
