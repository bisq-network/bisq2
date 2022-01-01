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

package network.misq.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.Browser;
import network.misq.desktop.common.UncaughtExceptionHandler;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.MainController;
import network.misq.desktop.overlay.OverlayController;

@Slf4j
public class StageController implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final Application.Parameters parameters;

    private final StageModel model;
    private final StageView stageView;

    private final MainController mainController;
    private final OverlayController overlayController;

    public StageController(DefaultServiceProvider serviceProvider, JavaFXApplication.Data applicationData) {
        this.serviceProvider = serviceProvider;
        parameters = applicationData.parameters();
        Browser.setHostServices(applicationData.hostServices());

        this.model = new StageModel();
        stageView = new StageView(model, this, applicationData.stage());

        overlayController = new OverlayController(stageView.getScene());
        mainController = new MainController(serviceProvider, overlayController);

        initialize();
    }

    @Override
    public void initialize() {
        // todo add splash screen

        stageView.addMainView(mainController.getView());
        model.setTitle(serviceProvider.getApplicationOptions().appName());
        mainController.initialize();
    }

    public void onDomainInitialized() {

    }

    @Override
    public void onViewAdded() {
        //  overlayController.initialize();
    }

    @Override
    public void onViewRemoved() {
    }

    @Override
    public StageView getView() {
        return stageView;
    }

    public void onQuit() {
        shutdown();
    }

    //todo
    public static void setupUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        Thread.UncaughtExceptionHandler handler = (thread, throwable) -> {
            // Might come from another thread
            if (throwable.getCause() != null && throwable.getCause().getCause() != null) {
                log.error(throwable.getMessage());
            } else if (throwable instanceof ClassCastException &&
                    "sun.awt.image.BufImgSurfaceData cannot be cast to sun.java2d.xr.XRSurfaceData".equals(throwable.getMessage())) {
                log.warn(throwable.getMessage());
            } else if (throwable instanceof UnsupportedOperationException &&
                    "The system tray is not supported on the current platform.".equals(throwable.getMessage())) {
                log.warn(throwable.getMessage());
            } else {
                log.error("Uncaught Exception from thread " + Thread.currentThread().getName());
                log.error("throwableMessage= " + throwable.getMessage());
                log.error("throwableClass= " + throwable.getClass());
                // log.error("Stack trace:\n" + ExceptionUtils.getStackTrace(throwable));
                throwable.printStackTrace();
                UIThread.run(() -> uncaughtExceptionHandler.handleUncaughtException(throwable, false));
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);
    }

    public void onInitializeDomainFailed() {
        //todo show error popup
    }

    public void shutdown() {
        serviceProvider.shutdown().whenComplete((__, throwable) -> Platform.exit());
    }
}
