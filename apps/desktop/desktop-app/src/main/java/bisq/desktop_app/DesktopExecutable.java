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

import bisq.application.Executable;
import bisq.desktop.DesktopController;
import bisq.desktop.common.application.JavaFxApplicationData;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

import static bisq.common.platform.PlatformUtils.EXIT_FAILURE;

@Slf4j
public class DesktopExecutable extends Executable<DesktopApplicationService> {
    @Nullable
    private DesktopController desktopController;
    @Nullable
    private Popup shutdownInProcessPopup;

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
            Thread.currentThread().setName("JavaFXApplication.launch");
            Application.launch(JavaFXApplication.class, args); //blocks until app is closed
        }).start();

        JavaFXApplication.onApplicationLaunched
                .whenComplete((applicationData, throwable) -> {
                    if (throwable == null) {
                        try {
                            log.info("Java FX Application launched");
                            setupStartupAndShutdownErrorHandlers();
                            startDesktopControllerAfterTailsCheck(applicationData);
                        } catch (Throwable t) {
                            shutdownAfterStartupFailure("Desktop startup failed", t);
                        }
                    } else {
                        shutdownAfterStartupFailure("Could not launch JavaFX application.", throwable);
                    }
                });
    }

    /**
     * On Tails, if the data directory is not on the Persistent Storage volume the user would lose
     * their identity keys and open offers on shutdown. Warn with a quit/continue popup before any
     * domain initialization starts (domain init is only triggered from desktopController via the
     * onApplicationLaunched callback, so deferring its creation defers initialization too).
     */
    private void startDesktopControllerAfterTailsCheck(JavaFxApplicationData applicationData) {
        if (!applicationService.isTailsDataDirNonPersistent()) {
            startDesktopController(applicationData);
            return;
        }

        // The app UI does not exist yet, so the Bisq Popup (which needs an owner scene) cannot be used.
        // Defer a standalone JavaFX Alert via runLater so it shows after the startup pulse, then gate
        // domain initialization on the user's choice (continue) or shut down.
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(Res.get("action.shutDown"));
            alert.setHeaderText(null);
            alert.setContentText(Res.get("popup.tails.noPersistentStorage.warning"));
            alert.getDialogPane().setMinWidth(560);

            ButtonType continueButton = new ButtonType(
                    Res.get("popup.tails.noPersistentStorage.continue"), ButtonBar.ButtonData.OK_DONE);
            ButtonType shutDownButton = new ButtonType(
                    Res.get("action.shutDown"), ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(continueButton, shutDownButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == continueButton) {
                startDesktopController(applicationData);
            } else {
                exitJavaFXPlatform();
            }
        });
    }

    private void startDesktopController(JavaFxApplicationData applicationData) {
        desktopController = new DesktopController(applicationService.getState(),
                applicationService.getServiceProvider(),
                applicationData,
                this::onApplicationLaunched);
        desktopController.init();
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        if (desktopController == null) {
            UIThread.run(() -> new Popup().error(throwable).show());
            return;
        }
        UIThread.run(() -> desktopController.onApplicationServiceInitialized(result, throwable));
    }

    @Override
    protected void setDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception:", throwable);
            if (throwable instanceof NullPointerException &&
                    Arrays.stream(throwable.getStackTrace()).anyMatch(e -> e.getClassName().contains("GraphicsPipeline"))) {
                // Ignore known JavaFX shutdown issue when runLater tasks are executed after the rendering subsystem
                // is already torn down
                return;
            }

            if (desktopController != null) {
                UIThread.run(() -> desktopController.onUncaughtException(thread, throwable));
            } else {
                log.error("UncaughtExceptionHandler not applied as desktopController is null");
            }
        });
    }

    @Override
    protected void notifyAboutShutdown() {
        if (shutdownInProcessPopup != null) {
            return;
        }
        try {
            UIThread.run(() -> {
                shutdownInProcessPopup = new Popup()
                        .headline(Res.get("action.shutDown"))
                        .feedback(Res.get("popup.shutdown", DesktopApplicationService.SHUTDOWN_TIMEOUT_SEC));
                shutdownInProcessPopup.hideCloseButton().show();
            });
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void exitJvm() {
        if (applicationService == null) {
            log.warn("Shutdown before applicationService have been created");
            super.exitJvm();
        } else if (applicationService.getShutDownErrorMessage().get() == null) {
            // If shutDownErrorMessage is set we display an error popup we leave it to the user to close it and
            // shutdown the app by clicking the shutdown button
            try {
                // We might get called from a non UI thread
                UIThread.run(this::exitJavaFXPlatform);
            } catch (Exception e) {
                log.error("Could not map shutdown handler to UI thread", e);
                super.exitJvm();
            }
        }
    }

    private void exitJavaFXPlatform() {
        log.info("Exiting JavaFX Platform");
        try {
            Platform.exit();
        } catch (Exception e) {
            log.error("Platform.exit failed", e);
            super.exitJvm();
        }
    }

    private void shutdownAfterStartupFailure(String message, Throwable throwable) {
        log.error(message, throwable);
        applicationService.shutdown().whenComplete((ignored, shutdownThrowable) -> {
            if (shutdownThrowable != null) {
                log.warn("Shutdown after startup failure failed", shutdownThrowable);
            }
            if (Platform.isFxApplicationThread()) {
                Platform.exit();
            } else {
                System.exit(EXIT_FAILURE);
            }
        });
    }

    private void setupStartupAndShutdownErrorHandlers() {
        applicationService.getStartupErrorMessage().addObserver(errorMessage -> {
            if (errorMessage != null) {
                UIThread.run(() ->
                        new Popup().error(Res.get("popup.startup.error", errorMessage))
                                .closeButtonText(Res.get("action.shutDown"))
                                .onClose(this::exitJavaFXPlatform)
                                .show()
                );
            }
        });
        applicationService.getShutDownErrorMessage().addObserver(errorMessage -> {
            if (errorMessage != null) {
                UIThread.run(() -> {
                    if (shutdownInProcessPopup != null) {
                        shutdownInProcessPopup.hide();
                        shutdownInProcessPopup = null;
                    }
                    Popup popup = new Popup();
                    popup.error(Res.get("popup.shutdown.error", errorMessage))
                            .closeButtonText(Res.get("action.shutDown"))
                            .onClose(this::exitJavaFXPlatform)
                            .show();
                    // The error popup allow to report to GH, in that case we get closed the error popup.
                    // We leave the app open and reset the shutDownStarted flag, so that at another
                    // shutdown action shut down can happen. Only when the user clicks the close
                    // button we actually shut down.
                    popup.getIsHiddenProperty().addListener((observableValue, oldValue, newValue) -> {
                        if (newValue) {
                            shutDownStarted = false;
                        }
                    });
                    // We reset the error so that it can get triggered again our error popup in case the user shutdown again.
                    UIScheduler.run(() -> applicationService.getShutDownErrorMessage().set(null)).after(1000);
                });
            }
        });
    }
}
