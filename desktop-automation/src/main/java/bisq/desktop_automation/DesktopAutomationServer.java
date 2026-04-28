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

package bisq.desktop_automation;

import bisq.desktop.automation.DesktopAutomationSelector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class DesktopAutomationServer {
    private static final String AUTH_HEADER = "X-Bisq-Automation-Token";
    private static final int PROTOCOL_VERSION = 2;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public enum ReadinessState {
        BOOTING,
        READY,
        SHUTTING_DOWN
    }

    private final Stage stage;
    private final HttpServer server;
    private final ExecutorService executor;
    private final String token;
    private final Path artifactsDir;
    private final long fxTimeoutMs;
    private final double defaultWidth;
    private final double defaultHeight;
    private final AtomicReference<ReadinessState> readinessState = new AtomicReference<>(ReadinessState.BOOTING);

    private DesktopAutomationServer(Stage stage,
                                    HttpServer server,
                                    ExecutorService executor,
                                    String token,
                                    Path artifactsDir,
                                    long fxTimeoutMs,
                                    double defaultWidth,
                                    double defaultHeight) {
        this.stage = stage;
        this.server = server;
        this.executor = executor;
        this.token = token;
        this.artifactsDir = artifactsDir;
        this.fxTimeoutMs = fxTimeoutMs;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
    }

    public static DesktopAutomationServer start(Stage stage, DesktopAutomationConfig config) {
        String bindHost = config.bindHost();
        if (!isLoopbackHost(bindHost)) {
            throw new IllegalArgumentException("Desktop automation server must bind to loopback only. Invalid host: " + bindHost);
        }
        int bindPort = config.bindPort();
        long fxTimeoutMs = config.fxTimeoutMs();
        double defaultWidth = config.defaultWidth();
        double defaultHeight = config.defaultHeight();
        String token = config.token();
        Path artifactsDir = Path.of(config.artifactsDir());

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(bindHost, bindPort), 0);
            ExecutorService executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setName("desktop-automation-http");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);
            DesktopAutomationServer desktopAutomationServer = new DesktopAutomationServer(stage, server, executor,
                    token, artifactsDir, fxTimeoutMs, defaultWidth, defaultHeight);
            desktopAutomationServer.start();
            return desktopAutomationServer;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start DesktopAutomationServer", e);
        }
    }

    public void stop() {
        readinessState.set(ReadinessState.SHUTTING_DOWN);
        try {
            server.stop(0);
        } catch (Exception e) {
            log.warn("Error while stopping DesktopAutomationServer", e);
        }
        executor.shutdownNow();
    }

    private void start() throws Exception {
        readinessState.set(ReadinessState.BOOTING);
        Files.createDirectories(artifactsDir);
        applyDefaultWindowSize();

        server.createContext("/health", this::handleHealth);
        server.createContext("/nodes", this::handleNodes);
        server.createContext("/automation/validate", this::handleValidate);
        server.createContext("/screenshot", this::handleScreenshot);
        server.createContext("/action/click", this::handleClick);
        server.createContext("/action/type", this::handleType);
        server.createContext("/action/pressKey", this::handlePressKey);
        server.createContext("/wait/node", this::handleWaitNode);

        server.start();
        readinessState.set(ReadinessState.READY);
        log.info("DesktopAutomationServer listening on {}:{} (artifacts={}, tokenProtected={})",
                server.getAddress().getHostString(),
                server.getAddress().getPort(),
                artifactsDir,
                token != null);
    }

    private void applyDefaultWindowSize() throws Exception {
        callOnFxThread(() -> {
            stage.setWidth(defaultWidth);
            stage.setHeight(defaultHeight);
            if (!stage.isShowing()) {
                stage.show();
            }
            stage.toFront();
            stage.requestFocus();
            return null;
        });
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        if (!isAuthorized(exchange)) {
            sendText(exchange, 401, "Unauthorized");
            return;
        }
        try {
            String body = callOnFxThread(() -> GSON.toJson(Map.of(
                    "status", "ok",
                    "protocolVersion", PROTOCOL_VERSION,
                    "showing", stage.isShowing(),
                    "sceneReady", stage.getScene() != null,
                    "readiness", readinessState.get().name(),
                    "ts", Instant.now().toString()
            )));
            sendJson(exchange, 200, body);
        } catch (Exception e) {
            log.error("Failed to evaluate DesktopAutomationServer health state", e);
            sendText(exchange, 500, "Failed to evaluate health state: " + e.getMessage());
        }
    }

    private void handleNodes(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        if (!isAuthorized(exchange)) {
            sendText(exchange, 401, "Unauthorized");
            return;
        }
        if (!requireReady(exchange)) {
            return;
        }
        try {
            String body = callOnFxThread(() -> serializeNodesPayload(captureSceneIndex()));
            sendJson(exchange, 200, body);
        } catch (Exception e) {
            log.error("Failed to dump nodes", e);
            sendText(exchange, 500, "Failed to dump nodes: " + e.getMessage());
        }
    }

    private void handleValidate(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        if (!isAuthorized(exchange)) {
            sendText(exchange, 401, "Unauthorized");
            return;
        }
        if (!requireReady(exchange)) {
            return;
        }
        try {
            DesktopAutomationSceneIndex sceneIndex = callOnFxThread(this::captureSceneIndex);
            String body = serializeValidationPayload(sceneIndex);
            sendJson(exchange, sceneIndex.isValid() ? 200 : 409, body);
        } catch (Exception e) {
            log.error("Failed to validate automation selectors", e);
            sendText(exchange, 500, "Failed to validate automation selectors: " + e.getMessage());
        }
    }

    private void handleScreenshot(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        if (!isAuthorized(exchange)) {
            sendText(exchange, 401, "Unauthorized");
            return;
        }
        if (!requireReady(exchange)) {
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String requestedName = query.getOrDefault("name", "shot");
        String sanitized = sanitizeFilePart(requestedName);
        String fileName = Instant.now().toEpochMilli() + "-" + sanitized + ".png";
        Path screenshotPath = artifactsDir.resolve(fileName).normalize();
        if (!screenshotPath.startsWith(artifactsDir)) {
            sendText(exchange, 400, "Invalid screenshot name");
            return;
        }

        try {
            WritableImage image = callOnFxThread(() -> {
                List<Scene> scenes = collectShowingScenes();
                for (Scene candidate : scenes) {
                    if (candidate.getRoot() != null) {
                        return candidate.snapshot(null);
                    }
                }
                return null;
            });

            if (image == null) {
                sendText(exchange, 409, "Scene not ready for screenshot");
                return;
            }

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", screenshotPath.toFile());
            String body = "{\"status\":\"ok\",\"path\":\"" + jsonEscape(screenshotPath.toString()) + "\"}";
            sendJson(exchange, 200, body);
        } catch (Exception e) {
            log.error("Failed to create screenshot", e);
            sendText(exchange, 500, "Failed to create screenshot: " + e.getMessage());
        }
    }

    private void handleClick(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        if (!isAuthorized(exchange)) {
            sendText(exchange, 401, "Unauthorized");
            return;
        }
        if (!requireReady(exchange)) {
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String rawSelector = query.get("selector");
        if (rawSelector == null || rawSelector.isBlank()) {
            sendText(exchange, 400, "Missing required query parameter: selector");
            return;
        }

        try {
            DesktopAutomationSelector selector = DesktopAutomationSelector.parse(rawSelector);
            boolean clicked = callOnFxThread(() -> clickBySelector(selector));
            if (!clicked) {
                sendText(exchange, 404, "Node not found or not clickable: " + selector.asString());
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        } catch (IllegalArgumentException e) {
            sendText(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, e.getMessage());
        } catch (Exception e) {
            log.error("Failed click action for selector={}", rawSelector, e);
            sendText(exchange, 500, "Click failed: " + e.getMessage());
        }
    }

    private void handleType(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        if (!isAuthorized(exchange)) {
            sendText(exchange, 401, "Unauthorized");
            return;
        }
        if (!requireReady(exchange)) {
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String rawSelector = query.get("selector");
        String text = query.get("text");
        if (rawSelector == null || rawSelector.isBlank()) {
            sendText(exchange, 400, "Missing required query parameter: selector");
            return;
        }
        if (text == null) {
            sendText(exchange, 400, "Missing required query parameter: text");
            return;
        }

        try {
            DesktopAutomationSelector selector = DesktopAutomationSelector.parse(rawSelector);
            boolean typed = callOnFxThread(() -> typeBySelector(selector, text));
            if (!typed) {
                sendText(exchange, 404, "Text input not found: " + selector.asString());
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        } catch (IllegalArgumentException e) {
            sendText(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, e.getMessage());
        } catch (Exception e) {
            log.error("Failed type action for selector={}", rawSelector, e);
            sendText(exchange, 500, "Type failed: " + e.getMessage());
        }
    }

    private void handlePressKey(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        if (!isAuthorized(exchange)) {
            sendText(exchange, 401, "Unauthorized");
            return;
        }
        if (!requireReady(exchange)) {
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String key = query.get("key");
        String rawSelector = query.get("selector");
        boolean shiftDown = parseBoolean(query.get("shift"), false);
        boolean controlDown = parseBoolean(query.get("ctrl"), false);
        boolean altDown = parseBoolean(query.get("alt"), false);
        boolean metaDown = parseBoolean(query.get("meta"), false);

        if (key == null || key.isBlank()) {
            sendText(exchange, 400, "Missing required query parameter: key");
            return;
        }

        try {
            DesktopAutomationSelector selector = rawSelector == null || rawSelector.isBlank()
                    ? null
                    : DesktopAutomationSelector.parse(rawSelector);
            boolean pressed = callOnFxThread(() ->
                    pressKey(key, selector, shiftDown, controlDown, altDown, metaDown));
            if (!pressed) {
                sendText(exchange, 404, "Unable to dispatch key press");
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        } catch (IllegalArgumentException e) {
            sendText(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, e.getMessage());
        } catch (Exception e) {
            log.error("Failed pressKey action for key={}", key, e);
            sendText(exchange, 500, "pressKey failed: " + e.getMessage());
        }
    }

    private void handleWaitNode(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "POST")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        if (!isAuthorized(exchange)) {
            sendText(exchange, 401, "Unauthorized");
            return;
        }
        if (!requireReady(exchange)) {
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String rawSelector = query.get("selector");
        long timeoutMs = parseLong(query.get("timeoutMs"), 5000L);
        boolean requireVisible = parseBoolean(query.get("visible"), false);

        if (rawSelector == null || rawSelector.isBlank()) {
            sendText(exchange, 400, "Missing required query parameter: selector");
            return;
        }
        if (timeoutMs < 1) {
            sendText(exchange, 400, "timeoutMs must be >= 1");
            return;
        }

        try {
            DesktopAutomationSelector selector = DesktopAutomationSelector.parse(rawSelector);
            boolean found = waitForNode(selector, timeoutMs, requireVisible);
            if (!found) {
                sendText(exchange, 408, "Timed out waiting for node " + selector.asString());
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        } catch (IllegalArgumentException e) {
            sendText(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, e.getMessage());
        } catch (Exception e) {
            log.error("Failed wait-node for selector={}", rawSelector, e);
            sendText(exchange, 500, "wait-node failed: " + e.getMessage());
        }
    }

    static String extractNodeText(Node node) {
        if (node instanceof Labeled labeled) {
            return Objects.toString(labeled.getText(), "");
        }
        if (node instanceof PasswordField) {
            return "";
        }
        if (node instanceof TextInputControl textInputControl) {
            return Objects.toString(textInputControl.getText(), "");
        }
        if (node instanceof Text text) {
            return Objects.toString(text.getText(), "");
        }
        return "";
    }

    private boolean clickBySelector(DesktopAutomationSelector selector) {
        Node node = lookupNode(selector);
        if (node == null) {
            return false;
        }
        return dispatchClick(node);
    }

    static boolean dispatchClick(Node node) {
        if (!isInteractable(node)) {
            return false;
        }
        if (node instanceof ButtonBase buttonBase) {
            // fire() triggers onAction; MOUSE_CLICKED covers views that use setOnMouseClicked instead
            buttonBase.fire();
            buttonBase.fireEvent(createMouseEvent(MouseEvent.MOUSE_CLICKED, false));
            return true;
        }
        node.requestFocus();
        node.fireEvent(createMouseEvent(MouseEvent.MOUSE_PRESSED, true));
        node.fireEvent(createMouseEvent(MouseEvent.MOUSE_RELEASED, false));
        node.fireEvent(createMouseEvent(MouseEvent.MOUSE_CLICKED, false));
        return true;
    }

    private static MouseEvent createMouseEvent(javafx.event.EventType<MouseEvent> eventType,
                                               boolean primaryButtonDown) {
        return new MouseEvent(eventType,
                0, 0,
                0, 0,
                MouseButton.PRIMARY,
                1,
                false, false, false, false,
                primaryButtonDown, false, false,
                false, false, false,
                null);
    }

    private boolean typeBySelector(DesktopAutomationSelector selector, String text) {
        Node node = lookupNode(selector);
        if (!(node instanceof TextInputControl textInputControl)) {
            return false;
        }
        if (!isInteractable(textInputControl) || !textInputControl.isEditable()) {
            return false;
        }
        textInputControl.requestFocus();
        textInputControl.setText(text);
        return true;
    }

    private static boolean isInteractable(Node node) {
        if (node.getScene() == null || node.isDisabled()) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (!current.isVisible() || current.isMouseTransparent() || current.getOpacity() <= 0.0d) {
                return false;
            }
            current = current.getParent();
        }
        return true;
    }

    private boolean pressKey(String key,
                             @Nullable DesktopAutomationSelector selector,
                             boolean shiftDown,
                             boolean controlDown,
                             boolean altDown,
                             boolean metaDown) {
        List<Scene> scenes = collectShowingScenes();
        if (scenes.isEmpty()) {
            return false;
        }

        KeyCode keyCode;
        try {
            keyCode = KeyCode.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown key code: " + key);
        }

        Node target;
        if (selector != null) {
            target = lookupNode(selector);
            if (target == null) {
                return false;
            }
            target.requestFocus();
        } else {
            Scene topmost = scenes.get(0);
            target = topmost.getFocusOwner();
            if (target == null) {
                target = topmost.getRoot();
                if (target != null) {
                    target.requestFocus();
                }
            }
            if (target == null) {
                return false;
            }
        }

        target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode,
                shiftDown, controlDown, altDown, metaDown));
        target.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCode,
                shiftDown, controlDown, altDown, metaDown));
        return true;
    }

    private boolean waitForNode(DesktopAutomationSelector selector, long timeoutMs, boolean requireVisible) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            boolean available = callOnFxThread(() -> isNodeAvailable(selector, requireVisible));
            if (available) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private boolean isNodeAvailable(DesktopAutomationSelector selector, boolean requireVisible) {
        Node node = lookupNode(selector);
        if (node == null) {
            return false;
        }
        if (!requireVisible) {
            return true;
        }
        return node.isVisible() && node.getScene() != null;
    }

    @Nullable
    private Node lookupNode(DesktopAutomationSelector selector) {
        DesktopAutomationSceneIndex sceneIndex = captureSceneIndex();
        if (!sceneIndex.isValid()) {
            throw new IllegalStateException(serializeValidationPayload(sceneIndex));
        }
        return sceneIndex.find(selector);
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String received = exchange.getRequestHeaders().getFirst(AUTH_HEADER);
        if (received == null) {
            return false;
        }
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isMethod(HttpExchange exchange, String method) {
        return method.equalsIgnoreCase(exchange.getRequestMethod());
    }

    private boolean requireReady(HttpExchange exchange) throws IOException {
        ReadinessState readiness = readinessState.get();
        if (readiness == ReadinessState.READY) {
            return true;
        }
        sendJson(exchange, 409, GSON.toJson(Map.of(
                "status", "not_ready",
                "protocolVersion", PROTOCOL_VERSION,
                "readiness", readiness.name()
        )));
        return false;
    }

    private static boolean isLoopbackHost(String host) {
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("127.0.0.1")
                || normalized.equals("localhost")
                || normalized.equals("::1");
    }

    private static Map<String, String> parseQuery(URI uri) {
        String raw = uri.getRawQuery();
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key;
            String value;
            if (idx >= 0) {
                key = pair.substring(0, idx);
                value = pair.substring(idx + 1);
            } else {
                key = pair;
                value = "";
            }
            result.put(urlDecode(key), urlDecode(value));
        }
        return result;
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }

    private static boolean parseBoolean(@Nullable String raw, boolean defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return defaultValue;
        }
        return normalized.equals("1") ||
                normalized.equals("true") ||
                normalized.equals("yes") ||
                normalized.equals("on");
    }

    private static long parseLong(@Nullable String raw, long defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private DesktopAutomationSceneIndex captureSceneIndex() {
        return DesktopAutomationSceneIndex.createFromMultipleScenes(collectShowingScenes());
    }

    private List<Scene> collectShowingScenes() {
        List<Scene> overlays = new ArrayList<>();
        Scene primaryScene = stage.getScene();
        for (Window window : Window.getWindows()) {
            if (!window.isShowing()) {
                continue;
            }
            Scene scene = window.getScene();
            if (scene == null || scene == primaryScene) {
                continue;
            }
            overlays.add(scene);
        }
        List<Scene> result = new ArrayList<>(overlays.size() + 1);
        result.addAll(overlays);
        if (primaryScene != null) {
            result.add(primaryScene);
        }
        return result;
    }

    private String serializeNodesPayload(DesktopAutomationSceneIndex sceneIndex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", sceneIndex.isValid() ? "ok" : "invalid");
        payload.put("protocolVersion", PROTOCOL_VERSION);
        payload.put("nodes", sceneIndex.nodes());
        payload.put("validationErrors", sceneIndex.validationIssues());
        payload.put("ts", Instant.now().toString());
        return GSON.toJson(payload);
    }

    private String serializeValidationPayload(DesktopAutomationSceneIndex sceneIndex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", sceneIndex.isValid() ? "ok" : "invalid");
        payload.put("protocolVersion", PROTOCOL_VERSION);
        payload.put("validationErrors", sceneIndex.validationIssues());
        payload.put("ts", Instant.now().toString());
        return GSON.toJson(payload);
    }

    private static String sanitizeFilePart(String input) {
        String sanitized = input.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return "shot";
        }
        return sanitized;
    }

    private static String jsonEscape(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private <T> T callOnFxThread(Callable<T> callable) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(fxTimeoutMs, TimeUnit.MILLISECONDS);
    }

}
