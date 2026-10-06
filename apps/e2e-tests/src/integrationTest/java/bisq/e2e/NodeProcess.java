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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
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

    // The main class and the classpath of a program which runs as a node with the JVM options of its kind
    record Program(String mainClass, String classpath) {
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
    // The time which the clock of the node runs ahead of the real time. Zero means the real clock.
    private final Duration clockOffset;
    // Whether an api-app node keeps the security settings of its version, which require a paired client. The harness
    // otherwise turns off authorization and sessions.
    private boolean defaultApiSecurity;
    // JVM options for the next start, for example a shorter time to live of sessions
    private final List<String> extraJvmOptions = new ArrayList<>();
    // A program which runs instead of the start script of the distribution, for example the authorized data publisher
    private Optional<Program> program = Optional.empty();
    private Process process;

    NodeProcess(String name,
                Kind kind,
                String version,
                Path tree,
                Path networkDir,
                List<Integer> seedPorts,
                String priceNodeUrl,
                Duration clockOffset) throws IOException {
        this(name, kind, version, tree, networkDir.resolve(name), freePort(), kind == Kind.API ? freePort() : -1,
                seedPorts, priceNodeUrl, clockOffset);
    }

    private NodeProcess(String name,
                        Kind kind,
                        String version,
                        Path tree,
                        Path dataDir,
                        int p2pPort,
                        int apiPort,
                        List<Integer> seedPorts,
                        String priceNodeUrl,
                        Duration clockOffset) {
        this.name = name;
        this.kind = kind;
        this.version = version;
        this.tree = tree;
        this.dataDir = dataDir;
        this.p2pPort = p2pPort;
        this.apiPort = apiPort;
        this.seedPorts = seedPorts;
        this.priceNodeUrl = priceNodeUrl;
        this.clockOffset = clockOffset;
    }

    // A seed node which uses a given port, because the other nodes need the ports of the seeds before they start
    static NodeProcess seed(String name, String version, Path tree, Path networkDir, int port, List<Integer> seedPorts,
                            String priceNodeUrl, Duration clockOffset) {
        return new NodeProcess(name, Kind.SEED, version, tree, networkDir.resolve(name), port, -1, seedPorts, priceNodeUrl,
                clockOffset);
    }

    /**
     * The same node with the distribution of another source tree, for an upgrade in place. It keeps the data
     * directory and the ports, so the node keeps its address and its peers reach it as before.
     */
    NodeProcess withTree(Path otherTree, String otherVersion) {
        NodeProcess node = new NodeProcess(name, kind, otherVersion, otherTree, dataDir, p2pPort, apiPort, seedPorts,
                priceNodeUrl, clockOffset);
        node.defaultApiSecurity = defaultApiSecurity;
        node.extraJvmOptions.addAll(extraJvmOptions);
        return node;
    }

    /** Keeps the security settings of the version of this api-app node, so a client must pair with it. */
    NodeProcess useDefaultApiSecurity() {
        defaultApiSecurity = true;
        return this;
    }

    void setExtraJvmOptions(List<String> options) {
        extraJvmOptions.clear();
        extraJvmOptions.addAll(options);
    }

    /** Runs the given program instead of the start script of the distribution. */
    NodeProcess runProgram(Program program) {
        this.program = Optional.of(program);
        return this;
    }

    /** Writes a line to the standard input of the node, which a program such as the authorized data publisher reads. */
    void sendCommand(String command) throws IOException {
        if (process == null || !process.isAlive()) {
            throw new IllegalStateException(name + " does not run");
        }
        OutputStream input = process.getOutputStream();
        input.write((command + "\n").getBytes(StandardCharsets.UTF_8));
        input.flush();
    }

    /**
     * The ID of the pairing code which the node writes to pairing_qr_code.txt. The first line of the file is the QR code
     * in Base64 (PairingQrCodeEncoder of both versions): a version byte, then the pairing code with a length of 16 bits,
     * which has a version byte and then the ID as text with a length of 16 bits.
     */
    Optional<String> pairingCodeId() throws IOException {
        Path file = dataDir.resolve("pairing_qr_code.txt");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        String qrCode = Files.readString(file, StandardCharsets.UTF_8).lines().findFirst().orElse("").trim();
        if (qrCode.isEmpty()) {
            return Optional.empty();
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(qrCode)));
        in.readByte();
        byte[] pairingCode = in.readNBytes(in.readUnsignedShort());
        DataInputStream code = new DataInputStream(new ByteArrayInputStream(pairingCode));
        code.readByte();
        return Optional.of(new String(code.readNBytes(code.readUnsignedShort()), StandardCharsets.UTF_8));
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
        ProcessBuilder builder = new ProcessBuilder(command());
        if (program.isEmpty()) {
            // The start script of the distribution passes them to the JVM
            builder.environment().put("JAVA_OPTS", String.join(" ", jvmOptions()));
        }
        if (!clockOffset.isZero()) {
            // libfaketime moves the clock of the whole process, also System.currentTimeMillis() of the JVM. The offset
            // is relative to the real time, so all nodes with the same offset show the same time. The monotonic clock
            // stays real, because the JVM uses it for timers.
            Path libfaketime = E2eSettings.libfaketime()
                    .orElseThrow(() -> new IllegalStateException("A fake clock needs libfaketime. See tools/compat/README.md."));
            builder.environment().put("LD_PRELOAD", libfaketime.toString());
            builder.environment().put("FAKETIME", (clockOffset.isNegative() ? "" : "+") + clockOffset.toSeconds());
            builder.environment().put("FAKETIME_DONT_FAKE_MONOTONIC", "1");
        }
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
            if (isPortOpen(port) && (kind != Kind.API || isApiReady())) {
                return;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException(name + " (" + version + ") was not ready after " + timeout);
    }

    // A node which requires a paired client answers only the routes of pairing and sessions without it
    private boolean isApiReady() {
        ApiClient api = new ApiClient(apiPort);
        return defaultApiSecurity ? api.isServing() : api.isReady();
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

    private List<String> command() {
        List<String> command = new ArrayList<>();
        if (program.isPresent()) {
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            command.addAll(jvmOptions());
            command.addAll(List.of("-cp", program.get().classpath(), program.get().mainClass()));
        } else {
            Path startScript = tree.resolve("apps").resolve(kind.distribution)
                    .resolve("build/install").resolve(kind.distribution).resolve("bin").resolve(kind.distribution);
            if (!Files.isExecutable(startScript)) {
                throw new IllegalStateException("Missing distribution " + startScript +
                        ". Run tools/compat/run-mixed-network.sh.");
            }
            command.add(startScript.toString());
        }
        command.add("--data-dir=" + dataDir);
        command.add("--app-name=" + name);
        return command;
    }

    private List<String> jvmOptions() {
        List<String> options = new ArrayList<>();
        options.add("-Xmx" + kind.maxHeap);
        options.add("-Dapplication.devMode=true");
        // Sellers need reputation. Without an oracle node, the network has no reputation data, so dev mode provides a
        // score for every profile. This hides the check of the reputation of a seller.
        options.add("-Dapplication.devModeReputationScore=50000");
        // The harness starts each node on a data directory of its own, and stops it before it starts it again. v2.1.13
        // binds its instance lock to a port which it derives from the path of the data directory. A closed connection
        // can still hold that port (TIME_WAIT), and then the node does not start.
        options.add("-Dapplication.checkInstanceLock=false");
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
            if (!defaultApiSecurity) {
                options.add("-Dapplication.api.server.security.supportSessionHandling=false");
                options.add("-Dapplication.api.server.security.authorizationRequired=false");
            }
        }
        options.addAll(extraJvmOptions);
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
