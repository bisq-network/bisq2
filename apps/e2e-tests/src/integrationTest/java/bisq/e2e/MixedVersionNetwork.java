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

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static bisq.e2e.ApiClient.JSON;

/**
 * A test network on 127.0.0.1 with nodes of two versions: a seed node of each version, and an api-app node of each
 * version. All nodes know both seed nodes. Each test class starts its own network in a directory of its own below
 * bisq.e2e.workDir. At the end, the network stops its nodes and scans their logs with scripts/log_anomaly_scan.py.
 */
final class MixedVersionNetwork implements AutoCloseable {
    static final Duration START_TIMEOUT = Duration.ofMinutes(3);

    final String oldVersion = E2eSettings.oldVersion();
    final String newVersion = "checkout";
    final Path dir;
    final FakePriceServer priceServer;
    final NodeProcess oldSeed;
    final NodeProcess newSeed;
    final NodeProcess oldNode;
    final NodeProcess newNode;
    final ApiClient oldApi;
    final ApiClient newApi;
    // The time which the clocks of all nodes and of the fake price node run ahead of the real time
    final Duration clockOffset;
    private final List<NodeProcess> extraNodes = new ArrayList<>();
    private final List<ExpectedLogFinding> expectedLogFindings = new ArrayList<>();
    private NodeProcess upgradedOldNode;

    private record ExpectedLogFinding(String nodeName, String ruleId, String textPart) {
    }

    MixedVersionNetwork(String name) throws Exception {
        this(name, Duration.ZERO);
    }

