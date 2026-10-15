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

import bisq.application.AnotherInstanceRunningException;
import bisq.application.Executable;
import bisq.application.InstanceLockException;
import bisq.application.InstanceLockUnavailableException;
import bisq.desktop.DesktopController;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import javafx.application.Application;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.Arrays;

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
                            desktopController = new DesktopController(applicationService.getState(),
                                    applicationService.getServiceProvider(),
                                    applicationData,
                                    this::onApplicationLaunched);
                            desktopController.init();
                        } catch (Throwable t) {
                            shutdownAfterStartupFailure("Desktop startup failed", t);
                        }
                    } else {
                        shutdownAfterStartupFailure("Could not launch JavaFX application.", throwable);
                    }
                });
    }

    @Override
    protected void handleInstanceLockFailure(InstanceLockException exception) {
        log.error(exception.getMessage(), exception.getCause());
        String appName = exception.getAppName();
        Path appDataDirPath = exception.getAppDataDirPath();
        String headline;
        String message;
        if (exception instanceof AnotherInstanceRunningException anotherInstanceRunningException) {
            headline = Res.get("popup.alreadyRunning.headline", appName);
            // The PID lets the user find the other instance if it is not visible, e.g. a hanging process.
            // We pass it as string as MessageFormat would render a number with grouping separators.
            message = Res.get("popup.alreadyRunning.msg", appName, appDataDirPath) +
                    anotherInstanceRunningException.getOwnerPid()
                            .map(pid -> "\n\n" + Res.get("popup.alreadyRunning.ownerPid", String.valueOf(pid)))
                            .orElse("");
        } else {
            headline = Res.get("popup.instanceLockFailed.headline", appName);
            String reason = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();
            message = Res.get("popup.instanceLockFailed.msg", appName, appDataDirPath,
                    InstanceLockUnavailableException.DISABLE_CHECK_OPTION, reason);
        }
        // We are called before JavaFX is launched, thus we cannot use our Popup. We use a lightweight AWT dialog
        // instead if a display is available and fall back to stderr otherwise.
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                SwingUtilities.invokeAndWait(() ->
                        JOptionPane.showMessageDialog(null, message, headline, JOptionPane.WARNING_MESSAGE));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted state
            } catch (Throwable t) {
                log.warn("Could not show the 'already running' dialog", t);
            }
        }
        System.err.println("Error: " + message);
        System.exit(EXIT_FAILURE);
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
