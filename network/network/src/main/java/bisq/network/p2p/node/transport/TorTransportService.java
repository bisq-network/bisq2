package bisq.network.p2p.node.transport;

import bisq.common.network.Address;
import bisq.common.network.TorAddress;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.threading.ExecutorFactory;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.ConnectionException;
import bisq.network.p2p.node.handshake.InboundHandshakeHandler;
import bisq.network.p2p.node.handshake.OutboundHandshakeHandler;
import bisq.network.protobuf.NetworkEnvelope;
import bisq.network.tor.TorService;
import bisq.network.tor.TorTransportConfig;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.TorKeyPair;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
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
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public class TorTransportService implements TransportService {
    private static TorService torService;

    private final int socketTimeout;
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

    public TorTransportService(TransportConfig config) {
        socketTimeout = config.getSocketTimeout();
        if (torService == null) {
            setTransportState(TransportState.NEW);
            torService = new TorService((TorTransportConfig) config);
        }
    }

    @Override
    public void initialize() {
        log.info("Initialize Tor");
        long ts = System.currentTimeMillis();
        setTransportState(TransportState.INITIALIZE);
        torService.initialize().join();
        setTransportState(TransportState.INITIALIZED);
        log.info("Initializing Tor took {} ms", System.currentTimeMillis() - ts);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        setTransportState(TransportState.STOPPING);

        CompletableFuture<Void> bossGroupShutdown = new CompletableFuture<>();
        bossGroup.shutdownGracefully().addListener(future ->
                bossGroupShutdown.complete(null)
        );

        CompletableFuture<Void> workerGroupShutdown = new CompletableFuture<>();
        workerGroup.shutdownGracefully().addListener(future ->
                workerGroupShutdown.complete(null)
        );

        CompletableFuture<Void> groupsShutdown = CompletableFuture.allOf(bossGroupShutdown, workerGroupShutdown);

        // When both groups are done, shutdown torService
        return groupsShutdown
                .thenCompose(v -> torService.shutdown())
                .whenComplete((result, throwable) -> setTransportState(TransportState.TERMINATED));
    }

    @Override
    public CompletableFuture<Address> startNettyServer(NetworkId networkId,
                                                       KeyBundle keyBundle,
                                                       Supplier<InboundHandshakeHandler> handshakeHandlerSupplier) {

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler())
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel channel) {
                            InboundHandshakeHandler inboundHandshakeHandler = handshakeHandlerSupplier.get();
                            // For inbound tor connections we don't need the Socks5ProxyHandler, only for outbound.
                            channel.pipeline()
                                    .addLast(new LoggingHandler())
                                    .addLast(new ProtobufVarint32FrameDecoder())
                                    .addLast(new ProtobufDecoder(NetworkEnvelope.getDefaultInstance()))
                                    .addLast(new ProtobufVarint32LengthFieldPrepender())
                                    .addLast(new ProtobufEncoder())
                                    .addLast(inboundHandshakeHandler);
                        }
                    });
            int port = networkId.getAddressByTransportTypeMap().get(TransportType.TOR).getPort();
            TorKeyPair torKeyPair = keyBundle.getTorKeyPair();
            Address address = evaluateMyAddress(networkId, keyBundle).get();
            CompletableFuture<Address> serverFuture = new CompletableFuture<>();
            torService.publishOnionServiceForNetty(port, torKeyPair)
                    .whenComplete((localPort, throwable) -> {
                        if (throwable == null) {
                            bootstrap.bind(localPort).addListener(future -> {
                                if (future instanceof ChannelFuture channelFuture && future.isSuccess()) {
                                    Channel channel = channelFuture.channel();
                                    serverFuture.complete(address);
                                } else {
                                    serverFuture.completeExceptionally(future.cause());
                                }
                            });
                        } else {
                            log.error("publishOnionServiceForNetty failed", throwable);
                        }
                    });
            return serverFuture;
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at getServerSocket method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
            throw new ConnectionException(e);
        } catch (ExecutionException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public CompletableFuture<Channel> connect(Address address,
                                              Supplier<OutboundHandshakeHandler> handshakeHandlerSupplier) {
        return CompletableFuture.supplyAsync(() -> {
            int socksPort = torService.getSocksPort();
            checkArgument(socksPort != -1, "socksPort is not yet set. torService must be initialized before connect is called.");
            CompletableFuture<Channel> future = new CompletableFuture<>();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000000);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            OutboundHandshakeHandler outboundHandshakeHandler = handshakeHandlerSupplier.get();
                            Socks5ProxyHandler socks5ProxyHandler = new Socks5ProxyHandler(new InetSocketAddress("127.0.0.1", socksPort));
                            socks5ProxyHandler.setConnectTimeoutMillis(1000000);
                            socketChannel.pipeline()
                                    .addFirst(socks5ProxyHandler)
                                    .addLast(new ProtobufVarint32FrameDecoder())
                                    .addLast(new ProtobufDecoder(NetworkEnvelope.getDefaultInstance()))
                                    .addLast(new ProtobufVarint32LengthFieldPrepender())
                                    .addLast(new ProtobufEncoder())
                                    .addLast(outboundHandshakeHandler);
                        }
                    });

            // Create unresolved socket address so Tor handles DNS resolution
            InetSocketAddress unresolved = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());
            ChannelFuture connect = bootstrap.connect(unresolved);
            connect.addListener((ChannelFutureListener) channelFuture -> {
                if (!channelFuture.isSuccess()) {
                    // complete only on TCP failure (e.g., Tor not listening)
                    future.completeExceptionally(channelFuture.cause());
                }
            });
            return future.join();
        }, ExecutorFactory.newSingleThreadExecutor(""));
    }

    @Override
    public CompletableFuture<Address> evaluateMyAddress(NetworkId networkId, KeyBundle keyBundle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int port = networkId.getAddressByTransportTypeMap().get(TransportType.TOR).getPort();
                TorKeyPair torKeyPair = keyBundle.getTorKeyPair();
                String onionAddress = torKeyPair.getOnionAddress();
                return new TorAddress(onionAddress, port);
            } catch (Exception exception) {
                throw new ConnectionException(exception);
            }
        });
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle, String nodeId) {
        try {
            Optional<Address> optionalAddress = networkId.getAddressByTransportTypeMap().getAddress(TransportType.TOR);
            checkArgument(optionalAddress.isPresent(), "networkId.getAddressByTransportTypeMap().getAddress(TransportType.TOR) must not be empty");
            int port = optionalAddress.map(Address::getPort).orElseThrow();
            initializeServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());

            TorKeyPair torKeyPair = keyBundle.getTorKeyPair();
            Address address = evaluateMyAddress(networkId, keyBundle).get();
            ServerSocket serverSocket = torService.publishOnionServiceAndCreateServerSocket(port, torKeyPair).get();
            initializedServerSocketTimestampByNetworkId.put(networkId, System.currentTimeMillis());
            return new ServerSocketResult(serverSocket, address);
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at getServerSocket method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
            throw new ConnectionException(e);
        } catch (ExecutionException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address, String nodeId) throws IOException {
        if (address instanceof TorAddress torAddress) {
            long ts = System.currentTimeMillis();
            log.info("Start creating tor socket to {}", torAddress);
            Socket socket = torService.getSocket(null); // Blocking call. Takes 5-15 sec usually.
            socket.setSoTimeout(socketTimeout);
            InetSocketAddress inetSocketAddress = InetSocketAddress.createUnresolved(torAddress.getHost(), torAddress.getPort());
            try {
                socket.connect(inetSocketAddress);
            } catch (IOException e) {
                socket.close();
                throw e;
            }
            log.info("Tor socket creation to {} took {} ms", torAddress, System.currentTimeMillis() - ts);
            return socket;
        } else {
            throw new IllegalArgumentException("Address is not a TorAddress");
        }
    }

    @Override
    public CompletableFuture<Boolean> isPeerOnlineAsync(Address address, String nodeId) {
        if (address instanceof TorAddress torAddress) {
            return torService.isOnionServiceOnlineAsync(torAddress.getHost());
        } else {
            throw new IllegalArgumentException("Address is not a TorAddress");
        }
    }

    @Override
    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.of(torService.getSocks5Proxy(null));
    }

    public Observable<Boolean> getUseExternalTor() {
        return torService.getUseExternalTor();
    }

    public CompletableFuture<String> publishOnionService(int localPort, int onionServicePort, TorKeyPair torKeyPair) {
        return torService.publishOnionService(localPort, onionServicePort, torKeyPair);
    }
}
