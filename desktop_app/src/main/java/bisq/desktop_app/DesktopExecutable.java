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

package bisq.desktop_app;

import bisq.common.application.Executable;
import bisq.common.application.JavaFXApplication;
import bisq.desktop.DesktopController;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.overlay.Popup;
import javafx.application.Application;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static bisq.common.util.OsUtils.EXIT_FAILURE;

@Slf4j
public class DesktopExecutable extends Executable<DesktopApplicationService> {
    @Nullable
    private DesktopController desktopController;

    public DesktopExecutable(String[] args) {
        super(args);
    }

    @Override
    protected DesktopApplicationService createApplicationService(String[] args) {
        return new DesktopApplicationService(args, this);
    }

    @Override
    protected void launchApplication(String[] args) {
        new Thread(() -> {
            Thread.currentThread().setName("Java FX Application Launcher");
            Application.launch(JavaFXApplication.class, args); //blocks until app is closed
        }).start();

        JavaFXApplication.onApplicationLaunched
                .whenComplete((applicationData, throwable) -> {
                    if (throwable == null) {
                        try {
                            log.info("Java FX Application launched");
                            desktopController = new DesktopController(applicationService.getState(), applicationService.getServiceProvider(),
                                    applicationData,
                                    this::onApplicationLaunched);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    } else {
                        log.error("Could not launch JavaFX application.", throwable);
                        if (Platform.isFxApplicationThread()) {
                            applicationService.shutdown().thenAccept(result -> Platform.exit());
                        } else {
                            applicationService.shutdown().thenAccept(result -> System.exit(EXIT_FAILURE));
                        }
                    }
                });
    }

    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        if (desktopController == null) {
            UIThread.run(() -> new Popup().error(throwable).show());
            return;
        }
        Platform.runLater(() -> desktopController.onApplicationServiceInitialized(result, throwable));
    }

    @Override
    protected void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception", throwable);
            UIThread.run(() -> {
                if (desktopController != null) {
                    desktopController.onUncaughtException(thread, throwable);
                } else {
                    log.error("primaryStageController not set yet");
                }
            });
        });
    }
}
