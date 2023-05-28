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
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.main.MainController;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.tac.TacController;
import bisq.desktop.primary.overlay.unlock.UnlockController;
import bisq.desktop.primary.splash.SplashController;
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

import static bisq.settings.DontShowAgainKey.BISQ_2_INTRO;

@Slf4j
public class PrimaryStageController extends NavigationController {
    protected final DefaultApplicationService applicationService;
    @Getter
    protected final PrimaryStageModel model;
    @Getter
    protected final PrimaryStageView view;
    protected final SettingsService settingsService;
    protected final Runnable initDomainHandler;
    private final SplashController splashController;
    private final UserIdentityService userIdentityService;

    public PrimaryStageController(DefaultApplicationService applicationService,
                                  JavaFxApplicationData applicationJavaFxApplicationData,
                                  Runnable initDomainHandler) {
        super(NavigationTarget.PRIMARY_STAGE);

        this.applicationService = applicationService;
        settingsService = applicationService.getSettingsService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        this.initDomainHandler = initDomainHandler;

        model = new PrimaryStageModel(applicationService.getConfig().getAppName());
        setInitialScreenSize(applicationService);
        view = new PrimaryStageView(model, this, applicationJavaFxApplicationData.getStage());

        splashController = new SplashController(applicationService);

        Browser.setHostServices(applicationJavaFxApplicationData.getHostServices());
        Transitions.setSettingsService(settingsService);
        AnchorPane viewRoot = view.getRoot();

        Navigation.init(settingsService);
        Overlay.init(viewRoot,
                applicationService.getConfig().getBaseDir(),
                settingsService,
                this::shutdown);

        // Here we start to attach the view hierarchy to the stage.
        view.showStage();

        new OverlayController(applicationService, viewRoot);
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

    // Step 1: Maybe show locked screen
    @Override
    public void onActivate() {
        // We show the splash screen as background also if we show the 'unlock' or 'tac' overlay screens
        Navigation.navigateTo(NavigationTarget.SPLASH);

        if (isLocked()) {
            // We delay to allow the splash screen to be displayed 
            // After successful unlock we start loading persisted data and init of the domain
            UIThread.runOnNextRenderFrame(() -> Navigation.navigateTo(NavigationTarget.UNLOCK,
                    new UnlockController.InitData(this::initDomain)));
            return;
        }

        // We had no password protection so no storage files and wallets had been encrypted, and we can start reading
        // persisted data and init the domain
        initDomain();
    }

    // Step 2: We load persisted data and after that initialise the application service
    private void initDomain() {
        initDomainHandler.run();
    }

    @Override
    public void onDeactivate() {
    }

    // Step 3: Maybe show tac screen
    public void readAllPersistedCompleted(boolean result, Throwable throwable) {
        if (throwable != null) {
            new Popup().error(throwable).show();
            return;
        }

        if (!result) {
            new Popup().warning("Could not read persisted data.").show();
            return;
        }

        // Now we have the persisted data loaded and can continue with the tac screen if not yet accepted.
        if (isTacNotAccepted()) {
            UIThread.runOnNextRenderFrame(() -> Navigation.navigateTo(NavigationTarget.TAC,
                    new TacController.InitData(this::tacAccepted)));
        } else {
            splashController.startAnimation();
        }
    }

    // Step 4a: Initialize application service completed. Maybe apply navigation target
    public void initializeApplicationServiceCompleted(boolean result, Throwable throwable) {
        if (throwable != null) {
            new Popup().error(throwable).show();
            return;
        }

        if (!result) {
            new Popup().warning("Initialising applicationService failed.").show();
            return;
        }

        model.setInitializeApplicationServiceCompleted(true);
        maybeApplyNavigationTarget();
    }

    // Step 4b: Tac accepted, Maybe apply navigation target
    private void tacAccepted() {
        splashController.startAnimation();
        maybeApplyNavigationTarget();
    }

    // Step 5: If initializeApplicationServiceCompleted and tac accepted we apply navigation target
    private void maybeApplyNavigationTarget() {
        if (!model.isInitializeApplicationServiceCompleted() || isTacNotAccepted()) {
            return;
        }

        splashController.stopAnimation();
        boolean hasUserIdentities = applicationService.getUserService().getUserIdentityService().hasUserIdentities();

        if (!hasUserIdentities) {
            if (DontShowAgainService.showAgain(BISQ_2_INTRO)) {
                Navigation.navigateTo(NavigationTarget.ONBOARDING_BISQ_2_INTRO);
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
        applicationService.shutdown().thenAccept(result -> Platform.exit());
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

    private void setInitialScreenSize(DefaultApplicationService applicationService) {
        Cookie cookie = applicationService.getSettingsService().getCookie();
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        model.setStageWidth(cookie.asDouble(CookieKey.STAGE_W)
                .orElse(Math.max(PrimaryStageModel.MIN_WIDTH, Math.min(PrimaryStageModel.PREF_WIDTH, screenBounds.getWidth()))));
        model.setStageHeight(cookie.asDouble(CookieKey.STAGE_H)
                .orElse(Math.max(PrimaryStageModel.MIN_HEIGHT, Math.min(PrimaryStageModel.PREF_HEIGHT, screenBounds.getHeight()))));
        model.setStageX(cookie.asDouble(CookieKey.STAGE_X)
                .orElse((screenBounds.getWidth() - model.getStageWidth()) / 2));
        model.setStageY(cookie.asDouble(CookieKey.STAGE_Y)
                .orElse((screenBounds.getHeight() - model.getStageHeight()) / 2));
    }

    private boolean isLocked() {
        // todo add wallet support
        return userIdentityService.isDataStoreEncrypted();
    }

    private boolean isTacNotAccepted() {
        return !settingsService.isTacAccepted();
    }
}
