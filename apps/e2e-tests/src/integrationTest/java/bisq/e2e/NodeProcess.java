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

package bisq.e2e;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * One node of a test network, started from the installed distribution of api-app or seed-node-app of a source tree.
 * All nodes use clearnet on 127.0.0.1, dev mode and the fake price node. Their data directories and logs stay after
 * the run, so that tools/compat/run-mixed-network.sh can scan the logs.
 */
final class NodeProcess implements AutoCloseable {
    enum Kind {
        SEED("seed-node-app", "256m"),
        API("api-app", "384m");

        private final String distribution;
        private final String maxHeap;

        Kind(String distribution, String maxHeap) {
            this.distribution = distribution;
            this.maxHeap = maxHeap;
        }
    }

    private final String name;
    private final Kind kind;
    private final String version;
    private final Path tree;
    private final Path dataDir;
    private final int p2pPort;
    private final int apiPort;
    private final List<Integer> seedPorts;
    private final String priceNodeUrl;
    private Process process;

    NodeProcess(String name,
                Kind kind,
                String version,
                Path tree,
                Path networkDir,
                List<Integer> seedPorts,
                String priceNodeUrl) throws IOException {
        this(name, kind, version, tree, networkDir.resolve(name), freePort(), kind == Kind.API ? freePort() : -1,
                seedPorts, priceNodeUrl);
    }

    private NodeProcess(String name,
                        Kind kind,
                        String version,
                        Path tree,
                        Path dataDir,
                        int p2pPort,
                        int apiPort,
                        List<Integer> seedPorts,
                        String priceNodeUrl) {
        this.name = name;
        this.kind = kind;
        this.version = version;
        this.tree = tree;
        this.dataDir = dataDir;
        this.p2pPort = p2pPort;
        this.apiPort = apiPort;
        this.seedPorts = seedPorts;
        this.priceNodeUrl = priceNodeUrl;
    }

    // A seed node which uses a given port, because the other nodes need the ports of the seeds before they start
    static NodeProcess seed(String name, String version, Path tree, Path networkDir, int port, List<Integer> seedPorts,
                            String priceNodeUrl) {
        return new NodeProcess(name, Kind.SEED, version, tree, networkDir.resolve(name), port, -1, seedPorts, priceNodeUrl);
    }

    /**
     * The same node with the distribution of another source tree, for an upgrade in place. It keeps the data
     * directory and the ports, so the node keeps its address and its peers reach it as before.
     */
    NodeProcess withTree(Path otherTree, String otherVersion) {
        return new NodeProcess(name, kind, otherVersion, otherTree, dataDir, p2pPort, apiPort, seedPorts, priceNodeUrl);
    }

    String getName() {
        return name;
    }

    String getVersion() {
        return version;
    }

    int getP2pPort() {
        return p2pPort;
    }

    Path getDataDir() {
        return dataDir;
    }

    Optional<ApiClient> apiClient() {
        return kind == Kind.API ? Optional.of(new ApiClient(apiPort)) : Optional.empty();
    }

    /**
     * Starts the node with the data directory which it has, or with a new one. A later start of a node of another
     * version on the same data directory is an upgrade in place.
     */
    void start() throws IOException {
        if (process != null && process.isAlive()) {
            throw new IllegalStateException(name + " runs already");
        }
        Files.createDirectories(dataDir);
        Files.writeString(dataDir.resolve("bisq.conf"), bisqConf(), StandardCharsets.UTF_8);
        Path startScript = tree.resolve("apps").resolve(kind.distribution)
                .resolve("build/install").resolve(kind.distribution).resolve("bin").resolve(kind.distribution);
        if (!Files.isExecutable(startScript)) {
            throw new IllegalStateException("Missing distribution " + startScript + ". Run tools/compat/run-mixed-network.sh.");
        }
        ProcessBuilder builder = new ProcessBuilder(startScript.toString(), "--data-dir=" + dataDir, "--app-name=" + name);
        builder.environment().put("JAVA_OPTS", String.join(" ", jvmOptions()));
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(dataDir.resolve("stdout.log").toFile()));
        process = builder.start();
    }

    // Waits until the node accepts connections: the API for an api-app node, the P2P port for a seed node
    void awaitReady(Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        int port = kind == Kind.API ? apiPort : p2pPort;
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException(name + " (" + version + ") stopped with exit code " + process.exitValue() +
                        ". See " + dataDir.resolve("stdout.log"));
            }
            if (isPortOpen(port) && (kind != Kind.API || new ApiClient(apiPort).isReady())) {
                return;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException(name + " (" + version + ") was not ready after " + timeout);
    }

    // Stops the node with its shutdown hook, as a user who quits the app does
    void stop() throws InterruptedException {
        if (process == null) {
            return;
        }
        process.destroy();
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void close() throws InterruptedException {
        stop();
    }

    private List<String> jvmOptions() {
        List<String> options = new ArrayList<>();
        options.add("-Xmx" + kind.maxHeap);
        options.add("-Dapplication.devMode=true");
        // Sellers need reputation. Without an oracle node, the network has no reputation data, so dev mode provides a
        // score for every profile. This hides the check of the reputation of a seller.
        options.add("-Dapplication.devModeReputationScore=50000");
        options.add("-Dapplication.network.supportedTransportTypes.0=CLEAR");
        options.add("-Dapplication.network.configByTransportType.clear.defaultNodePort=" + p2pPort);
        for (int i = 0; i < seedPorts.size(); i++) {
            options.add("-Dapplication.network.seedAddressByTransportType.clear." + i + "=127.0.0.1:" + seedPorts.get(i));
        }
        if (kind == Kind.API) {
            options.add("-Dapplication.api.accessTransportType=CLEAR");
            options.add("-Dapplication.api.server.restEnabled=true");
            options.add("-Dapplication.api.server.websocketEnabled=true");
            options.add("-Dapplication.api.server.bind.host=127.0.0.1");
            options.add("-Dapplication.api.server.bind.port=" + apiPort);
            options.add("-Dapplication.api.server.security.supportSessionHandling=false");
            options.add("-Dapplication.api.server.security.authorizationRequired=false");
        }
        return options;
    }

    // Lists of objects are easier to set in HOCON than with system properties. A bisq.conf in the data directory
    // overrides the bundled config in both versions.
    private String bisqConf() {
        String provider = "[{url=\"" + priceNodeUrl + "\", operator=\"e2e\", apiPath=\"getAllMarketPrices\"}]";
        return "application {\n" +
                "    bondedRoles.marketPrice.providers=" + provider + "\n" +
                "    bondedRoles.marketPrice.fallbackProviders=[]\n" +
                "    network.referenceTimeService.providers=" + provider + "\n" +
                "    network.referenceTimeService.fallbackProviders=[]\n" +
                "}\n";
    }

    private static boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    @Override
    public String toString() {
        return name + " (" + kind.distribution + " " + version + ", P2P port " + p2pPort +
                (kind == Kind.API ? ", API port " + apiPort : "") + ")";
    }
}
