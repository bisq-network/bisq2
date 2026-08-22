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

package bisq.api.rest_api.endpoints.alert_notifications;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AlertNotificationsRestApiTest {

    @Test
    void dismissAlertUsesRequestedAppType() {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AuthorizedAlertData desktopAlert = createAlert("desktop-alert", AppType.DESKTOP, 20L, AlertType.INFO);
        when(alertNotificationsService.getUnconsumedAlertsByAppType(AppType.DESKTOP))
                .thenReturn(Stream.of(desktopAlert));

        AlertNotificationsRestApi restApi = new AlertNotificationsRestApi(alertNotificationsService);

        Response response = restApi.dismissAlert("desktop-alert", "desktop");

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(alertNotificationsService).getUnconsumedAlertsByAppType(AppType.DESKTOP);
        verify(alertNotificationsService).dismissAlert(desktopAlert);
    }

    @Test
    void dismissAlertReturnsBadRequestForInvalidAppType() {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AlertNotificationsRestApi restApi = new AlertNotificationsRestApi(alertNotificationsService);

        Response response = restApi.dismissAlert("alert-id", "foo");

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(Map.of("error", "Invalid appType: foo"));
        verifyNoInteractions(alertNotificationsService);
    }

    @Test
    void dismissAlertReturnsNotFoundWhenAlertDoesNotExistForRequestedAppType() {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        when(alertNotificationsService.getUnconsumedAlertsByAppType(AppType.DESKTOP))
                .thenReturn(Stream.empty());

        AlertNotificationsRestApi restApi = new AlertNotificationsRestApi(alertNotificationsService);

        Response response = restApi.dismissAlert("missing-alert", "desktop");

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        verify(alertNotificationsService).getUnconsumedAlertsByAppType(AppType.DESKTOP);
        verify(alertNotificationsService, never()).dismissAlert(any());
    }

    @Test
    void getAlertNotificationsReturnsBadRequestForInvalidAppType() {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AlertNotificationsRestApi restApi = new AlertNotificationsRestApi(alertNotificationsService);

        Response response = restApi.getAlertNotifications("invalid");

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(Map.of("error", "Invalid appType: invalid"));
    }

    @Test
    void getAlertNotificationsSkipsUnsupportedAlertTypes() {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AuthorizedAlertData infoAlert = createAlert("info-alert", AppType.MOBILE_CLIENT, 10L, AlertType.INFO);
        AuthorizedAlertData banAlert = createAlert("ban-alert", AppType.MOBILE_CLIENT, 30L, AlertType.BAN);
        AuthorizedAlertData bannedAccountDataAlert = createAlert("bad-account-alert", AppType.MOBILE_CLIENT, 20L, AlertType.BANNED_ACCOUNT_DATA);
        when(alertNotificationsService.getUnconsumedAlertsByAppType(AppType.MOBILE_CLIENT))
                .thenReturn(Stream.of(infoAlert, banAlert, bannedAccountDataAlert));

        AlertNotificationsRestApi restApi = new AlertNotificationsRestApi(alertNotificationsService);

        Response response = restApi.getAlertNotifications("mobile_client");

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<AuthorizedAlertDataDto> authorizedAlerts = (List<AuthorizedAlertDataDto>) response.getEntity();
        assertThat(authorizedAlerts)
                .hasSize(1)
                .allSatisfy(alert -> assertThat(alert.alertType()).isEqualTo(AlertType.INFO));
    }

    private AuthorizedAlertData createAlert(String id, AppType appType, long date, AlertType alertType) {
        return new AuthorizedAlertData(
                id,
                System.currentTimeMillis() + date,
                alertType,
                AlertType.isMessageAlert(alertType) ? Optional.of("Headline " + id) : Optional.empty(),
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