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
import bisq.common.data.Pair;
import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import bisq.common.observable.Observable;
import bisq.common.platform.LinuxDistribution;
import bisq.common.platform.OS;
import bisq.common.util.StringUtils;
import bisq.network.tor.common.torrc.BaseTorrcGenerator;
import bisq.network.tor.common.torrc.TorrcFileGenerator;
import bisq.network.tor.controller.TorControlAuthenticationFailed;
import bisq.network.tor.controller.TorController;
import bisq.network.tor.controller.events.events.TorBootstrapEvent;
import bisq.network.tor.installer.TorInstaller;
import bisq.network.tor.process.EmbeddedTorProcess;
import bisq.network.tor.process.control_port.ControlPortFileParser;
import bisq.security.keys.TorKeyPair;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;
import static bisq.network.tor.common.torrc.Torrc.Keys.CONTROL_PORT;
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
            } else if (LinuxDistribution.isWhonix()) {
                // In case we failed at connectedToExternalTor, we do not start the embedded Tor as fallback if we are on Whonix.
                throw new RuntimeException("Bisq runs on a Whonix system but it could not connect to the Whonix system Tor. " +
                        "Please check the Bisq external_tor.config and the system Tor torrc config.");
            } else {
                // In case connectedToExternalTor failed we try embedded Tor (if not running on Whonix)
                useExternalTor.set(false);
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
        Map<String, String> torrcConfigMap = createTorrcConfigFile(torDataDirPath, hashedControlPassword);

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
        boolean useExternalTor = false;
        if (LinuxDistribution.isWhonix()) {
            log.info("Bisq runs on a Whonix system and use the Whonix system Tor");
            return true;
        } else {
            // If environment variable is set we take that.
            String torSkipLaunch = System.getenv(TOR_SKIP_LAUNCH);
            if (StringUtils.isNotEmpty(torSkipLaunch)) {
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
        Pair<String, Integer> controlHostAndPort = getControlHostAndPort(torConfigMap);
        String controlHost = controlHostAndPort.getFirst();
        int controlPort = controlHostAndPort.getSecond();
        if (!isTorControlAvailable(controlHost, controlPort)) {
            log.warn("Couldn't connect to external Tor control server. " +
                    "This could happen if the control port is not correctly set up in 'external_tor.config' file.");
            return false;
        }

        if (torController != null) {
            torController.shutdown();
        }
        torController = new TorController(transportConfig.getBootstrapTimeout(), transportConfig.getHsUploadTimeout(), bootstrapEvent);
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
                    byte[] authCookie = Files.readAllBytes(Path.of(authCookiePath));
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

        // We do not call torController.bootstrap as we do not need to bootstrap Tor. Instead, we set the event to
        // trigger the application state update.
        bootstrapEvent.set(TorBootstrapEvent.CONNECTION_TO_EXTERNAL_TOR_COMPLETED);

        int port = torController.getSocksPort();
        log.info("Tor control provided SOCKS port: {}", port);
        torSocksProxyFactory = Optional.of(new TorSocksProxyFactory(port));
        return true;
    }

    private Pair<String, Integer> getControlHostAndPort(Map<String, String> externalTorConfigMap) {
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
        return new Pair<>(controlHost, controlPort);
    }

    private void readExternalTorConfigMap() {
        try {
            String torConfigFileName = "external_tor.config";
            Path torConfigFilePath = transportConfig.getDataDirPath().resolve(torConfigFileName);
            String torConfig;
            if (!Files.exists(torConfigFilePath)) {
                torConfig = FileReaderUtils.readStringFromResource("tor/" + torConfigFileName);
                FacadeProvider.getJdkFacade().writeString(torConfig, torConfigFilePath);
            } else {
                torConfig = FacadeProvider.getJdkFacade().readString(torConfigFilePath);
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
            FileMutatorUtils.createRestrictedDirectories(torDataDirPath);
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
}
