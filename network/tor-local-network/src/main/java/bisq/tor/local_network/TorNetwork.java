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

package bisq.tor.local_network;

import bisq.common.util.NetworkUtils;
import bisq.tor.local_network.da.DirectoryAuthorityFactory;
import bisq.tor.local_network.torrc.ClientTorrcGenerator;
import bisq.tor.local_network.torrc.DirectoryAuthorityTorrcGenerator;
import bisq.tor.local_network.torrc.RelayTorrcGenerator;
import bisq.tor.local_network.torrc.TorrcFileGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class TorNetwork {

    private final Path rootDataDir;
    private final DirectoryAuthorityFactory dirAuthFactory = new DirectoryAuthorityFactory();
    private final Set<TorNode> directoryAuthorities = new HashSet<>();
    private final Set<TorNode> relays = new HashSet<>();
    private final Set<TorNode> clients = new HashSet<>();

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
                .controlPort(NetworkUtils.findFreeSystemPort())
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

                .controlPort(NetworkUtils.findFreeSystemPort())
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

                .controlPort(NetworkUtils.findFreeSystemPort())
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())

                .build();
        clients.add(firstClient);
        return this;
    }

    public void start() throws IOException {
        generateTorrcFiles();
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

    private void generateTorrcFiles() throws IOException {
        Set<TorNode> allDAs = dirAuthFactory.getAllDirectoryAuthorities();
        for (TorNode da : allDAs) {
            var torDaTorrcGenerator = new DirectoryAuthorityTorrcGenerator(da);
            var torrcFileGenerator = new TorrcFileGenerator(torDaTorrcGenerator, allDAs);
            generateTorrc(da, torrcFileGenerator);
        }

        for (TorNode relay : relays) {
            var relayTorrcGenerator = new RelayTorrcGenerator(relay);
            var torrcFileGenerator = new TorrcFileGenerator(relayTorrcGenerator, allDAs);
            generateTorrc(relay, torrcFileGenerator);
        }

        for (TorNode client : clients) {
            var clientTorrcGenerator = new ClientTorrcGenerator(client);
            var torrcFileGenerator = new TorrcFileGenerator(clientTorrcGenerator, allDAs);
            generateTorrc(client, torrcFileGenerator);
        }
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
        String absoluteTorrcPathAsString = torNode.getTorrcPath()
                .toAbsolutePath()
                .toString();
        var processBuilder = new ProcessBuilder("tor", "-f", absoluteTorrcPathAsString);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        return processBuilder.start();
    }

    private void generateTorrc(TorNode torNode, TorrcFileGenerator torrcFileGenerator) throws IOException {
        if (torNode.getTorrcPath().toFile().exists()) {
            return;
        }
        torrcFileGenerator.generate();
    }
}
