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

import bisq.desktop.automation.DesktopAutomationMetadata;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopAutomationServerTimeoutTest {
    @BeforeAll
    static void initJavaFxToolkit() {
        new JFXPanel();
        Platform.setImplicitExit(false);
    }

    @Test
    void timedOutQueuedClickDoesNotExecuteWhenUiResumes(@TempDir Path artifactsDir) throws Exception {
        AtomicInteger clicks = new AtomicInteger();
        Stage stage = onFxThread(() -> {
            Button button = new Button("Click");
            button.setOnAction(event -> clicks.incrementAndGet());
            DesktopAutomationMetadata.setId(button, "click");
            StackPane root = new StackPane(button);
            DesktopAutomationMetadata.setScope(root, "timeout-test");
            Stage result = new Stage();
            result.setScene(new Scene(root));
            return result;
        });
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        DesktopAutomationConfig config = new DesktopAutomationConfig(
                "127.0.0.1", port, 100, 200, 120, "test-token", artifactsDir.toString());
        DesktopAutomationServer server = onFxThread(() -> DesktopAutomationServer.start(stage, config));
        CountDownLatch uiBlocked = new CountDownLatch(1);
        CountDownLatch releaseUi = new CountDownLatch(1);
        try (HttpClient client = HttpClient.newHttpClient()) {
            Platform.runLater(() -> {
                uiBlocked.countDown();
                try {
                    releaseUi.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(uiBlocked.await(5, TimeUnit.SECONDS));
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port +
                            "/action/click?selector=timeout-test/click"))
                    .header("X-Bisq-Automation-Token", "test-token")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            assertEquals(500, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
            releaseUi.countDown();
            assertEquals(0, onFxThread(clicks::get));

            assertEquals(200, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
            assertEquals(1, onFxThread(clicks::get));
        } finally {
            releaseUi.countDown();
            server.stop();
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    private static <T> T onFxThread(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }
}
