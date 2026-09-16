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

package bisq.bonded_roles.security_manager.alert;

import bisq.bonded_roles.release.AppType;
import bisq.common.observable.collection.ObservableSet;
import bisq.settings.SettingsService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertNotificationsServiceTest {

    @Test
    void initializeWithUnspecifiedAppTypeCollectsAlertsForAllAppTypes() {
        ObservableSet<AuthorizedAlertData> authorizedAlerts = new ObservableSet<>();
        ObservableSet<String> consumedAlertIds = new ObservableSet<>();
        SettingsService settingsService = mock(SettingsService.class);
        AlertService alertService = mock(AlertService.class);
        when(settingsService.getConsumedAlertIds()).thenReturn(consumedAlertIds);
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(authorizedAlerts);

        AlertNotificationsService service = new AlertNotificationsService(settingsService, alertService, AppType.UNSPECIFIED);
        service.initialize().join();

        AuthorizedAlertData desktopAlert = createAlert("desktop-alert", AppType.DESKTOP, 1L);
        AuthorizedAlertData mobileAlert = createAlert("mobile-alert", AppType.MOBILE_CLIENT, 2L);
        AuthorizedAlertData legacyAlert = createAlert("legacy-alert", AppType.UNSPECIFIED, 3L);

        authorizedAlerts.add(desktopAlert);
        authorizedAlerts.add(mobileAlert);
        authorizedAlerts.add(legacyAlert);

        assertThat(service.getUnconsumedAlerts()).containsExactlyInAnyOrder(desktopAlert, mobileAlert, legacyAlert);
        assertThat(service.getUnconsumedAlertsByAppType(AppType.DESKTOP)).containsExactlyInAnyOrder(desktopAlert, legacyAlert);
        assertThat(service.getUnconsumedAlertsByAppType(AppType.MOBILE_CLIENT)).containsExactlyInAnyOrder(mobileAlert, legacyAlert);
    }

    @Test
    void initializeWithSpecificAppTypeCollectsOnlyMatchingAndLegacyAlerts() {
        ObservableSet<AuthorizedAlertData> authorizedAlerts = new ObservableSet<>();
        ObservableSet<String> consumedAlertIds = new ObservableSet<>();
        SettingsService settingsService = mock(SettingsService.class);
        AlertService alertService = mock(AlertService.class);
        when(settingsService.getConsumedAlertIds()).thenReturn(consumedAlertIds);
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(authorizedAlerts);

        AlertNotificationsService service = new AlertNotificationsService(settingsService, alertService, AppType.DESKTOP);

        AuthorizedAlertData desktopAlert = createAlert("desktop-alert", AppType.DESKTOP, 1L);
        AuthorizedAlertData mobileAlert = createAlert("mobile-alert", AppType.MOBILE_CLIENT, 2L);
        AuthorizedAlertData legacyAlert = createAlert("legacy-alert", AppType.UNSPECIFIED, 3L);

        authorizedAlerts.add(desktopAlert);
        authorizedAlerts.add(mobileAlert);
        authorizedAlerts.add(legacyAlert);

        service.initialize().join();

        assertThat(service.getUnconsumedAlerts()).containsExactlyInAnyOrder(desktopAlert, legacyAlert);
        assertThat(service.getUnconsumedAlerts()).doesNotContain(mobileAlert);
    }

    private AuthorizedAlertData createAlert(String id, AppType appType, long date) {
        return new AuthorizedAlertData(
                id,
                System.currentTimeMillis() + date,
                AlertType.INFO,
                Optional.of("Headline " + id),
                Optional.of("Message " + id),
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                "0123456789abcdef0123456789abcdef01234567",
                false,
                Optional.empty(),
                appType
        );
    }
}