    /**
     * A network whose nodes have a fake clock, which runs ahead of the real time by the given offset. It needs
     * libfaketime (see E2eSettings.libfaketime).
     */
    MixedVersionNetwork(String name, Duration clockOffset) throws Exception {
        this.clockOffset = clockOffset;
        dir = E2eSettings.workDir().resolve(name);
        deleteRecursively(dir);
        Files.createDirectories(dir);
        priceServer = new FakePriceServer(clockOffset);

        List<Integer> seedPorts = List.of(NodeProcess.freePort(), NodeProcess.freePort());
        oldSeed = NodeProcess.seed("seed-old", oldVersion, E2eSettings.oldTree(), dir, seedPorts.get(0), seedPorts,
                priceServer.getUrl(), clockOffset);
        newSeed = NodeProcess.seed("seed-new", newVersion, E2eSettings.newTree(), dir, seedPorts.get(1), seedPorts,
                priceServer.getUrl(), clockOffset);
        oldNode = new NodeProcess("node-old", NodeProcess.Kind.API, oldVersion, E2eSettings.oldTree(), dir, seedPorts,
                priceServer.getUrl(), clockOffset);
        newNode = new NodeProcess("node-new", NodeProcess.Kind.API, newVersion, E2eSettings.newTree(), dir, seedPorts,
                priceServer.getUrl(), clockOffset);
        oldApi = oldNode.apiClient().orElseThrow();
        newApi = newNode.apiClient().orElseThrow();

        try {
            for (NodeProcess node : List.of(oldSeed, newSeed)) {
                node.start();
            }
            for (NodeProcess node : List.of(oldSeed, newSeed)) {
                node.awaitReady(START_TIMEOUT);
            }
            for (NodeProcess node : List.of(oldNode, newNode)) {
                node.start();
            }
            for (NodeProcess node : List.of(oldNode, newNode)) {
                node.awaitReady(START_TIMEOUT);
            }
            oldApi.createUserIdentity("e2e-old");
            newApi.createUserIdentity("e2e-new");
            oldApi.acceptTermsAndConditions();
            newApi.acceptTermsAndConditions();
        } catch (Exception e) {
            // The failure of the start is the cause. A failed scan of the logs is added to it.
            try {
                close();
            } catch (Throwable closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    /** The current time of the clocks of the nodes. */
    long nodeTimeMillis() {
        return System.currentTimeMillis() + clockOffset.toMillis();
    }

    /**
     * Starts one more api-app node, which knows only the given seed nodes, for example a node which joins the network
     * later. It gets a user identity like the other api-app nodes.
     */
    ApiClient startExtraApiNode(String name, boolean ofNewVersion, List<Integer> seedPorts) throws Exception {
        NodeProcess node = newExtraApiNode(name, ofNewVersion, seedPorts);
        node.start();
        node.awaitReady(START_TIMEOUT);
        ApiClient api = node.apiClient().orElseThrow();
        api.createUserIdentity("e2e-" + name);
        api.acceptTermsAndConditions();
        return api;
    }

    /**
     * Starts one more api-app node with the security settings of its version, as a user of Bisq Connect runs it. The
     * returned client pairs with the node through the pairing code of the node, as the app does with its QR code, and
     * then gets a user identity like the other api-app nodes.
     */
    ApiClient startExtraApiNodeForPairedClient(String name, boolean ofNewVersion, List<Integer> seedPorts,
                                               String clientName) throws Exception {
        NodeProcess node = newExtraApiNode(name, ofNewVersion, seedPorts).useDefaultApiSecurity();
        node.start();
        node.awaitReady(START_TIMEOUT);
        String[] pairingCodeId = new String[1];
        await("the node writes its pairing code", START_TIMEOUT, () -> {
            try {
                pairingCodeId[0] = node.pairingCodeId().orElse(null);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return pairingCodeId[0] != null;
        });
        ApiClient api = node.apiClient().orElseThrow();
        api.pair(pairingCodeId[0], clientName);
        api.createUserIdentity("e2e-" + name);
        api.acceptTermsAndConditions();
        return api;
    }

    private NodeProcess newExtraApiNode(String name, boolean ofNewVersion, List<Integer> seedPorts) throws IOException {
        NodeProcess node = new NodeProcess(name, NodeProcess.Kind.API, ofNewVersion ? newVersion : oldVersion,
                ofNewVersion ? E2eSettings.newTree() : E2eSettings.oldTree(), dir, seedPorts, priceServer.getUrl(),
                clockOffset);
        extraNodes.add(node);
        return node;
    }

    /** Stops an extra node and starts it again with its version and the given JVM options. */
    void restartExtraNode(String name, List<String> extraJvmOptions) throws Exception {
        NodeProcess node = extraNode(name);
        node.stop();
        node.setExtraJvmOptions(extraJvmOptions);
        node.start();
        node.awaitReady(START_TIMEOUT);
    }

    private NodeProcess extraNode(String name) {
        return extraNodes.stream()
                .filter(node -> node.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Stops an extra node of the old version and starts the distribution of this checkout on its data directory, as a
     * user who upgrades. The node keeps its ports, so its ApiClient reaches the upgraded node.
     */
    void upgradeExtraNode(String name) throws Exception {
        NodeProcess nodeOfOldVersion = extraNode(name);
        nodeOfOldVersion.stop();
        NodeProcess upgradedNode = nodeOfOldVersion.withTree(E2eSettings.newTree(), newVersion);
        extraNodes.set(extraNodes.indexOf(nodeOfOldVersion), upgradedNode);
        upgradedNode.start();
        upgradedNode.awaitReady(START_TIMEOUT);
    }

    /** Stops and starts the node of this checkout on its data directory, as a user who restarts the app. */
    void restartNewNode() throws Exception {
        stopNewNode();
        startNewNode();
    }

    /** Stops the node of this checkout, as a user who quits the app. */
    void stopNewNode() throws InterruptedException {
        newNode.stop();
    }

    /** Starts the stopped node of this checkout again on its data directory. */
    void startNewNode() throws Exception {
        newNode.start();
        newNode.awaitReady(START_TIMEOUT);
    }

    /** Stops the node of the old version, as a user who quits the app before an update does. */
    void stopOldNode() throws InterruptedException {
        oldNode.stop();
    }

    /** Starts the stopped node of the old version again on its data directory, with the old version. */
    void startOldNode() throws Exception {
        oldNode.start();
        oldNode.awaitReady(START_TIMEOUT);
    }

    /**
     * Starts the distribution of this checkout on the data directory of the stopped node of the old version: an upgrade
     * in place. The node keeps its ports, so oldApi reaches the upgraded node.
     */
    void startOldNodeWithNewVersion() throws Exception {
        upgradedOldNode = oldNode.withTree(E2eSettings.newTree(), newVersion);
        upgradedOldNode.start();
        upgradedOldNode.awaitReady(START_TIMEOUT);
    }

    /**
     * Declares a finding of the log scan which the test causes on purpose, for example the log lines of a take which
     * the maker rejects. The scan then accepts the findings of this rule on this node if each of their lines contains
     * the text. All other findings still fail the test class.
     */
    void expectLogFinding(String nodeName, String ruleId, String text) {
        expectedLogFindings.add(new ExpectedLogFinding(nodeName, ruleId, text));
    }

    /** The lines of the log of the node since its last start. */
    List<String> logOfLastSession(String nodeName) throws IOException {
        String log = new String(Files.readAllBytes(dir.resolve(nodeName).resolve("bisq.log")), StandardCharsets.UTF_8);
        List<String> lines = List.of(log.split("\n"));
        int lastStart = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("ApplicationService: Version: v")) {
                lastStart = i;
            }
        }
        return lines.subList(lastStart, lines.size());
    }

    /** Waits until the condition holds. The nodes deliver data asynchronously. */
    static void await(String description, Duration timeout, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        RuntimeException lastError = null;
        while (System.nanoTime() < deadline) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
                lastError = null;
            } catch (RuntimeException e) {
                lastError = e;
            }
            Thread.sleep(1000);
        }
        throw new AssertionError("Timeout after " + timeout + ": " + description, lastError);
    }

    /**
     * Stops all nodes, then scans their logs. A finding of level warning or error, which the allowlist of the scan does
     * not remove, fails the test class: it is a failure which a tester would not see on screen.
     */
    @Override
    public void close() throws Exception {
        List<NodeProcess> nodesToStop = new ArrayList<>(extraNodes);
        nodesToStop.add(newNode);
        if (upgradedOldNode != null) {
            nodesToStop.add(upgradedOldNode);
        }
        nodesToStop.addAll(List.of(oldNode, newSeed, oldSeed));
        for (NodeProcess node : nodesToStop) {
            if (node != null) {
                node.stop();
            }
        }
        priceServer.close();
        scanLogs();
    }

    private void scanLogs() throws IOException, InterruptedException {
        Path script = E2eSettings.newTree().resolve("scripts/log_anomaly_scan.py");
        Path report = dir.resolve("log-anomaly-report.txt");
        Process process = new ProcessBuilder("python3", script.toString(), dir.toString())
                .redirectErrorStream(true)
                .redirectOutput(report.toFile())
                .start();
        int exitCode = process.waitFor();
        boolean onlyExpectedFindings = exitCode == 1 && !expectedLogFindings.isEmpty() &&
                unexpectedLogFindings(script).isEmpty();
        if (exitCode == 0 || onlyExpectedFindings) {
            return;
        }
        throw new AssertionError("The logs of the nodes contain unexpected lines (exit code " + exitCode + "). " +
                "Report " + report + ":\n" + Files.readString(report, StandardCharsets.UTF_8));
    }

    // The findings of the scan which no test declared. The JSON report lists every line of a finding.
    private List<String> unexpectedLogFindings(Path script) throws IOException, InterruptedException {
        Path jsonReport = dir.resolve("log-anomaly-report.json");
        Process process = new ProcessBuilder("python3", script.toString(), "--json", "--max-examples", "100000",
                dir.toString())
                .redirectOutput(jsonReport.toFile())
                .start();
        process.waitFor();
        List<String> unexpected = new ArrayList<>();
        for (JsonNode node : JSON.readTree(jsonReport.toFile()).path("nodes")) {
            String nodeName = node.path("name").asText();
            for (JsonNode session : node.path("sessions")) {
                for (JsonNode finding : session.path("findings")) {
                    String ruleId = finding.path("ruleId").asText();
                    boolean isExpected = finding.path("severity").asText().equals("notice") ||
                            expectedLogFindings.stream().anyMatch(expected -> expected.nodeName().equals(nodeName) &&
                                    expected.ruleId().equals(ruleId) &&
                                    linesOf(finding).stream().allMatch(line -> line.contains(expected.textPart())));
                    if (!isExpected) {
                        unexpected.add(nodeName + ": " + ruleId);
                    }
                }
            }
        }
        return unexpected;
    }

    private static List<String> linesOf(JsonNode finding) {
        List<String> lines = new ArrayList<>();
        finding.path("examples").forEach(example -> lines.add(example.path("text").asText()));
        return lines;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path file : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        }
    }
}
