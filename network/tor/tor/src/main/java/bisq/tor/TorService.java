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

package bisq.tor;

import bisq.common.application.Service;
import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileUtils;
import bisq.common.observable.Observable;
import bisq.common.platform.LinuxDistribution;
import bisq.common.platform.OS;
import bisq.network.tor.common.torrc.BaseTorrcGenerator;
import bisq.network.tor.common.torrc.TorrcFileGenerator;
import bisq.security.keys.TorKeyPair;
import bisq.tor.controller.TorController;
import bisq.tor.controller.events.events.BootstrapEvent;
import bisq.tor.installer.TorInstaller;
import bisq.tor.process.NativeTorProcess;
import bisq.tor.process.control_port.ControlPortFilePoller;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TorService implements Service {
    private static final int RANDOM_PORT = 0;

    private final TorTransportConfig transportConfig;
    private final Path torDataDirPath;
    private final TorController torController;
    private final Set<String> publishedOnionServices = new CopyOnWriteArraySet<>();

    private final AtomicBoolean isRunning = new AtomicBoolean();

    private Optional<NativeTorProcess> torProcess = Optional.empty();
    private Optional<TorSocksProxyFactory> torSocksProxyFactory = Optional.empty();

    public TorService(TorTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.torDataDirPath = transportConfig.getDataDir();
        torController = new TorController(transportConfig.getBootstrapTimeout(), transportConfig.getHsUploadTimeout());
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        boolean isNotRunning = isRunning.compareAndSet(false, true);
        if (!isNotRunning) {
            return CompletableFuture.completedFuture(true);
        }

        if (!LinuxDistribution.isWhonix()) {
            try {
                Path torDataDirPath = transportConfig.getDataDir();
                FileUtils.makeDirs(torDataDirPath.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (!OS.isLinux()) {
                installTorIfNotUpToDate();
            }

            PasswordDigest hashedControlPassword = PasswordDigest.generateDigest();
            createTorrcConfigFile(torDataDirPath, hashedControlPassword);

            Path torBinaryPath = getTorBinaryPath();
            if (!isTorRunning(torBinaryPath.toString())) {
                File lockFile = torDataDirPath.resolve("lock").toFile();
                if (lockFile.exists()) {
                    boolean isSuccess = lockFile.delete();
                    if (!isSuccess) {
                        throw new IllegalStateException("Couldn't remove tor lock file.");
                    }
                }
            }

            var nativeTorProcess = new NativeTorProcess(torBinaryPath, torDataDirPath);
            torProcess = Optional.of(nativeTorProcess);
            nativeTorProcess.start();

            Path controlDirPath = torDataDirPath.resolve(BaseTorrcGenerator.CONTROL_DIR_NAME);
            Path controlPortFilePath = controlDirPath.resolve("control");

            return new ControlPortFilePoller(controlPortFilePath)
                    .parsePort()
                    .thenAccept(controlPort -> {
                        torController.initialize(controlPort, hashedControlPassword);
                        torController.bootstrap();

                        int port = torController.getSocksPort();
                        torSocksProxyFactory = Optional.of(new TorSocksProxyFactory(port));
                    })
                    .thenApply(unused -> true);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                torController.initialize(9051);
                torSocksProxyFactory = Optional.of(new TorSocksProxyFactory(9050));
                return true;
            });
        }
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.supplyAsync(() -> {
            torController.shutdown();
            torProcess.ifPresent(NativeTorProcess::waitUntilExited);
            return true;
        });
    }

    public CompletableFuture<ServerSocket> createOnionService(int port, TorKeyPair torKeyPair) {
        log.info("Start hidden service with port {}", port);
        long ts = System.currentTimeMillis();
        try {
            InetAddress bindAddress = !LinuxDistribution.isWhonix() ? Inet4Address.getLoopbackAddress()
                    : Inet4Address.getByName("0.0.0.0");
            var localServerSocket = new ServerSocket(RANDOM_PORT, 50, bindAddress);

            String onionAddress = torKeyPair.getOnionAddress();
            if (!publishedOnionServices.contains(onionAddress)) {
                int localPort = localServerSocket.getLocalPort();
                torController.publish(torKeyPair, port, localPort);
                publishedOnionServices.add(onionAddress);
            }

            log.info("Tor hidden service Ready. Took {} ms. Onion address={}",
                    System.currentTimeMillis() - ts, onionAddress);

            return CompletableFuture.completedFuture(localServerSocket);

        } catch (IOException | InterruptedException e) {
            log.error("Can't create onion service", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public boolean isOnionServiceOnline(String onionUrl) {
        try {
            return torController.isOnionServiceOnline(onionUrl).get(1, TimeUnit.MINUTES);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public Observable<BootstrapEvent> getBootstrapEvent() {
        return torController.getBootstrapEvent();
    }

    public Socket getSocket(String streamId) throws IOException {
        TorSocksProxyFactory socksProxyFactory = torSocksProxyFactory.orElseThrow();
        return socksProxyFactory.getSocket(streamId);
    }

    public Socks5Proxy getSocks5Proxy(String streamId) throws IOException {
        TorSocksProxyFactory socksProxyFactory = torSocksProxyFactory.orElseThrow();
        return socksProxyFactory.getSocks5Proxy(streamId);
    }

    private Path getTorBinaryPath() {
        if (OS.isLinux()) {
            Optional<Path> systemTorBinaryPath = NativeTorProcess.getSystemTorPath();
            return systemTorBinaryPath.orElseThrow(TorNotInstalledException::new);
        }
        return torDataDirPath.resolve("tor");
    }

    private boolean isTorRunning(String absoluteTorBinaryPath) {
        return FacadeProvider.getJdkFacade().getProcessCommandLineStream().anyMatch(e -> e.startsWith(absoluteTorBinaryPath));
    }

    private void installTorIfNotUpToDate() {
        Path torDataDirPath = transportConfig.getDataDir();
        var torInstaller = new TorInstaller(torDataDirPath);
        torInstaller.installIfNotUpToDate();
    }

    private void createTorrcConfigFile(Path dataDir, PasswordDigest hashedControlPassword) {
        TorrcClientConfigFactory torrcClientConfigFactory = TorrcClientConfigFactory.builder()
                .isTestNetwork(transportConfig.isTestNetwork())
                .dataDir(dataDir)
                .hashedControlPassword(hashedControlPassword)
                .build();

        Map<String, String> torrcOverrideConfigs = transportConfig.getTorrcOverrides();
        Map<String, String> torrcConfigMap = torrcClientConfigFactory.torrcClientConfigMap(torrcOverrideConfigs);

        Path torrcPath = dataDir.resolve("torrc");
        var torrcFileGenerator = new TorrcFileGenerator(torrcPath,
                torrcConfigMap,
                transportConfig.getDirectoryAuthorities());
        torrcFileGenerator.generate();
    }
}
