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

import bisq.application.DefaultApplicationService;
import bisq.desktop.JavaFXApplication;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.primary.main.MainController;
import bisq.user.CookieKey;
import bisq.user.UserService;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrimaryStageController implements Controller {
    private final DefaultApplicationService applicationService;
    private final PrimaryStageModel model;
    @Getter
    private final PrimaryStageView view;
    private final MainController mainController;
    private final OverlayController overlayController;
    private final UserService userService;

    public PrimaryStageController(DefaultApplicationService applicationService, JavaFXApplication.Data applicationData) {
        this.applicationService = applicationService;
        userService = applicationService.getUserService();
        Browser.setHostServices(applicationData.hostServices());

        model = new PrimaryStageModel(applicationService);
        view = new PrimaryStageView(model, this, applicationData.stage());
        overlayController = new OverlayController(view.getScene(), applicationService);
        mainController = new MainController(applicationService);
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
        applicationService.shutdown()
                .whenComplete((__, throwable) -> Platform.exit());
    }

    public void onStageXChanged(double value) {
        userService.getUserStore().getCookie().putAsDouble(CookieKey.STAGE_X, value);
        userService.persist();
    }

    public void onStageYChanged(double value) {
        userService.getUserStore().getCookie().putAsDouble(CookieKey.STAGE_Y, value);
        userService.persist();
    }

    public void onStageWidthChanged(double value) {
        userService.getUserStore().getCookie().putAsDouble(CookieKey.STAGE_W, value);
        userService.persist();
    }

    public void onStageHeightChanged(double value) {
        userService.getUserStore().getCookie().putAsDouble(CookieKey.STAGE_H, value);
        userService.persist();
    }
}
