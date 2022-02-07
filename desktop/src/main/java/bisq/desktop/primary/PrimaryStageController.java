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
import bisq.desktop.common.utils.DontShowAgainLookup;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.Notification;
import bisq.desktop.overlay.Overlay;
import bisq.desktop.primary.main.MainController;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
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
    private final SettingsService settingsService;

    public PrimaryStageController(DefaultApplicationService applicationService, JavaFXApplication.Data applicationData) {
        this.applicationService = applicationService;
        settingsService = applicationService.getSettingsService();

        model = new PrimaryStageModel(applicationService);
        view = new PrimaryStageView(model, this, applicationData.stage());
        mainController = new MainController(applicationService);

        Browser.setHostServices(applicationData.hostServices());
        Transitions.setDisplaySettings(applicationService.getSettingsService().getDisplaySettings());
        DontShowAgainLookup.setPreferences(applicationService.getSettingsService());
        Notification.init(view.getRoot(), applicationService.getSettingsService().getDisplaySettings());
        Overlay.init(view.getRoot(),
                applicationService.getApplicationConfig().baseDir(),
                applicationService.getSettingsService().getDisplaySettings(),
                this::shutdown);

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
        settingsService.getPersistableStore().getCookie().putAsDouble(CookieKey.STAGE_X, value);
        settingsService.persist();
    }

    public void onStageYChanged(double value) {
        settingsService.getPersistableStore().getCookie().putAsDouble(CookieKey.STAGE_Y, value);
        settingsService.persist();
    }

    public void onStageWidthChanged(double value) {
        settingsService.getPersistableStore().getCookie().putAsDouble(CookieKey.STAGE_W, value);
        settingsService.persist();
    }

    public void onStageHeightChanged(double value) {
        settingsService.getPersistableStore().getCookie().putAsDouble(CookieKey.STAGE_H, value);
        settingsService.persist();
    }
}
