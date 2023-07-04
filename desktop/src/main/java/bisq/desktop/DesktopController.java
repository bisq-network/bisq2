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

import bisq.common.observable.Observable;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.application.JavaFxApplicationData;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.MainController;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.tac.TacController;
import bisq.desktop.overlay.unlock.UnlockController;
import bisq.desktop.splash.SplashController;
import bisq.settings.Cookie;
import bisq.settings.CookieKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.settings.DontShowAgainKey.WELCOME;

/**
 * At start-up we first load the persisted data.
 * Once persisted data is loaded we show the tac window if not already accepted.
 * Then we show the unlock screen if a password protection was used.
 * Then we show the splash screen if domain initialisation is not already completed.
 * If domain initialisation is completed we show the on-boarding screen at the first visit.
 * If user has created a user profile the last persisted navigation target is shown.
 */
@Slf4j
public class DesktopController extends NavigationController {
    @Getter
    protected final DesktopModel model;
    @Getter
    protected final DesktopView view;
    protected final SettingsService settingsService;
    protected final Runnable onActivatedHandler;
    private final SplashController splashController;
    private final UserIdentityService userIdentityService;
    private final ServiceProvider serviceProvider;

    public DesktopController(Observable<State> applicationServiceState,
                             ServiceProvider serviceProvider,
                             JavaFxApplicationData applicationJavaFxApplicationData,
                             Runnable onActivatedHandler) {
        super(NavigationTarget.PRIMARY_STAGE);
        this.serviceProvider = serviceProvider;

        settingsService = this.serviceProvider.getSettingsService();
        userIdentityService = this.serviceProvider.getUserService().getUserIdentityService();
        this.onActivatedHandler = onActivatedHandler;

        model = new DesktopModel(serviceProvider.getConfig().getAppName());
        setInitialScreenSize();
        view = new DesktopView(model, this, applicationJavaFxApplicationData.getStage());

        splashController = new SplashController(applicationServiceState, this.serviceProvider);

        Browser.setHostServices(applicationJavaFxApplicationData.getHostServices());
        Transitions.setSettingsService(settingsService);
        AnchorPane viewRoot = view.getRoot();

        Navigation.init(settingsService);
        Overlay.init(viewRoot,
                serviceProvider.getConfig().getBaseDir(),
                settingsService,
                this::shutdown);

        // Here we start to attach the view hierarchy to the stage.
        view.showStage();

        new OverlayController(this.serviceProvider, viewRoot);
    }

    private void setInitialScreenSize() {
        Cookie cookie = serviceProvider.getSettingsService().getCookie();
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        model.setStageWidth(cookie.asDouble(CookieKey.STAGE_W)
                .orElse(Math.max(DesktopModel.MIN_WIDTH, Math.min(DesktopModel.PREF_WIDTH, screenBounds.getWidth()))));
        model.setStageHeight(cookie.asDouble(CookieKey.STAGE_H)
                .orElse(Math.max(DesktopModel.MIN_HEIGHT, Math.min(DesktopModel.PREF_HEIGHT, screenBounds.getHeight()))));
        model.setStageX(cookie.asDouble(CookieKey.STAGE_X)
                .orElse((screenBounds.getWidth() - model.getStageWidth()) / 2));
        model.setStageY(cookie.asDouble(CookieKey.STAGE_Y)
                .orElse((screenBounds.getHeight() - model.getStageHeight()) / 2));
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case SPLASH: {
                return Optional.of(splashController);
            }
            case MAIN: {
                return Optional.of(new MainController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onActivate() {
        // We show the splash screen as background also if we show the 'unlock' or 'tac' overlay screens
        Navigation.navigateTo(NavigationTarget.SPLASH);

        if (!settingsService.isTacAccepted()) {
            UIThread.runOnNextRenderFrame(() -> Navigation.navigateTo(NavigationTarget.TAC,
                    new TacController.InitData(this::maybeShowLockScreen)));
        } else {
            maybeShowLockScreen();
        }

        onActivatedHandler.run();
    }

    @Override
    public void onDeactivate() {
    }

    public void onApplicationServiceInitialized(boolean result, Throwable throwable) {
        if (throwable != null) {
            new Popup().error(throwable).show();
            return;
        }

        if (!result) {
            new Popup().warning("Initialising applicationService failed.").show();
            return;
        }

        model.setInitializeApplicationServiceCompleted(true);
        if (model.isUnlocked()) {
            applyNavigationTarget();
        } else {
            splashController.startAnimation();
        }
    }

    private void maybeShowLockScreen() {
        if (isLocked()) {
            // We delay to allow the splash screen to be displayed 
            UIThread.runOnNextRenderFrame(() -> Navigation.navigateTo(NavigationTarget.UNLOCK,
                    new UnlockController.InitData(this::onUnlocked)));
        } else {
            onUnlocked();
        }
    }

    private void onUnlocked() {
        model.setUnlocked(true);
        if (model.isInitializeApplicationServiceCompleted()) {
            applyNavigationTarget();
        } else {
            splashController.startAnimation();
        }
    }

    private void applyNavigationTarget() {
        splashController.stopAnimation();
        boolean hasUserIdentities = serviceProvider.getUserService().getUserIdentityService().hasUserIdentities();

        if (!hasUserIdentities) {
            if (DontShowAgainService.showAgain(WELCOME)) {
                Navigation.navigateTo(NavigationTarget.ONBOARDING_WELCOME);
            } else {
                Navigation.navigateTo(NavigationTarget.ONBOARDING_GENERATE_NYM);
            }
        } else {
            // After the domain is initialized we show the application content
            settingsService.getCookie().asString(CookieKey.NAVIGATION_TARGET)
                    .ifPresentOrElse(target -> {
                                try {
                                    NavigationTarget persisted = NavigationTarget.valueOf(target);
                                    Navigation.applyPersisted(persisted);
                                    Navigation.navigateTo(persisted);
                                } catch (Throwable t) {
                                    Navigation.navigateTo(NavigationTarget.DASHBOARD);
                                }
                            },
                            () -> Navigation.navigateTo(NavigationTarget.DASHBOARD));
        }
    }

    public void onQuit() {
        shutdown();
    }

    public void shutdown() {
        serviceProvider.getShotDownHandler().shutdown().thenAccept(result -> Platform.exit());
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

    public void onUncaughtException(Thread thread, Throwable throwable) {
        log.error("Uncaught exception from thread {}", thread);
        log.error("Uncaught exception", throwable);
        UIThread.run(() -> new Popup().error(throwable).show());
    }

    private boolean isLocked() {
        // todo add wallet support
        return userIdentityService.isDataStoreEncrypted();
    }
}
