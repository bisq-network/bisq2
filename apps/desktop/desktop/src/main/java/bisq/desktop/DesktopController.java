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

import bisq.application.State;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.application.JavaFxApplicationData;
import bisq.desktop.common.standby.PreventStandbyModeService;
import bisq.desktop.common.threading.UIClock;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.cathash.JavaFxCatHashService;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.MainController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.tac.TacController;
import bisq.desktop.overlay.unlock.UnlockController;
import bisq.desktop.splash.SplashController;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.presentation.formatters.DateFormatter;
import bisq.settings.Cookie;
import bisq.settings.CookieKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import bisq.user.RepublishUserProfileService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    protected DesktopModel model;
    @Getter
    protected DesktopView view;
    protected final SettingsService settingsService;
    protected final Runnable onActivatedHandler;
    private SplashController splashController;
    private final UserIdentityService userIdentityService;
    private final ChatNotificationService chatNotificationService;
    private final ServiceProvider serviceProvider;
    private final DontShowAgainService dontShowAgainService;
    private final PreventStandbyModeService preventStandbyModeService;
    private final RepublishUserProfileService republishUserProfileService;
    private final IdentityService identityService;

    private final Observable<State> applicationServiceState;
    private final JavaFxApplicationData applicationJavaFxApplicationData;
    private Pin referenceTimePin;
    private boolean systemClockDriftWarningDisplayed;

    public DesktopController(Observable<State> applicationServiceState,
                             ServiceProvider serviceProvider,
                             JavaFxApplicationData applicationJavaFxApplicationData,
                             Runnable onActivatedHandler) {
        super(NavigationTarget.PRIMARY_STAGE);

        this.applicationServiceState = applicationServiceState;
        this.applicationJavaFxApplicationData = applicationJavaFxApplicationData;
        this.serviceProvider = serviceProvider;
        this.onActivatedHandler = onActivatedHandler;

        settingsService = serviceProvider.getSettingsService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        dontShowAgainService = serviceProvider.getDontShowAgainService();
        preventStandbyModeService = new PreventStandbyModeService(serviceProvider);
        republishUserProfileService = serviceProvider.getUserService().getRepublishUserProfileService();
        identityService = serviceProvider.getIdentityService();
    }

    public void init() {
        model = new DesktopModel(serviceProvider.getConfig().getAppName());
        setInitialScreenSize();
        view = new DesktopView(model, this, applicationJavaFxApplicationData.getStage());

        splashController = new SplashController(applicationServiceState, serviceProvider);

        Browser.initialize(applicationJavaFxApplicationData.getHostServices(), settingsService, dontShowAgainService);
        Transitions.setSettingsService(settingsService);
        AnchorPane viewRoot = view.getRoot();

        CatHash.setDelegate(new JavaFxCatHashService(serviceProvider.getConfig().getAppDataDirPath()));

        Navigation.init(settingsService);
        Overlay.init(serviceProvider, viewRoot);
        serviceProvider.getShutDownHandler().addShutDownHook(this::onShutdown);
        UIClock.initialize();

        // Here we start to attach the view hierarchy to the stage.
        view.showStage();

        new OverlayController(serviceProvider, viewRoot);

        identityService.getFatalException().addObserver(exception -> {
            if (exception != null) {
                UIThread.run(() -> new Popup().error(exception).hideCloseButton().useShutDownButton().show());
            }
        });

        EasyBind.subscribe(viewRoot.getScene().getWindow().focusedProperty(), chatNotificationService::setApplicationFocussed);

        // Prune user profile icons
        Map<String, UserProfile> userProfileById = serviceProvider.getUserService().getUserProfileService().getUserProfileById().getUnmodifiableMap();
        CatHash.pruneOutdatedProfileIcons(userProfileById.values());
    }

    private void setInitialScreenSize() {
        Cookie cookie = serviceProvider.getSettingsService().getCookie();
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        model.setStageWidth(cookie.asDouble(CookieKey.STAGE_W)
                .orElseGet(() -> Math.max(DesktopModel.MIN_WIDTH, Math.min(DesktopModel.PREF_WIDTH, screenBounds.getWidth()))));
        model.setStageHeight(cookie.asDouble(CookieKey.STAGE_H)
                .orElseGet(() -> Math.max(DesktopModel.MIN_HEIGHT, Math.min(DesktopModel.PREF_HEIGHT, screenBounds.getHeight()))));
        model.setStageX(cookie.asDouble(CookieKey.STAGE_X)
                .orElseGet(() -> (screenBounds.getWidth() - model.getStageWidth()) / 2));
        model.setStageY(cookie.asDouble(CookieKey.STAGE_Y)
                .orElseGet(() -> (screenBounds.getHeight() - model.getStageHeight()) / 2));
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case SPLASH -> Optional.of(splashController);
            case MAIN -> Optional.of(new MainController(serviceProvider));
            default -> Optional.empty();
        };
    }

    @Override
    public void onActivate() {
        preventStandbyModeService.initialize();
        // We show the splash screen as background also if we show the 'unlock' or 'tac' overlay screens
        Navigation.navigateTo(NavigationTarget.SPLASH);

        if (!settingsService.getIsTacAccepted().get()) {
            UIThread.runOnNextRenderFrame(() -> Navigation.navigateTo(NavigationTarget.TAC,
                    new TacController.InitData(this::maybeShowLockScreen)));
        } else {
            maybeShowLockScreen();
        }

        Scene scene = view.getRoot().getScene();
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> republishUserProfileService.userActivityDetected());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> republishUserProfileService.userActivityDetected());

        onActivatedHandler.run();

        referenceTimePin = serviceProvider.getNetworkService().getReferenceTime().addObserver(referenceTime -> {
            if (referenceTime != null) {
                UIThread.run(() -> {
                    long systemTime = System.currentTimeMillis();
                    String formattedSystemTime = DateFormatter.formatDateTime(new Date(systemTime));
                    String formattedReferenceTime = DateFormatter.formatDateTime(new Date(referenceTime));
                    long drift = Math.abs(systemTime - referenceTime);
                    if (drift > TimeUnit.HOURS.toMillis(1) && !systemClockDriftWarningDisplayed) {
                        systemClockDriftWarningDisplayed = true;
                        new Popup().headline(Res.get("referenceTimeService.systemClockDrift.popup.headline"))
                                .warning(Res.get("referenceTimeService.systemClockDrift.popup.message", formattedSystemTime, formattedReferenceTime))
                                .show();
                    } else {
                        log.info("System clock is in tolerance range with reference time received from explorer node. " +
                                        "System clock: {}; network reference time: {} ",
                                formattedSystemTime, formattedReferenceTime);
                    }
                });
            }
        });
    }

    @Override
    public void onDeactivate() {
        referenceTimePin.unbind();
        UIClock.shutdown();
    }

    public void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        if (throwable != null) {
            new Popup().error(throwable).show();
            return;
        }

        if (result == null || !result) {
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

    public void onUncaughtException(Thread thread, Throwable throwable) {
        log.error("Uncaught exception from thread {}", thread);
        log.error("Uncaught exception:", throwable);
        if (throwable instanceof UnsupportedOperationException &&
                throwable.getMessage() != null &&
                throwable.getMessage().contains("system tray")) {
            // User with Ubuntu 24.04.1 LTS / Wayland reported an UnsupportedOperationException, despite we use the
            // SystemTray only in AwtNotificationService which is used only for Windows and the usage there is covered
            // by a try/catch. It is unclear from where it gets called as the stacktrace does not expose the path to a
            // Bisq source code caller.
            // See https://github.com/bisq-network/bisq2/issues/2832
            log.error("System tray not supported", throwable);
            return;
        }
        UIThread.run(() -> new Popup().error(throwable).show());
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
            if (dontShowAgainService.showAgain(WELCOME)) {
                Navigation.navigateTo(NavigationTarget.ONBOARDING_WELCOME);
            } else {
                Navigation.navigateTo(NavigationTarget.ONBOARDING_GENERATE_NYM);
            }
        } else {
            // After the domain is initialized we show the application content
            OverlayController.getInstance().setUseEscapeKeyHandler(true);
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

    void onQuit() {
        serviceProvider.getShutDownHandler().shutdown();
    }

    void onStageXChanged(double value) {
        settingsService.setCookie(CookieKey.STAGE_X, value);
    }

    void onStageYChanged(double value) {
        settingsService.setCookie(CookieKey.STAGE_Y, value);
    }

    void onStageWidthChanged(double value) {
        settingsService.setCookie(CookieKey.STAGE_W, value);
    }

    void onStageHeightChanged(double value) {
        settingsService.setCookie(CookieKey.STAGE_H, value);
    }

    private void onShutdown() {
        preventStandbyModeService.shutdown();
    }

    private boolean isLocked() {
        // todo (deferred) add wallet support
        return userIdentityService.isDataStoreEncrypted();
    }
}
