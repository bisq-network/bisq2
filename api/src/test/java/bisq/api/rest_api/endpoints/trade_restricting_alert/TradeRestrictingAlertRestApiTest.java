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

package bisq.api.rest_api.endpoints.trade_restricting_alert;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.observable.collection.ObservableSet;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeRestrictingAlertRestApiTest {

    @Test
    void getTradeRestrictingAlertReturnsNoContentWhenNoActiveAlert() {
        AlertService alertService = mock(AlertService.class);
        ObservableSet<AuthorizedAlertData> alerts = new ObservableSet<>();
        alerts.add(createAlert("mobile-info", AppType.MOBILE_CLIENT, 10L, AlertType.INFO, false, false));
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(alerts);

        TradeRestrictingAlertRestApi restApi = new TradeRestrictingAlertRestApi(alertService);

        Response response = restApi.getTradeRestrictingAlert("MOBILE_CLIENT");

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void getTradeRestrictingAlertPrefersHaltTradingOverRequireVersionEvenWhenOlder() {
        AlertService alertService = mock(AlertService.class);
        ObservableSet<AuthorizedAlertData> alerts = new ObservableSet<>();
        // halt-old is older but halt-trading takes precedence over require-version
        alerts.add(createAlert("halt-old", AppType.MOBILE_CLIENT, 10L, AlertType.EMERGENCY, true, false));
        alerts.add(createAlert("require-new", AppType.MOBILE_CLIENT, 60L, AlertType.EMERGENCY, false, true, Optional.of("2.1.5")));
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(alerts);

        TradeRestrictingAlertRestApi restApi = new TradeRestrictingAlertRestApi(alertService);

        Response response = restApi.getTradeRestrictingAlert("MOBILE_CLIENT");

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthorizedAlertDataDto alert = (AuthorizedAlertDataDto) response.getEntity();
        assertThat(alert.id()).isEqualTo("halt-old");
        assertThat(alert.haltTrading()).isTrue();
    }

    @Test
    void getTradeRestrictingAlertReturnsOnlyMostRecentHaltWhenMultiplePresent() {
        AlertService alertService = mock(AlertService.class);
        ObservableSet<AuthorizedAlertData> alerts = new ObservableSet<>();
        alerts.add(createAlert("halt-old", AppType.MOBILE_CLIENT, 10L, AlertType.EMERGENCY, true, false));
        alerts.add(createAlert("halt-new", AppType.MOBILE_CLIENT, 50L, AlertType.EMERGENCY, true, false));
        alerts.add(createAlert("require-latest", AppType.MOBILE_CLIENT, 60L, AlertType.EMERGENCY, false, true, Optional.of("2.1.5")));
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(alerts);

        TradeRestrictingAlertRestApi restApi = new TradeRestrictingAlertRestApi(alertService);

        Response response = restApi.getTradeRestrictingAlert("MOBILE_CLIENT");

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthorizedAlertDataDto alert = (AuthorizedAlertDataDto) response.getEntity();
        assertThat(alert.id()).isEqualTo("halt-new");
    }

    @Test
    void getTradeRestrictingAlertReturnsRequireVersionWhenNoHaltPresent() {
        AlertService alertService = mock(AlertService.class);
        ObservableSet<AuthorizedAlertData> alerts = new ObservableSet<>();
        alerts.add(createAlert("require-old", AppType.MOBILE_CLIENT, 10L, AlertType.EMERGENCY, false, true, Optional.of("2.1.0")));
        alerts.add(createAlert("require-new", AppType.MOBILE_CLIENT, 60L, AlertType.EMERGENCY, false, true, Optional.of("2.1.5")));
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(alerts);

        TradeRestrictingAlertRestApi restApi = new TradeRestrictingAlertRestApi(alertService);

        Response response = restApi.getTradeRestrictingAlert("MOBILE_CLIENT");

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AuthorizedAlertDataDto alert = (AuthorizedAlertDataDto) response.getEntity();
        assertThat(alert.id()).isEqualTo("require-new");
        assertThat(alert.requireVersionForTrading()).isTrue();
    }

    @Test
    void getTradeRestrictingAlertReturnsBadRequestForInvalidAppType() {
        AlertService alertService = mock(AlertService.class);
        TradeRestrictingAlertRestApi restApi = new TradeRestrictingAlertRestApi(alertService);

        Response response = restApi.getTradeRestrictingAlert("invalid");

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(Map.of("error", "Invalid appType: invalid"));
    }

    private AuthorizedAlertData createAlert(String id,
                                            AppType appType,
                                            long date,
                                            AlertType alertType,
                                            boolean haltTrading,
                                            boolean requireVersionForTrading) {
        return createAlert(id,
                appType,
                date,
                alertType,
                haltTrading,
                requireVersionForTrading,
                requireVersionForTrading ? Optional.of("2.1.9") : Optional.of("2.1.8"));
    }

    private AuthorizedAlertData createAlert(String id,
                                            AppType appType,
                                            long date,
                                            AlertType alertType,
                                            boolean haltTrading,
                                            boolean requireVersionForTrading,
                                            Optional<String> minVersion) {
        return new AuthorizedAlertData(
                id,
                System.currentTimeMillis() + date,
                alertType,
                AlertType.isMessageAlert(alertType) ? Optional.of("Headline " + id) : Optional.empty(),
                Optional.of("Message " + id),
                haltTrading,
                requireVersionForTrading,
                minVersion,
                Optional.empty(),
                "0123456789abcdef0123456789abcdef01234567",
                false,
                Optional.empty(),
                appType
        );
    }
}

