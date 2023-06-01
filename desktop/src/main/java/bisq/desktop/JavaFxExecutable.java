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

package bisq.desktop;

import bisq.application.DefaultApplicationService;
import bisq.application.Executable;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.PrimaryStageController;
import javafx.application.Application;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static bisq.common.util.OsUtils.EXIT_FAILURE;

@Slf4j
public class JavaFxExecutable extends Executable<DefaultApplicationService> {
    @Nullable
    private PrimaryStageController primaryStageController;

    public JavaFxExecutable(String[] args) {
        super(args);
    }

    @Override
    protected DefaultApplicationService createApplicationService(String[] args) {
        return new DefaultApplicationService(args);
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
                            primaryStageController = new PrimaryStageController(applicationService,
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
        if (primaryStageController != null) {
            Platform.runLater(() -> primaryStageController.onApplicationServiceInitialized(result, throwable));
        } else if (throwable != null) {
            UIThread.run(() -> new Popup().error(throwable).show());
        }
    }

    @Override
    protected void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception", throwable);
            UIThread.run(() -> {
                if (primaryStageController != null) {
                    primaryStageController.onUncaughtException(thread, throwable);
                } else {
                    log.error("primaryStageController not set yet");
                }
            });
        });
    }
}
