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

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.MainController;
import bisq.desktop.overlay.OverlayController;
import javafx.application.Application;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

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

        // todo add splash screen

        stageView.addMainView(mainController.getView());
        model.setTitle(serviceProvider.getApplicationOptions().appName());
    }

    public void onDomainInitialized() {
    }

    public void onUncaughtException(Thread thread, Throwable throwable) {
        // todo show error popup
    }

    @Override
    public StageView getView() {
        return stageView;
    }

    public void onQuit() {
        shutdown();
    }

    public void onInitializeDomainFailed() {
        //todo show error popup
    }

    public void shutdown() {
        serviceProvider.shutdown().whenComplete((__, throwable) -> Platform.exit());
    }
}
