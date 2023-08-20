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
import bisq.common.util.NetworkUtils;
import bisq.tor.controller.NativeTorController;
import bisq.tor.installer.TorInstallationFiles;
import bisq.tor.installer.TorInstaller;
import bisq.tor.onionservice.CreateOnionServiceResponse;
import bisq.tor.onionservice.OnionServicePublishService;
import bisq.tor.process.NativeTorProcess;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class TorService implements Service {
    private static final int RANDOM_PORT = 0;

    private final TorTransportConfig transportConfig;
    private final Path torDataDirPath;
    private final NativeTorController nativeTorController = new NativeTorController();

    private Optional<NativeTorProcess> torProcess = Optional.empty();
    private Optional<Integer> socksPort = Optional.empty();
    private Optional<TorSocksProxyFactory> torSocksProxyFactory = Optional.empty();
    private Optional<OnionAddress> onionAddress = Optional.empty();

    public TorService(TorTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
        this.torDataDirPath = transportConfig.getDataDir();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        installTorIfNotUpToDate();

        int controlPort = NetworkUtils.findFreeSystemPort();
        PasswordDigest hashedControlPassword = PasswordDigest.generateDigest();
        createTorrcConfigFile(torDataDirPath, controlPort, hashedControlPassword);

        Path torBinaryPath = torDataDirPath.resolve("tor");
        Path torrcPath = torDataDirPath.resolve("torrc");
        var nativeTorProcess = new NativeTorProcess(torBinaryPath, torrcPath);
        torProcess = Optional.of(nativeTorProcess);

        File debugLogFile = torDataDirPath.resolve("debug.log").toFile();
        if (debugLogFile.exists()) {
            boolean isSuccess = debugLogFile.delete();
            if (!isSuccess) {
                throw new IllegalStateException("Can't delete old debug.log file");
            }
        }

        nativeTorProcess.start();
        nativeTorProcess.waitUntilControlPortReady();

        nativeTorController.connect(controlPort, hashedControlPassword);
        nativeTorController.bindTorToConnection();

        nativeTorController.enableTorNetworking();
        nativeTorController.waitUntilBootstrapped();

        int socksPort = this.socksPort.orElseThrow();
        torSocksProxyFactory = Optional.of(new TorSocksProxyFactory(socksPort));

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        nativeTorController.shutdown();
        torProcess.ifPresent(NativeTorProcess::waitUntilExited);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<CreateOnionServiceResponse> createOnionService(int port, String nodeId) {
        log.info("Start hidden service with port {} and nodeId {}", port, nodeId);
        long ts = System.currentTimeMillis();
        try {
            @SuppressWarnings("resource") ServerSocket localServerSocket = new ServerSocket(RANDOM_PORT);
            int localPort = localServerSocket.getLocalPort();

            var onionServicePublishService = new OnionServicePublishService(nativeTorController, torDataDirPath);
            return onionServicePublishService.initialize(nodeId, port, localPort)
                    .thenApply(onionAddress -> {
                                log.info("Tor hidden service Ready. Took {} ms. Onion address={}; nodeId={}",
                                        System.currentTimeMillis() - ts, onionAddress, nodeId);
                                this.onionAddress = Optional.of(onionAddress);
                                return new CreateOnionServiceResponse(nodeId, localServerSocket, onionAddress);
                            }
                    );

        } catch (IOException e) {
            log.error("Can't create onion service", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public boolean isOnionServiceOnline(String onionUrl) {
        return nativeTorController.isHiddenServiceAvailable(onionUrl);
    }

    public Optional<String> getHostName(String serverId) {
        return onionAddress.map(OnionAddress::getHost);
    }

    public Socket getSocket(String streamId) throws IOException {
        TorSocksProxyFactory socksProxyFactory = torSocksProxyFactory.orElseThrow();
        return socksProxyFactory.getSocket(streamId);
    }

    public Socks5Proxy getSocks5Proxy(String streamId) throws IOException {
        TorSocksProxyFactory socksProxyFactory = torSocksProxyFactory.orElseThrow();
        return socksProxyFactory.getSocks5Proxy(streamId);
    }

    private void installTorIfNotUpToDate() {
        Path torDataDirPath = transportConfig.getDataDir();
        var torInstallationFiles = new TorInstallationFiles(torDataDirPath);

        var torInstaller = new TorInstaller(torInstallationFiles);
        try {
            torInstaller.installIfNotUpToDate();
        } catch (IOException e) {
            throw new CannotInstallBundledTor(e);
        }
    }

    private void createTorrcConfigFile(Path dataDir, int controlPort, PasswordDigest hashedControlPassword) {
        int socksPort = NetworkUtils.findFreeSystemPort();
        this.socksPort = Optional.of(socksPort);

        TorrcClientConfigFactory torrcClientConfigFactory = TorrcClientConfigFactory.builder()
                .isTestNetwork(transportConfig.isTestNetwork())
                .dataDir(dataDir)
                .controlPort(controlPort)
                .socksPort(socksPort)
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
