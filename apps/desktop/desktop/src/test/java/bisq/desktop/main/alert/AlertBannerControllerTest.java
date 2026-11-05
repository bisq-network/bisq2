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

package bisq.desktop.main.alert;

import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.desktop.testutil.JavaFxToolkitExtension.runOnFxThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Uses the real AlertNotificationsService, so an alert reaches the banner on the same path as in the app: the alerts
 * of the security manager in AlertService, then the unconsumed alerts of AlertNotificationsService, then the banner.
 * The alerts of the security manager change on a network thread, so the tests change them off the FX thread.
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class AlertBannerControllerTest {
    private final ObservableSet<AuthorizedAlertData> authorizedAlerts = new ObservableSet<>();
    private final Observable<Boolean> useAnimations = new Observable<>(false);
    private final AuthorizedAlertData haltAlert = alert("halt", AlertType.EMERGENCY, "Trading is halted", true);
    private final AuthorizedAlertData infoAlert = alert("info", AlertType.INFO, "Info", false);
    private AlertNotificationsService alertNotificationsService;
    private AlertBannerController controller;

    @BeforeEach
    void setUp() throws TimeoutException {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(useAnimations);
        when(settingsService.getConsumedAlertIds()).thenReturn(new ObservableSet<>());
        Transitions.setSettingsService(settingsService);

        AlertService alertService = mock(AlertService.class);
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(authorizedAlerts);
        alertNotificationsService = new AlertNotificationsService(settingsService, alertService, AppType.DESKTOP);
        alertNotificationsService.initialize().join();

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getAlertNotificationsService()).thenReturn(alertNotificationsService);
        when(serviceProvider.getBisqEasyService().getBisqEasyNotificationsService().getIsNotificationPanelVisible())
                .thenReturn(new Observable<>(false));

        runOnFxThread(() -> controller = new AlertBannerController(serviceProvider));
        // Attaching the view activates the controller and binds the labels to the model.
        FxToolkit.setupSceneRoot(() -> controller.getView().getRoot());
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        FxToolkit.setupSceneRoot(Pane::new);
        alertNotificationsService.shutdown().join();
        Transitions.setSettingsService(null);
    }

    @Test
    void bannerIsResetWhenTheSecurityManagerRemovesTheLastAlert() {
        authorizedAlerts.add(haltAlert);
        waitForFxEvents();
        assertThat(headline()).isEqualTo("Trading is halted");
        assertThat(isBannerVisible()).isTrue();

        authorizedAlerts.remove(haltAlert);
        waitForFxEvents();

        assertThat(headline()).isNullOrEmpty();
        assertThat(isBannerVisible()).isFalse();
    }

    @Test
    void closingTheBannerShowsTheNextAlertAndThenResetsIt() {
        authorizedAlerts.add(infoAlert);
        authorizedAlerts.add(haltAlert);
        waitForFxEvents();
        assertThat(headline()).isEqualTo("Trading is halted");

        runOnFxThread(controller::onClose);
        assertThat(headline()).isEqualTo("Info");
        assertThat(isBannerVisible()).isTrue();

        runOnFxThread(controller::onClose);
        assertThat(headline()).isNullOrEmpty();
        assertThat(isBannerVisible()).isFalse();
    }

    // With animations, the banner and its close button stay on screen while the banner slides out after a reset.
    @Test
    void closeClickedWhileTheBannerSlidesOutDoesNotThrow() {
        useAnimations.set(true);
        authorizedAlerts.add(haltAlert);
        waitForFxEvents();
        WaitForAsyncUtils.sleep(500, TimeUnit.MILLISECONDS); // Let the slide-in finish
        assertThat(isBannerVisible()).isTrue();

        authorizedAlerts.remove(haltAlert);
        waitForFxEvents();
        assertThat(isBannerVisible()).isTrue(); // Still sliding out

        AtomicReference<Throwable> thrown = new AtomicReference<>();
        runOnFxThread(() -> {
            try {
                controller.onClose();
            } catch (Throwable t) {
                thrown.set(t);
            }
        });

        assertThat(thrown.get()).isNull();
    }

    private String headline() {
        return controller.getView().headline.getText();
    }

    private boolean isBannerVisible() {
        return controller.getView().getRoot().isVisible();
    }

    private static AuthorizedAlertData alert(String id, AlertType alertType, String headline, boolean haltTrading) {
        return new AuthorizedAlertData(id,
                System.currentTimeMillis(),
                alertType,
                Optional.of(headline),
                Optional.of("Message"),
                haltTrading,
                false,
                Optional.empty(),
                Optional.empty(),
                "0123456789abcdef0123456789abcdef01234567",
                false,
                Optional.empty(),
                AppType.DESKTOP);
    }
}
