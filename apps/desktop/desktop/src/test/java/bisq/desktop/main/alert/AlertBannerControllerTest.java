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

import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
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

import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static bisq.desktop.testutil.JavaFxToolkitExtension.runOnFxThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class AlertBannerControllerTest {
    private final ObservableSet<AuthorizedAlertData> unconsumedAlerts = new ObservableSet<>();
    private final AuthorizedAlertData emergencyAlert = alert(AlertType.EMERGENCY, "Trading is halted");
    private final AuthorizedAlertData infoAlert = alert(AlertType.INFO, "Info");
    private AlertBannerController controller;

    @BeforeEach
    void setUp() throws TimeoutException {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);

        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        when(alertNotificationsService.getUnconsumedAlerts()).thenReturn(unconsumedAlerts);
        doAnswer(invocation -> unconsumedAlerts.remove(invocation.<AuthorizedAlertData>getArgument(0)))
                .when(alertNotificationsService).dismissAlert(any());

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
        Transitions.setSettingsService(null);
    }

    @Test
    void bannerIsResetWhenTheLastAlertIsRemoved() {
        unconsumedAlerts.add(emergencyAlert);
        waitForFxEvents();
        assertThat(controller.getView().headline.getText()).isEqualTo("Trading is halted");
        assertThat(controller.getView().getRoot().isVisible()).isTrue();

        unconsumedAlerts.remove(emergencyAlert);
        waitForFxEvents();

        assertThat(controller.getView().headline.getText()).isNullOrEmpty();
        assertThat(controller.getView().getRoot().isVisible()).isFalse();
    }

    @Test
    void closingTheBannerShowsTheNextAlertAndThenResetsIt() {
        unconsumedAlerts.add(infoAlert);
        unconsumedAlerts.add(emergencyAlert);
        waitForFxEvents();
        assertThat(controller.getView().headline.getText()).isEqualTo("Trading is halted");

        runOnFxThread(controller::onClose);
        assertThat(controller.getView().headline.getText()).isEqualTo("Info");
        assertThat(controller.getView().getRoot().isVisible()).isTrue();

        runOnFxThread(controller::onClose);
        assertThat(controller.getView().headline.getText()).isNullOrEmpty();
        assertThat(controller.getView().getRoot().isVisible()).isFalse();
    }

    private static AuthorizedAlertData alert(AlertType alertType, String headline) {
        AuthorizedAlertData alert = mock(AuthorizedAlertData.class);
        when(alert.getAlertType()).thenReturn(alertType);
        when(alert.getDate()).thenReturn(1_700_000_000_000L);
        when(alert.getHeadline()).thenReturn(Optional.of(headline));
        when(alert.getMessage()).thenReturn(Optional.of("Message"));
        return alert;
    }
}
