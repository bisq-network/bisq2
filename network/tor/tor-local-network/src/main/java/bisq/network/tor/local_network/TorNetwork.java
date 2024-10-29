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

package bisq.network.tor.local_network;

import bisq.common.util.NetworkUtils;
import bisq.network.tor.common.torrc.DirectoryAuthority;
import bisq.network.tor.common.torrc.TorrcConfigGenerator;
import bisq.network.tor.common.torrc.TorrcFileGenerator;
import bisq.network.tor.installer.TorInstaller;
import bisq.network.tor.local_network.da.DirectoryAuthorityFactory;
import bisq.network.tor.local_network.torrc.TestNetworkTorrcGeneratorFactory;
import bisq.network.tor.process.LdPreload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TorNetwork {

    private final Path rootDataDir;
    private final DirectoryAuthorityFactory dirAuthFactory = new DirectoryAuthorityFactory();
    private final Set<TorNode> directoryAuthorities = new HashSet<>();
    private final Set<TorNode> relays = new HashSet<>();
    private final Set<TorNode> clients = new HashSet<>();

    // TODO Never queried
    private final Set<Process> allTorProcesses = new HashSet<>();

    private int dirAuthIndex;
    private int relayIndex;
    private int clientIndex;

    public TorNetwork(Path rootDataDir) {
        this.rootDataDir = rootDataDir;
    }

    public TorNetwork addDirAuth(String passphrase) throws IOException, InterruptedException {
        String nickname = "da" + dirAuthIndex++;

        Path nodeDataDir = rootDataDir.resolve(nickname);
        createDataDirIfNotPresent(nodeDataDir);

        var dirAuth = TorNode.builder()
                .type(TorNode.Type.DIRECTORY_AUTHORITY)
                .nickname(nickname)
                .dataDir(nodeDataDir)
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();
        dirAuthFactory.createDirectoryAuthority(dirAuth, passphrase);
        directoryAuthorities.add(dirAuth);

        return this;
    }

    public TorNetwork addRelay() {
        String nickname = "relay" + relayIndex++;

        Path nodeDataDir = rootDataDir.resolve(nickname);
        createDataDirIfNotPresent(nodeDataDir);

        TorNode firstRelay = TorNode.builder()
                .type(TorNode.Type.RELAY)
                .nickname(nickname)
                .dataDir(nodeDataDir)

                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())

                .build();
        relays.add(firstRelay);
        return this;
    }

    public TorNetwork addClient() {
        String nickname = "client" + clientIndex++;

        Path nodeDataDir = rootDataDir.resolve(nickname);
        createDataDirIfNotPresent(nodeDataDir);

        TorNode firstClient = TorNode.builder()
                .type(TorNode.Type.CLIENT)
                .nickname(nickname)
                .dataDir(nodeDataDir)

                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())

                .build();
        clients.add(firstClient);
        return this;
    }

    public void start() throws IOException {
        generateTorrcFiles();
        installTor();
        startProcesses();
    }

    private void createDataDirIfNotPresent(Path nodeDataDirPath) {
        File nodeDataDirFile = nodeDataDirPath.toFile();
        if (nodeDataDirFile.exists()) {
            return;
        }

        boolean isSuccess = nodeDataDirPath.toFile().mkdir();
        if (!isSuccess) {
            throw new IllegalStateException("Couldn't create data directory: " + nodeDataDirPath.toAbsolutePath());
        }
    }

    private void generateTorrcFiles() {
        Set<TorNode> allDAs = dirAuthFactory.getAllDirectoryAuthorities();
        for (TorNode da : allDAs) {
            TorrcConfigGenerator torDaTorrcGenerator = TestNetworkTorrcGeneratorFactory.directoryTorrcGenerator(da);
            Map<String, String> torrcConfigs = torDaTorrcGenerator.generate();
            generateTorrc(da, torrcConfigs, allDAs);
        }

        for (TorNode relay : relays) {
            TorrcConfigGenerator relayTorrcGenerator = TestNetworkTorrcGeneratorFactory.relayTorrcGenerator(relay);
            Map<String, String> torrcConfigs = relayTorrcGenerator.generate();
            generateTorrc(relay, torrcConfigs, allDAs);
        }

        for (TorNode client : clients) {
            TorrcConfigGenerator clientTorrcGenerator = TestNetworkTorrcGeneratorFactory.clientTorrcGenerator(client);
            Map<String, String> torrcConfigs = clientTorrcGenerator.generate();
            generateTorrc(client, torrcConfigs, allDAs);
        }
    }

    private void installTor() {
        Path torBinaryDirPath = rootDataDir.resolve("tor_binary");
        var torInstaller = new TorInstaller(torBinaryDirPath);
        torInstaller.installIfNotUpToDate();
    }

    private void startProcesses() throws IOException {
        for (TorNode directoryAuthority : directoryAuthorities) {
            Process process = createAndStartTorProcess(directoryAuthority);
            allTorProcesses.add(process);
        }

        for (TorNode relay : relays) {
            Process process = createAndStartTorProcess(relay);
            allTorProcesses.add(process);
        }

        for (TorNode client : clients) {
            Process process = createAndStartTorProcess(client);
            allTorProcesses.add(process);
        }
    }

    private Process createAndStartTorProcess(TorNode torNode) throws IOException {
        String absoluteTorBinaryPath = rootDataDir.resolve("tor_binary")
                .resolve("tor")
                .toAbsolutePath()
                .toString();
        String absoluteTorrcPathAsString = torNode.getTorrcPath()
                .toAbsolutePath()
                .toString();
        var processBuilder = new ProcessBuilder(absoluteTorBinaryPath, "-f", absoluteTorrcPathAsString);

        Map<String, String> environment = processBuilder.environment();
        Path torDataDirPath = rootDataDir.resolve("tor_binary");
        environment.put("LD_PRELOAD", LdPreload.computeLdPreloadVariable(torDataDirPath));

        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        return processBuilder.start();
    }

    private void generateTorrc(TorNode torNode, Map<String, String> torrcConfigs, Set<TorNode> allDAs) {
        if (torNode.getTorrcPath().toFile().exists()) {
            return;
        }

        Set<DirectoryAuthority> directoryAuthorities = allDAs.stream()
                .map(TorNode::toDirectoryAuthority)
                .collect(Collectors.toSet());

        var torrcFileGenerator = new TorrcFileGenerator(torNode.getTorrcPath(), torrcConfigs, directoryAuthorities);
        torrcFileGenerator.generate();
    }
}
