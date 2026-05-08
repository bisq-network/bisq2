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

import bisq.common.application.Service;
import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import bisq.common.observable.Observable;
import bisq.common.platform.LinuxDistribution;
import bisq.common.platform.OS;
import bisq.common.util.StringUtils;
import bisq.network.tor.common.torrc.BaseTorrcGenerator;
import bisq.network.tor.common.torrc.TorrcFileGenerator;
import bisq.network.tor.controller.TorControlConnectionClosedException;
import bisq.network.tor.controller.TorController;
import bisq.network.tor.controller.events.events.TorBootstrapEvent;
import bisq.network.tor.installer.TorInstaller;
import bisq.network.tor.process.EmbeddedTorProcess;
import bisq.network.tor.process.control_port.ControlPortFileParser;
import bisq.security.keys.TorKeyPair;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;
import static bisq.network.tor.common.torrc.Torrc.Keys.CONTROL_PORT;
import static bisq.network.tor.common.torrc.Torrc.Keys.CONTROL_SOCKET;
import static bisq.network.tor.common.torrc.Torrc.Keys.COOKIE_AUTHENTICATION;
import static bisq.network.tor.common.torrc.Torrc.Keys.COOKIE_AUTH_FILE;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class TorService implements Service {
    private static final int RANDOM_PORT = 0;
    // Environment variable to not launch the embedded Tor process
    public static final String TOR_SKIP_LAUNCH = "TOR_SKIP_LAUNCH";

    private final TorTransportConfig transportConfig;
    private final Path torDataDirPath;

    private TorController torController;
    private final Set<String> publishedOnionServices = new CopyOnWriteArraySet<>();
    @Getter
    private final Observable<TorBootstrapEvent> bootstrapEvent = new Observable<>();
    @Getter
    private final Observable<Boolean> useExternalTor = new Observable<>();
    private final AtomicBoolean isRunning = new AtomicBoolean();

    private Optional<EmbeddedTorProcess> torProcess = Optional.empty();
    private Optional<TorSocksProxyFactory> torSocksProxyFactory = Optional.empty();
    private final Map<String, String> externalTorConfigMap = new HashMap<>();
    private Optional<Path> activeExternalTorConfigPath = Optional.empty();

    public TorService(TorTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.torDataDirPath = transportConfig.getDataDirPath();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        boolean isNotRunning = isRunning.compareAndSet(false, true);
        if (!isNotRunning) {
            return CompletableFuture.completedFuture(true);
        }

        makeTorDir();

        readExternalTorConfigMap();

        useExternalTor.set(evaluateUseExternalTor());
        if (useExternalTor.get()) {
            bootstrapEvent.set(TorBootstrapEvent.CONNECT_TO_EXTERNAL_TOR);

            if (connectedToExternalTor(externalTorConfigMap)) {
                log.info("External Tor will be used");
                return CompletableFuture.completedFuture(true);
            } else {
                throw new RuntimeException("`useExternalTor` is enabled, but Bisq could not connect to the external Tor control " +
                        "server. Please check " + getExternalTorConfigDescription() + " and your system Tor configuration.");
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
        torController = new TorController(transportConfig.getBootstrapTimeout(), transportConfig.getHsUploadTimeout(), bootstrapEvent);

        PasswordDigest hashedControlPassword = PasswordDigest.generateDigest();
        createTorrcConfigFile(torDataDirPath, hashedControlPassword);

        // remove the control port file before we launch tor, just in case it exists from
        // a previous run
        Path controlDirPath = torDataDirPath.resolve(BaseTorrcGenerator.CONTROL_DIR_NAME);
        Path controlPortFilePath = controlDirPath.resolve("control");
        try {
            FileMutatorUtils.deleteFileAndWait(controlPortFilePath, 5000);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete tor control port file", e);
        }

        var embeddedTorProcess = new EmbeddedTorProcess(torBinaryPath, torDataDirPath);
        torProcess = Optional.of(embeddedTorProcess);
        embeddedTorProcess.start();

        try {
            FileReaderUtils.waitUntilFileExists(controlPortFilePath, 5000);
        } catch (Exception e) {
            throw new RuntimeException("Error while waiting for tor control port file to exist", e);
        }

        int controlPort = ControlPortFileParser.parse(controlPortFilePath);

        FileMutatorUtils.deleteOnExit(controlPortFilePath);

        torController.initialize(controlPort);
        torController.authenticate(hashedControlPassword);
        torController.bootstrap();

        int port = torController.getSocksPort();
        torSocksProxyFactory = Optional.of(new TorSocksProxyFactory(port));

        return CompletableFuture.completedFuture(true);
    }

    private boolean evaluateUseExternalTor() {
        boolean isWhonixOrTails = LinuxDistribution.isWhonix() || LinuxDistribution.isTails();
        String torSkipLaunch = System.getenv(TOR_SKIP_LAUNCH);
        String useExternalTorFromConfig = externalTorConfigMap.get("UseExternalTor");
        boolean useExternalTorFromJvmConfig = transportConfig.isUseExternalTor();

        boolean useExternalTor = UseExternalTorResolver.evaluateUseExternalTorValue(
                isWhonixOrTails,
                torSkipLaunch,
                useExternalTorFromJvmConfig,
                useExternalTorFromConfig);

        if (isWhonixOrTails) {
            log.info("Bisq runs on a Whonix/Tails system and use the system Tor");
        } else if (StringUtils.isNotEmpty(torSkipLaunch)) {
            log.info("Environment variable 'TOR_SKIP_LAUNCH' is set to '{}'", torSkipLaunch);
        }

        return useExternalTor;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.supplyAsync(() -> {
            torController.shutdown();
            torController = null;
            torProcess.ifPresent(EmbeddedTorProcess::waitUntilExited);
            torProcess = Optional.empty();
            torSocksProxyFactory = Optional.empty();
            publishedOnionServices.clear();
            externalTorConfigMap.clear();
            activeExternalTorConfigPath = Optional.empty();
            return true;
        }, commonForkJoinPool());
    }

    public CompletableFuture<String> publishOnionService(int localPort, int onionServicePort, TorKeyPair torKeyPair) {
        long ts = System.currentTimeMillis();
        try {
            String onionAddress = torKeyPair.getOnionAddress();
            log.info("Publish onion service for address {}:{} (localPort={})", onionAddress, onionServicePort, localPort);
            if (!publishedOnionServices.contains(onionAddress)) {
                torController.publish(torKeyPair, onionServicePort, localPort);
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

    public CompletableFuture<ServerSocket> publishOnionServiceAndCreateServerSocket(int port, TorKeyPair torKeyPair) {
        long ts = System.currentTimeMillis();
        try {
            InetAddress bindAddress = !LinuxDistribution.isWhonix() ? Inet4Address.getLoopbackAddress()
                    : Inet4Address.getByName("0.0.0.0");
            var localServerSocket = new ServerSocket(RANDOM_PORT, 50, bindAddress);

            String onionAddress = torKeyPair.getOnionAddress();
            log.info("Publish onion service for onion address {}:{}", onionAddress, port);
            if (!publishedOnionServices.contains(onionAddress)) {
                int localPort = localServerSocket.getLocalPort();
                torController.publish(torKeyPair, port, localPort);
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

    public Socket getSocket(String streamId) throws IOException {
        TorSocksProxyFactory socksProxyFactory = torSocksProxyFactory.orElseThrow();
        return socksProxyFactory.getSocket(streamId);
    }

    public Socks5Proxy getSocks5Proxy(String streamId) throws IOException {
        TorSocksProxyFactory socksProxyFactory = torSocksProxyFactory.orElseThrow();
        return socksProxyFactory.getSocks5Proxy(streamId);
    }

    private boolean connectedToExternalTor(Map<String, String> torConfigMap) {
        ControlEndpoint controlEndpoint = getControlEndpoint(torConfigMap);

        if (torController != null) {
            torController.shutdown();
        }
        torController = new TorController(transportConfig.getBootstrapTimeout(), transportConfig.getHsUploadTimeout(), bootstrapEvent);
        if (controlEndpoint instanceof UnixSocketControlEndpoint socketControlEndpoint) {
            torController.initialize(socketControlEndpoint.controlSocketPath());
        } else if (controlEndpoint instanceof TcpControlEndpoint tcpControlEndpoint) {
            torController.initialize(tcpControlEndpoint.controlHost(), tcpControlEndpoint.controlPort());
        }

        boolean isAuthenticated = false;
        boolean noCookieAuthenticationRequired = Optional.ofNullable(torConfigMap.get(COOKIE_AUTHENTICATION)).orElse("0").equals("0");
        if (noCookieAuthenticationRequired) {
            // No authentication required, but we still need to send an empty
            // AUTHENTICATE call to be able to send control commands
            try {
                torController.authenticate();
                isAuthenticated = true;
            } catch (Exception e) {
                log.warn("Authentication to Tor Control failed.", e);
                return false;
            }
        }

        if (!isAuthenticated) {
            if (torConfigMap.containsKey(COOKIE_AUTH_FILE)) {
                String authCookiePath = StringUtils.unquote(torConfigMap.get(COOKIE_AUTH_FILE));
                try {
                    byte[] authCookie = Files.readAllBytes(Paths.get(authCookiePath));
                    torController.authenticate(authCookie);
                    isAuthenticated = true;
                } catch (IOException e) {
                    log.warn("Couldn't read the COOKIE_AUTH_FILE '{}' from the config.", authCookiePath);
                } catch (TorControlConnectionClosedException e) {
                    log.warn("Tor control connection was closed unexpectedly when trying to authenticate.", e);
                    return false; // since pipe is broken
                } catch (Exception e) {
                    log.warn("Authentication to Tor Control failed.", e);
                }
            } else {
                log.warn("The user did not provide the path to the COOKIE_AUTH_FILE.");
            }
        }

        if (!isAuthenticated) {
            log.info("We try to call authenticate without parameters for the case that authentication is not required.");
            try {
                torController.authenticate();
            } catch (Exception e) {
                log.warn("Authentication to Tor Control failed.", e);
                return false;
            }
        }

        // We do not call torController.bootstrap as we do not need to bootstrap Tor. Instead, we set the event to
        // trigger the application state update.
        bootstrapEvent.set(TorBootstrapEvent.CONNECTION_TO_EXTERNAL_TOR_COMPLETED);

        int port = torController.getSocksPort();
        log.info("Tor control provided SOCKS port: {}", port);
        torSocksProxyFactory = Optional.of(new TorSocksProxyFactory(port));
        return true;
    }

    private ControlEndpoint getControlEndpoint(Map<String, String> externalTorConfigMap) {
        String controlSocketString = externalTorConfigMap.get(CONTROL_SOCKET);
        if (controlSocketString != null && !controlSocketString.isBlank()) {
            Path controlSocketPath = Path.of(StringUtils.unquote(controlSocketString));
            return ControlEndpoint.forSocket(controlSocketPath);
        }

        String controlPortString = externalTorConfigMap.get(CONTROL_PORT);
        checkArgument(controlPortString != null && !controlPortString.isBlank(),
                "Either ControlSocket or ControlPort must be set in " + getExternalTorConfigDescription());
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
        return ControlEndpoint.forPort(controlHost, controlPort);
    }

    private void readExternalTorConfigMap() {
        try {
            Path externalTorConfigPath = getExternalTorConfigPath();
            activeExternalTorConfigPath = Optional.of(externalTorConfigPath);
            externalTorConfigMap.clear();
            String torConfig = readExternalTorConfig(externalTorConfigPath);
            for (String line : torConfig.lines().toList()) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    int firstSpaceIndex = line.indexOf(" ");
                    if (firstSpaceIndex != -1) {
                        String key = line.substring(0, firstSpaceIndex);
                        String value = line.substring(firstSpaceIndex + 1);
                        externalTorConfigMap.put(key, value);
                    } else {
                        log.warn("Ignoring malformed line (no key/value separator) in external Tor config: '{}'", line);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not read external tor config file.", e);
        }
    }

    private String readExternalTorConfig(Path externalTorConfigPath) throws IOException {
        if (!Files.exists(externalTorConfigPath)) {
            String fallbackTorConfigFileName = "external_tor.config";
            Path fallbackTorConfigPath = transportConfig.getDataDirPath().resolve(fallbackTorConfigFileName);
            String fallbackTorConfigTemplate = FileReaderUtils.readStringFromResource("tor/" + fallbackTorConfigFileName);
            String fallbackTorConfig = ExternalTorConfigHeuristics.apply(fallbackTorConfigTemplate);
            FileMutatorUtils.writeToPath(fallbackTorConfig, fallbackTorConfigPath);
            return fallbackTorConfig;
        }
        return FileReaderUtils.readUTF8String(externalTorConfigPath);
    }

    private String getExternalTorConfigDescription() {
        return activeExternalTorConfigPath
                .map(path -> "the external Tor config file '" + path + "'")
                .orElse("the external Tor config file");
    }

    private Path getExternalTorConfigPath() {
        String torrcOverrideFilePath = transportConfig.getTorrcOverrideFilePath();
        if (torrcOverrideFilePath != null && !torrcOverrideFilePath.isBlank()) {
            Path overrideFilePath = resolveTorrcOverrideFilePath(transportConfig.getDataDirPath(), torrcOverrideFilePath);
            if (Files.exists(overrideFilePath)) {
                return overrideFilePath;
            }
            log.warn("torrcOverrideFilePath '{}' does not exist. Falling back to external_tor.config.",
                    torrcOverrideFilePath);
        }
        return transportConfig.getDataDirPath().resolve("external_tor.config");
    }

    private void makeTorDir() {
        try {
            Path torDataDirPath = transportConfig.getDataDirPath();
            FileMutatorUtils.createDirectories(torDataDirPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        return FacadeProvider.getJdkFacade().getProcessCommandStream().anyMatch(e -> e.equals(absoluteTorBinaryPath));
    }

    private void installTorIfNotUpToDate() {
        Path torDataDirPath = transportConfig.getDataDirPath();
        var torInstaller = new TorInstaller(torDataDirPath);
        torInstaller.installIfNotUpToDate();
    }

    private List<String> createTorrcConfigFile(Path dataDirPath, PasswordDigest hashedControlPassword) {
        TorrcClientConfigFactory torrcClientConfigFactory = TorrcClientConfigFactory.builder()
                .isTestNetwork(transportConfig.isTestNetwork())
                .dataDirPath(dataDirPath)
                .hashedControlPassword(hashedControlPassword)
                .build();

        Map<String, List<String>> overrides = resolveOverrides(dataDirPath);
        List<String> torrcConfigLines = torrcClientConfigFactory.torrcClientConfigLines(overrides);

        Path torrcPath = dataDirPath.resolve("torrc");
        var torrcFileGenerator = new TorrcFileGenerator(torrcPath,
                torrcConfigLines,
                transportConfig.getDirectoryAuthorities());
        torrcFileGenerator.generate();
        return torrcConfigLines;
    }

    /**
     * Returns the effective torrc overrides.  If {@code torrcOverrideFilePath} is set it is
     * resolved against the data directory (when relative) and its contents take precedence over
     * the inline {@code torrcOverrides} map.  Falls back to {@code torrcOverrides} when the
     * file path is empty or the file cannot be read.
     */
    private Map<String, List<String>> resolveOverrides(Path dataDirPath) {
        String filePathStr = transportConfig.getTorrcOverrideFilePath();
        if (filePathStr == null || filePathStr.isBlank()) {
            return transportConfig.getTorrcOverrides();
        }
        try {
            Path overrideFilePath = resolveTorrcOverrideFilePath(dataDirPath, filePathStr);
            return TorTransportConfig.parseTorrcOverrideFile(overrideFilePath);
        } catch (IOException e) {
            log.warn("Could not read torrcOverrideFilePath '{}', falling back to torrcOverrides. Error: {}",
                    filePathStr, e.getMessage());
            return transportConfig.getTorrcOverrides();
        }
    }

    private Path resolveTorrcOverrideFilePath(Path dataDirPath, String torrcOverrideFilePath) {
        Path overrideFilePath = Path.of(torrcOverrideFilePath);
        if (!overrideFilePath.isAbsolute()) {
            overrideFilePath = dataDirPath.resolve(overrideFilePath);
        }
        return overrideFilePath;
    }

    private sealed interface ControlEndpoint permits TcpControlEndpoint, UnixSocketControlEndpoint {

        static ControlEndpoint forPort(String controlHost, int controlPort) {
            return new TcpControlEndpoint(controlHost, controlPort);
        }

        static ControlEndpoint forSocket(Path controlSocketPath) {
            return new UnixSocketControlEndpoint(controlSocketPath);
        }
    }

    private record TcpControlEndpoint(String controlHost, int controlPort) implements ControlEndpoint {
        @Override
        @NonNull
        public String toString() {
            return String.format("TcpControlEndpoint(host: %s, port: %d)", controlHost, controlPort);
        }
    }

    private record UnixSocketControlEndpoint(Path controlSocketPath) implements ControlEndpoint {
        @Override
        @NonNull
        public String toString() {
            return String.format("UnixSocketControlEndpoint(controlSocketPath: %s)", controlSocketPath);
        }
    }
}
