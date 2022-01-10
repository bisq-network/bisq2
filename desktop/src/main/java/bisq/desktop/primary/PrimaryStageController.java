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

package bisq.desktop.primary;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.JavaFXApplication;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.primary.main.MainController;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrimaryStageController implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final PrimaryStageModel model;
    @Getter
    private final PrimaryStageView view;
    private final MainController mainController;
    private final OverlayController overlayController;

    public PrimaryStageController(DefaultServiceProvider serviceProvider, JavaFXApplication.Data applicationData) {
        this.serviceProvider = serviceProvider;

        Browser.setHostServices(applicationData.hostServices());

        model = new PrimaryStageModel(serviceProvider);
        view = new PrimaryStageView(model, this, applicationData.stage());
        overlayController = new OverlayController(view.getScene());
        mainController = new MainController(serviceProvider, overlayController);
        model.setView(mainController.getView());
    }

    @Override
    public void onViewAttached() {
        // called at view creation
        // todo add splash screen
    }


    public void onDomainInitialized() {
        mainController.onDomainInitialized();
    }

    public void onUncaughtException(Thread thread, Throwable throwable) {
        // todo show error popup
    }

    public void onQuit() {
        shutdown();
    }

    public void onInitializeDomainFailed() {
        //todo show error popup
    }

    public void shutdown() {
        serviceProvider.shutdown()
                .whenComplete((__, throwable) -> Platform.exit());
    }

    public void onStageXChanged(double value) {
        model.setStageX(value);
    }

    public void onStageYChanged(double value) {
        model.setStageY(value);
    }

    public void onStageWidthChanged(double value) {
        model.setStageWidth(value);
    }

    public void onStageHeightChanged(double value) {
        model.setStageHeight(value);
    }
}
