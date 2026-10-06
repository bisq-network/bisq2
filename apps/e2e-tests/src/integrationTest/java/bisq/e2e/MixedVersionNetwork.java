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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

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
    private final List<NodeProcess> extraNodes = new ArrayList<>();
    private NodeProcess upgradedOldNode;

    MixedVersionNetwork(String name) throws Exception {
        dir = E2eSettings.workDir().resolve(name);
        deleteRecursively(dir);
        Files.createDirectories(dir);
        priceServer = new FakePriceServer();

        List<Integer> seedPorts = List.of(NodeProcess.freePort(), NodeProcess.freePort());
        oldSeed = NodeProcess.seed("seed-old", oldVersion, E2eSettings.oldTree(), dir, seedPorts.get(0), seedPorts, priceServer.getUrl());
        newSeed = NodeProcess.seed("seed-new", newVersion, E2eSettings.newTree(), dir, seedPorts.get(1), seedPorts, priceServer.getUrl());
        oldNode = new NodeProcess("node-old", NodeProcess.Kind.API, oldVersion, E2eSettings.oldTree(), dir, seedPorts, priceServer.getUrl());
        newNode = new NodeProcess("node-new", NodeProcess.Kind.API, newVersion, E2eSettings.newTree(), dir, seedPorts, priceServer.getUrl());
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

    /**
     * Starts one more api-app node, which knows only the given seed nodes, for example a node which joins the network
     * later. It gets a user identity like the other api-app nodes.
     */
    ApiClient startExtraApiNode(String name, boolean ofNewVersion, List<Integer> seedPorts) throws Exception {
        NodeProcess node = new NodeProcess(name, NodeProcess.Kind.API, ofNewVersion ? newVersion : oldVersion,
                ofNewVersion ? E2eSettings.newTree() : E2eSettings.oldTree(), dir, seedPorts, priceServer.getUrl());
        extraNodes.add(node);
        node.start();
        node.awaitReady(START_TIMEOUT);
        ApiClient api = node.apiClient().orElseThrow();
        api.createUserIdentity("e2e-" + name);
        api.acceptTermsAndConditions();
        return api;
    }

    /** Stops the node of the old version, as a user who quits the app before an update does. */
    void stopOldNode() throws InterruptedException {
        oldNode.stop();
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
        if (exitCode != 0) {
            throw new AssertionError("The logs of the nodes contain unexpected lines (exit code " + exitCode + "). " +
                    "Report " + report + ":\n" + Files.readString(report, StandardCharsets.UTF_8));
        }
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
