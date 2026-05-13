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

package bisq.desktop_ui_harness_app;

import bisq.common.platform.PlatformUtils;
import bisq.desktop.common.view.ViewLifecycleObservers;
import bisq.desktop_automation.DesktopAutomationServer;
import bisq.desktop_app.DesktopApp;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DesktopUiHarnessApp {
    private final AtomicReference<DesktopAutomationServer> desktopAutomationServer = new AtomicReference<>();
    private final AtomicReference<AutoCloseable> automationViewObserverRegistration = new AtomicReference<>();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        Thread.currentThread().setName("DesktopUiHarnessApp.main");
        try {
            new DesktopUiHarnessApp().run(args);
        } catch (Throwable throwable) {
            log.error("Desktop UI harness startup failed", throwable);
            System.exit(PlatformUtils.EXIT_FAILURE);
        }
    }

    private void run(String[] args) throws Exception {
        DesktopUiHarnessConfig config = DesktopUiHarnessConfig.fromSystemProperties();
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
        automationViewObserverRegistration.set(ViewLifecycleObservers.register(new DesktopAutomationViewObserver()));

        DesktopApp.main(args);

        Stage stage = new DesktopUiHarnessStageLocator(config.stageTimeoutMs(), config.fxTimeoutMs()).awaitStage();
        desktopAutomationServer.set(DesktopAutomationServer.start(stage, config.toAutomationConfig()));
        log.info("Desktop UI harness attached on {}:{}", config.bindHost(), config.bindPort());
        keepRunning();
    }

    private void onShutdown() {
        log.info("Desktop UI harness shutting down");
        stopAutomationServer();
        shutdownLatch.countDown();
    }

    private void stopAutomationServer() {
        DesktopAutomationServer server = desktopAutomationServer.getAndSet(null);
        if (server != null) {
            server.stop();
        }
        AutoCloseable registration = automationViewObserverRegistration.getAndSet(null);
        if (registration != null) {
            try {
                registration.close();
            } catch (Exception e) {
                log.warn("Failed to unregister desktop automation view observer", e);
            }
        }
    }

    private void keepRunning() {
        log.info("Desktop UI harness main thread waiting for JVM shutdown");
        boolean interrupted = false;
        while (true) {
            try {
                shutdownLatch.await();
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            } catch (InterruptedException e) {
                interrupted = true;
                log.warn("Desktop UI harness main thread interrupted; keeping harness alive until JVM shutdown");
            }
        }
    }
}
