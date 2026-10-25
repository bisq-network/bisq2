/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.network.tor;

import bisq.network.tor.common.file.FileUtils;
import bisq.network.tor.common.platform.LinuxDistribution;
import bisq.network.tor.common.platform.OS;
import bisq.network.tor.common.torrc.BaseTorrcGenerator;
import bisq.network.tor.common.torrc.TorrcFileGenerator;
import bisq.network.tor.controller.TorControlAuthenticationFailed;
import bisq.network.tor.controller.TorController;
import bisq.network.tor.installer.TorInstaller;
import bisq.network.tor.onionservice.OnionServiceKey;
import bisq.network.tor.process.EmbeddedTorProcess;
import bisq.network.tor.process.control_port.ControlPortFileParser;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static bisq.network.tor.common.torrc.Torrc.Keys.CONTROL_PORT;
import static bisq.network.tor.common.torrc.Torrc.Keys.COOKIE_AUTHENTICATION;
import static bisq.network.tor.common.torrc.Torrc.Keys.COOKIE_AUTH_FILE;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class TorService {
    private static final int RANDOM_PORT = 0;
    private static final long CONTROL_PORT_FILE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);
    private static final long CONTROL_PORT_FILE_POLL_INTERVAL_MS = 100;
    // Environment variable to not launch the embedded Tor process
    public static final String TOR_SKIP_LAUNCH = "TOR_SKIP_LAUNCH";

    private final TorTransportConfig transportConfig;
    private final PlatformServices platformServices;
    private final Path torDataDirPath;

    private TorController torController;
    private final Set<String> publishedOnionServices = new CopyOnWriteArraySet<>();
    // Written by initialize, read by the user interface
    @Getter
    private volatile boolean useExternalTor;
    private final AtomicBoolean isRunning = new AtomicBoolean();

    private Optional<EmbeddedTorProcess> torProcess = Optional.empty();
    private Optional<TorSocksProxyFactory> torSocksProxyFactory = Optional.empty();
    private final Map<String, String> externalTorConfigMap = new HashMap<>();

    public TorService(TorTransportConfig transportConfig, PlatformServices platformServices) {
        this.transportConfig = transportConfig;
        this.platformServices = platformServices;
        this.torDataDirPath = transportConfig.getDataDirPath();
    }

    public CompletableFuture<Boolean> initialize() {
        boolean isNotRunning = isRunning.compareAndSet(false, true);
        if (!isNotRunning) {
            return CompletableFuture.completedFuture(true);
        }

        makeTorDir();

        readExternalTorConfigMap();

        useExternalTor = evaluateUseExternalTor();
        if (useExternalTor) {
            if (connectedToExternalTor(externalTorConfigMap)) {
                log.info("External Tor will be used");
                return CompletableFuture.completedFuture(true);
            } else if (LinuxDistribution.isWhonix()) {
                // In case we failed at connectedToExternalTor, we do not start the embedded Tor as fallback if we are on Whonix.
                throw new RuntimeException("Bisq runs on a Whonix system but it could not connect to the Whonix system Tor. " +
                        "Please check the Bisq external_tor.config and the system Tor torrc config.");
            } else {
                // In case connectedToExternalTor failed we try embedded Tor (if not running on Whonix)
                useExternalTor = false;
            }
        }

        if (!OS.isLinux()) {
            installTorIfNotUpToDate();
        }

        Path torBinaryPath = getTorBinaryPath();
        if (!isTorRunning(torBinaryPath.toString())) {
            Path lockFilePath = torDataDirPath.resolve("lock");
            try {
                Files.deleteIfExists(lockFilePath);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't remove tor lock file.", e);
            }
        }

        if (torController != null) {
            torController.shutdown();
        }
        torController = new TorController(transportConfig.getBootstrapTimeout(), transportConfig.getHsUploadTimeout());

        PasswordDigest hashedControlPassword = PasswordDigest.generateDigest();
        Map<String, String> torrcConfigMap = createTorrcConfigFile(torDataDirPath, hashedControlPassword);

        // remove the control port file before we launch tor, just in case it exists from
        // a previous run
        Path controlDirPath = torDataDirPath.resolve(BaseTorrcGenerator.CONTROL_DIR_NAME);
        Path controlPortFilePath = controlDirPath.resolve("control");
        try {
            FileUtils.deleteFileAndWait(controlPortFilePath, 5000);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete tor control port file", e);
        }

        var embeddedTorProcess = new EmbeddedTorProcess(torBinaryPath, torDataDirPath, platformServices.getMyPid());
        torProcess = Optional.of(embeddedTorProcess);
        embeddedTorProcess.start();

        try {
            waitUntilControlPortFileExists(controlPortFilePath, embeddedTorProcess);
        } catch (InterruptedException e) {
            embeddedTorProcess.shutdown();
            torProcess = Optional.empty();
            isRunning.set(false);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for tor control port file to exist", e);
        } catch (Exception e) {
            embeddedTorProcess.shutdown();
            torProcess = Optional.empty();
            isRunning.set(false);
            throw new RuntimeException("Error while waiting for tor control port file to exist", e);
        }

        int controlPort = ControlPortFileParser.parse(controlPortFilePath);

        controlPortFilePath.toFile().deleteOnExit();

        torController.initialize(controlPort);
        torController.authenticate(hashedControlPassword);
        torController.bootstrap();

        int port = torController.getSocksPort();
        torSocksProxyFactory = Optional.of(new TorSocksProxyFactory(port));

        return CompletableFuture.completedFuture(true);
    }

    private boolean evaluateUseExternalTor() {
        if (LinuxDistribution.isWhonix()) {
            log.info("Bisq runs on a Whonix system and use the Whonix system Tor");
            return true;
        } else {
            // If environment variable is set we take that.
            String torSkipLaunch = System.getenv(TOR_SKIP_LAUNCH);
            if (torSkipLaunch != null && !torSkipLaunch.trim().isEmpty()) {
                log.info("Environment variable 'TOR_SKIP_LAUNCH' is set to '{}'", torSkipLaunch);
                return torSkipLaunch.equals("1") ||
                        torSkipLaunch.equalsIgnoreCase("true") ||
                        torSkipLaunch.equalsIgnoreCase("yes");
            } else {
                // We check if external_tor.config value is set. If so we use that value
                Optional<String> externalTorFromConfigMap = Optional.ofNullable(externalTorConfigMap.get("UseExternalTor"));
                if (externalTorFromConfigMap.isPresent()) {
                    return externalTorFromConfigMap.get().equals("1");
                } else {
                    // Last we take the value form the JVM config
                    return transportConfig.isUseExternalTor();
                }
            }
        }
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.supplyAsync(() -> {
            if (torController != null) {
                torController.shutdown();
            }
            torController = null;
            torProcess.ifPresent(EmbeddedTorProcess::shutdown);
            torProcess = Optional.empty();
            torSocksProxyFactory = Optional.empty();
            publishedOnionServices.clear();
            externalTorConfigMap.clear();
            return true;
        }, ForkJoinPool.commonPool());
    }

    public CompletableFuture<String> publishOnionService(int localPort, int onionServicePort, OnionServiceKey onionServiceKey) {
        long ts = System.currentTimeMillis();
        try {
            String onionAddress = onionServiceKey.getOnionAddress();
            log.info("Publish onion service for address {}:{} (localPort={})", onionAddress, onionServicePort, localPort);
            if (!publishedOnionServices.contains(onionAddress)) {
                torController.publish(onionServiceKey, onionServicePort, localPort);
                publishedOnionServices.add(onionAddress);
            }

            log.info("Tor onion service Ready. Took {} ms. Onion address={}:{} (localPort={})",
                    System.currentTimeMillis() - ts, onionAddress, onionServicePort, localPort);

            return CompletableFuture.completedFuture(onionAddress);
        } catch (InterruptedException e) {
            log.error("Can't create onion service", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<ServerSocket> publishOnionServiceAndCreateServerSocket(int port, OnionServiceKey onionServiceKey) {
        long ts = System.currentTimeMillis();
        try {
            InetAddress bindAddress = !LinuxDistribution.isWhonix() ? Inet4Address.getLoopbackAddress()
                    : Inet4Address.getByName("0.0.0.0");
            var localServerSocket = new ServerSocket(RANDOM_PORT, 50, bindAddress);

            String onionAddress = onionServiceKey.getOnionAddress();
            log.info("Publish onion service for onion address {}:{}", onionAddress, port);
            if (!publishedOnionServices.contains(onionAddress)) {
                int localPort = localServerSocket.getLocalPort();
                torController.publish(onionServiceKey, port, localPort);
                publishedOnionServices.add(onionAddress);
            }

            log.info("Tor onion service Ready. Took {} ms. Onion address={}:{}",
                    System.currentTimeMillis() - ts, onionAddress, port);

            return CompletableFuture.completedFuture(localServerSocket);
        } catch (InterruptedException e) {
            log.warn("Can't create onion service. Thread got interrupted at publishOnionService method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
            return CompletableFuture.failedFuture(e);
        } catch (IOException e) {
            log.error("Can't create onion service", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Boolean> isOnionServiceOnlineAsync(String onionUrl) {
        return torController.isOnionServiceOnlineAsync(onionUrl)
                .orTimeout(1, TimeUnit.MINUTES);
    }

    public Socket getSocket(String host, int port, String streamId, int socketTimeout) throws IOException {
        TorSocksProxyFactory socksProxyFactory = torSocksProxyFactory.orElseThrow();
        return socksProxyFactory.getSocket(host, port, streamId, socketTimeout);
    }

    public Socks5Proxy getSocks5Proxy() throws IOException {
        TorSocksProxyFactory socksProxyFactory = torSocksProxyFactory.orElseThrow();
        return socksProxyFactory.getSocks5Proxy();
    }

    private boolean connectedToExternalTor(Map<String, String> torConfigMap) {
        ControlHostAndPort controlHostAndPort = getControlHostAndPort(torConfigMap);
        String controlHost = controlHostAndPort.host();
        int controlPort = controlHostAndPort.port();
        if (!isTorControlAvailable(controlHost, controlPort)) {
            log.warn("Couldn't connect to external Tor control server. " +
                    "This could happen if the control port is not correctly set up in 'external_tor.config' file.");
            return false;
        }

        if (torController != null) {
            torController.shutdown();
        }
        torController = new TorController(transportConfig.getBootstrapTimeout(), transportConfig.getHsUploadTimeout());
        torController.initialize(controlPort);

        boolean isAuthenticated = false;
        boolean noCookieAuthenticationRequired = Optional.ofNullable(torConfigMap.get(COOKIE_AUTHENTICATION)).orElse("0").equals("0");
        if (noCookieAuthenticationRequired) {
            // No authentication required, but we still need to send an empty
            // AUTHENTICATE call to be able to send control commands
            try {
                torController.authenticate();
                isAuthenticated = true;
            } catch (TorControlAuthenticationFailed e) {
                log.warn("Authentication to Tor Control failed.");
                return false;
            }
        }

        if (!isAuthenticated) {
            if (torConfigMap.containsKey(COOKIE_AUTH_FILE)) {
                String authCookiePath = torConfigMap.get(COOKIE_AUTH_FILE);
                try {
                    byte[] authCookie = Files.readAllBytes(Paths.get(authCookiePath));
                    torController.authenticate(authCookie);
                    isAuthenticated = true;
                } catch (TorControlAuthenticationFailed e) {
                    log.warn("Authentication with authCookie failed.");
                } catch (IOException e) {
                    log.warn("Couldn't read the COOKIE_AUTH_FILE '{}' from the config.", authCookiePath);
                }
            } else {
                log.warn("The user did not provide the path to the COOKIE_AUTH_FILE.");
            }
        }

        if (!isAuthenticated) {
            log.info("We try to call authenticate without parameters for the case that authentication is not required.");
            try {
                torController.authenticate();
            } catch (TorControlAuthenticationFailed ex) {
                log.warn("Authentication to Tor Control failed.");
                return false;
            }
        }

        // We do not call torController.bootstrap as we do not need to bootstrap Tor.
        int port = torController.getSocksPort();
        log.info("Tor control provided SOCKS port: {}", port);
        torSocksProxyFactory = Optional.of(new TorSocksProxyFactory(port));
        return true;
    }

    private ControlHostAndPort getControlHostAndPort(Map<String, String> externalTorConfigMap) {
        String controlPortString = externalTorConfigMap.get(CONTROL_PORT);
        String[] tokens = controlPortString.split(":");
        String controlHost;
        int controlPort;
        if (tokens.length == 2) {
            controlHost = tokens[0];
            controlPort = Integer.parseInt(tokens[1]);
        } else {
            checkArgument(tokens.length == 1,
                    "Value for CONTROL_PORT must be in the form of 'host:port' or 'port' but was: " + controlPortString);
            controlHost = "127.0.0.1";
            controlPort = Integer.parseInt(tokens[0]);
        }
        return new ControlHostAndPort(controlHost, controlPort);
    }

    private void readExternalTorConfigMap() {
        try {
            String torConfigFileName = "external_tor.config";
            Path torConfigFilePath = transportConfig.getDataDirPath().resolve(torConfigFileName);
            String torConfig;
            if (!Files.exists(torConfigFilePath)) {
                torConfig = FileUtils.readStringFromResource("tor/" + torConfigFileName);
                FileUtils.writeToPath(torConfig, torConfigFilePath, platformServices.supportsPosixPermissions());
            } else {
                torConfig = FileUtils.readUTF8String(torConfigFilePath);
            }
            Set<String> lines = torConfig.lines().collect(Collectors.toSet());
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    int firstSpaceIndex = line.indexOf(" ");
                    if (firstSpaceIndex != -1) {
                        String key = line.substring(0, firstSpaceIndex);
                        String value = line.substring(firstSpaceIndex + 1);
                        externalTorConfigMap.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not read external tor config file.", e);
        }
    }

    private boolean isTorControlAvailable(String controlHost, int controlPort) {
        try (Socket socket = new Socket()) {
            var socketAddress = new InetSocketAddress(controlHost, controlPort);
            socket.connect(socketAddress);
            log.info("Test connection to control server of external tor process successful.");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void makeTorDir() {
        try {
            Path torDataDirPath = transportConfig.getDataDirPath();
            FileUtils.createDirectories(torDataDirPath, platformServices.supportsPosixPermissions());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitUntilControlPortFileExists(Path controlPortFilePath,
                                                EmbeddedTorProcess embeddedTorProcess) throws InterruptedException, IOException {
        long start = System.currentTimeMillis();
        while (!Files.exists(controlPortFilePath)) {
            if (!embeddedTorProcess.isAlive()) {
                throw new IOException("Embedded Tor exited before writing control port file. " +
                        embeddedTorProcess.getStartupDiagnostics());
            }
            if (System.currentTimeMillis() - start > CONTROL_PORT_FILE_TIMEOUT_MS) {
                throw new IOException("Control port file did not exist after " + CONTROL_PORT_FILE_TIMEOUT_MS + " ms: " +
                        controlPortFilePath.toAbsolutePath() + ". " + embeddedTorProcess.getStartupDiagnostics());
            }
            Thread.sleep(CONTROL_PORT_FILE_POLL_INTERVAL_MS);
        }
    }

    private Path getTorBinaryPath() {
        if (OS.isLinux()) {
            Optional<Path> systemTorBinaryPath = EmbeddedTorProcess.getSystemTorPath();
            return systemTorBinaryPath.orElseThrow(TorNotInstalledException::new);
        }
        return torDataDirPath.resolve("tor");
    }

    private boolean isTorRunning(String absoluteTorBinaryPath) {
        return platformServices.getProcessCommandStream().anyMatch(e -> e.equals(absoluteTorBinaryPath));
    }

    private void installTorIfNotUpToDate() {
        Path torDataDirPath = transportConfig.getDataDirPath();
        var torInstaller = new TorInstaller(torDataDirPath);
        torInstaller.installIfNotUpToDate();
    }

    private Map<String, String> createTorrcConfigFile(Path dataDirPath, PasswordDigest hashedControlPassword) {
        TorrcClientConfigFactory torrcClientConfigFactory = TorrcClientConfigFactory.builder()
                .isTestNetwork(transportConfig.isTestNetwork())
                .dataDirPath(dataDirPath)
                .hashedControlPassword(hashedControlPassword)
                .build();

        Map<String, String> torrcOverrideConfigs = transportConfig.getTorrcOverrides();
        Map<String, String> torrcConfigMap = torrcClientConfigFactory.torrcClientConfigMap(torrcOverrideConfigs);

        Path torrcPath = dataDirPath.resolve("torrc");
        var torrcFileGenerator = new TorrcFileGenerator(torrcPath,
                torrcConfigMap,
                transportConfig.getDirectoryAuthorities());
        torrcFileGenerator.generate();
        return torrcConfigMap;
    }

    private record ControlHostAndPort(String host, int port) {
    }
}
