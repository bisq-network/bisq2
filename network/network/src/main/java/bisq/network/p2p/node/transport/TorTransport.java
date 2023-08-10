package bisq.network.p2p.node.transport;

import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.ConnectionException;
import bisq.tor.DirectoryAuthority;
import bisq.tor.TorService;
import bisq.tor.onionservice.CreateOnionServiceResponse;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public class TorTransport implements Transport {
    public final static int DEFAULT_PORT = 9999;

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements Transport.Config {
        public static Config from(String baseDir, com.typesafe.config.Config config) {
            return new Config(
                    config.getBoolean("testNetwork"),
                    baseDir,
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")),
                    parseDirectoryAuthorities(config.getList("directoryAuthorities")),
                    parseTorrcOverrideConfig(config.getConfig("torrcOverrides"))
            );
        }

        private static Set<DirectoryAuthority> parseDirectoryAuthorities(ConfigList directoryAuthoritiesConfig) {
            Set<DirectoryAuthority> allDirectoryAuthorities = new HashSet<>();
            directoryAuthoritiesConfig.forEach(authConfig -> {
                DirectoryAuthority directoryAuthority = DirectoryAuthority.builder()
                        .nickname(getStringFromConfigValue(authConfig, "nickname"))
                        .orPort(Integer.parseInt(getStringFromConfigValue(authConfig, "orPort")))
                        .v3Ident(getStringFromConfigValue(authConfig, "v3Ident"))
                        .dirPort(Integer.parseInt(getStringFromConfigValue(authConfig, "dirPort")))
                        .relayFingerprint(getStringFromConfigValue(authConfig, "relayFingerprint"))
                        .build();

                allDirectoryAuthorities.add(directoryAuthority);
            });
            return allDirectoryAuthorities;
        }

        private static Map<String, String> parseTorrcOverrideConfig(com.typesafe.config.Config torrcOverrides) {
            Map<String, String> torrcOverrideConfigMap = new HashMap<>();
            torrcOverrides.entrySet()
                    .forEach(entry -> torrcOverrideConfigMap.put(
                            entry.getKey(), (String) entry.getValue().unwrapped()
                    ));
            return torrcOverrideConfigMap;
        }

        private static String getStringFromConfigValue(ConfigValue configValue, String key) {
            return (String) configValue
                    .atKey(key)
                    .getObject(key)
                    .get(key)
                    .unwrapped();
        }

        private final boolean isTestNetwork;
        private final int socketTimeout;
        private final String baseDir;
        private final Set<DirectoryAuthority> directoryAuthorities;
        private final Map<String, String> torrcOverrides;

        public Config(boolean isTestNetwork,
                      String baseDir,
                      int socketTimeout,
                      Set<DirectoryAuthority> directoryAuthorities,
                      Map<String, String> torrcOverrides) {
            this.isTestNetwork = isTestNetwork;
            this.baseDir = baseDir;
            this.socketTimeout = socketTimeout;
            this.directoryAuthorities = directoryAuthorities;
            this.torrcOverrides = torrcOverrides;
        }
    }

    private final TorService torService;

    public TorTransport(Transport.Config config) {
        Path torDirPath = Paths.get(config.getBaseDir(), "tor");
        torService = new TorService(NetworkService.NETWORK_IO_POOL, torDirPath);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("Initialize Tor");
        return torService.initialize()
                .thenApply(isSuccess -> {
                    checkArgument(isSuccess, "Tor start failed");
                    return true;
                })
                .exceptionally(throwable -> {
                    log.error("tor.start failed", throwable);
                    throw new ConnectionException(throwable);
                });
    }

    @Override
    public ServerSocketResult getServerSocket(int port, String nodeId) {
        try {
            CompletableFuture<CreateOnionServiceResponse> completableFuture = torService.createOnionService(port, nodeId);
            CreateOnionServiceResponse response = completableFuture.get(2, TimeUnit.MINUTES);
            return new ServerSocketResult(response);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            throw new ConnectionException(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        long ts = System.currentTimeMillis();
        Socket socket = torService.getSocket(null); // Blocking call. Takes 5-15 sec usually.
        socket.connect(new InetSocketAddress(address.getHost(), address.getPort()));
        log.info("Tor socket to {} created. Took {} ms", address, System.currentTimeMillis() - ts);
        return socket;
    }

    @Override
    public boolean isPeerOnline(Address address) {
        return torService.isOnionServiceOnline(address.getHost());
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        return Optional.of(torService.getSocks5Proxy(null));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        log.info("Shutdown tor.");
        torService.shutdown().join();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        return torService.getHostName(serverId).map(hostName -> new Address(hostName, TorTransport.DEFAULT_PORT));
    }
}
