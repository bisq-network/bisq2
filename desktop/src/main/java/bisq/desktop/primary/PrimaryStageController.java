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
import bisq.desktop.common.Browser;
import bisq.desktop.common.JavaFxApplicationData;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Notification;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.primary.main.MainController;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.splash.SplashController;
import bisq.settings.CookieKey;
import bisq.settings.DisplaySettings;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import javafx.application.Platform;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.settings.DontShowAgainKey.BISQ_2_INTRO;

@Slf4j
public class PrimaryStageController extends NavigationController {
    protected final DefaultApplicationService applicationService;
    @Getter
    protected final PrimaryStageModel model;
    @Getter
    protected final PrimaryStageView view;
    protected final SettingsService settingsService;
    protected final Runnable onStageReadyHandler;
    private final SplashController splashController;

    public PrimaryStageController(DefaultApplicationService applicationService,
                                  JavaFxApplicationData applicationJavaFxApplicationData,
                                  Runnable onStageReadyHandler) {
        super(NavigationTarget.PRIMARY_STAGE);

        this.applicationService = applicationService;
        settingsService = applicationService.getSettingsService();
        this.onStageReadyHandler = onStageReadyHandler;

        model = new PrimaryStageModel(applicationService);
        view = new PrimaryStageView(model, this, applicationJavaFxApplicationData.getStage());

        splashController = new SplashController(applicationService);

        Browser.setHostServices(applicationJavaFxApplicationData.getHostServices());
        DisplaySettings displaySettings = settingsService.getDisplaySettings();
        Transitions.setDisplaySettings(displaySettings);
        AnchorPane viewRoot = view.getRoot();
        Notification.init(viewRoot, displaySettings);
        Navigation.init(settingsService);
        Overlay.init(viewRoot,
                applicationService.getAppConfig().getBaseDir(),
                displaySettings,
                this::shutdown);

        // Here we start to attach the view hierarchy to the stage.
        view.showStage();

        new OverlayController(applicationService, viewRoot);
    }

    @Override
    public void onActivate() {
        onStageReadyHandler.run();

        Navigation.navigateTo(NavigationTarget.SPLASH);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case SPLASH: {
                return Optional.of(splashController);
            }
            case MAIN: {
                return Optional.of(new MainController(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    public void onDomainInitialized() {
        splashController.stopAnimation();
        if (DontShowAgainService.showAgain(BISQ_2_INTRO)) {
            Navigation.navigateTo(NavigationTarget.ONBOARDING_BISQ_2_INTRO);
        } else if (applicationService.getSocialService().getChatUserService().isDefaultUserProfileMissing()) {
            Navigation.navigateTo(NavigationTarget.ONBOARDING_GENERATE_NYM);
       /* } else if (!settingsService.getCookie().getAsOptionalBoolean(CookieKey.BISQ_EASY_ONBOARDED).orElse(false)) {
            Navigation.navigateTo(NavigationTarget.ONBOARDING_BISQ_EASY_OLD);*/
        } else {
            // After the domain is initialized we show the application content
            String value = settingsService.getCookie().getValue(CookieKey.NAVIGATION_TARGET);
            if (value != null && !value.isEmpty()) {
                try {
                    NavigationTarget persisted = NavigationTarget.valueOf(value);
                    Navigation.applyPersisted(persisted);
                    Navigation.navigateTo(persisted);
                } catch (Throwable t) {
                    Navigation.navigateTo(NavigationTarget.DASHBOARD);
                }
            } else {
                Navigation.navigateTo(NavigationTarget.DASHBOARD);
            }
        }
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
                .thenAccept(__ -> Platform.exit());
    }

    public void onStageXChanged(double value) {
        settingsService.setCookie(CookieKey.STAGE_X, value);
    }

    public void onStageYChanged(double value) {
        settingsService.setCookie(CookieKey.STAGE_Y, value);
    }

    public void onStageWidthChanged(double value) {
        settingsService.setCookie(CookieKey.STAGE_W, value);
    }

    public void onStageHeightChanged(double value) {
        settingsService.setCookie(CookieKey.STAGE_H, value);
    }
}
