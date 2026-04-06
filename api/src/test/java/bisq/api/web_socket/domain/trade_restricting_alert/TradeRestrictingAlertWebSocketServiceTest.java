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

package bisq.api.web_socket.domain.trade_restricting_alert;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.json.JsonMapperProvider;
import bisq.common.observable.collection.ObservableSet;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeRestrictingAlertWebSocketServiceTest {

    @Test
    void getJsonPayloadUsesSubscriberAppTypeParameterAndFiltersOutNonLimitingAlerts() throws Exception {
        AlertService alertService = mock(AlertService.class);
        ObservableSet<AuthorizedAlertData> alerts = new ObservableSet<>();
        alerts.add(createAlert("desktop-info", AppType.DESKTOP, 10L, AlertType.INFO, false, false));
        alerts.add(createAlert("desktop-halt", AppType.DESKTOP, 20L, AlertType.EMERGENCY, true, false));
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(alerts);

        TradeRestrictingAlertWebSocketService service = new TradeRestrictingAlertWebSocketService(new SubscriberRepository(), alertService);

        String json = service.getJsonPayload(Optional.of("desktop")).orElseThrow();

        AuthorizedAlertDataDto payload = JsonMapperProvider.get().readValue(json, AuthorizedAlertDataDto.class);
        assertThat(payload.id()).isEqualTo("desktop-halt");
        assertThat(payload.appType()).isEqualTo(AppType.DESKTOP);
        assertThat(payload.haltTrading()).isTrue();
    }

    @Test
    void getJsonPayloadThrowsWhenRequestParameterMissing() {
        AlertService alertService = mock(AlertService.class);
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(new ObservableSet<>());

        TradeRestrictingAlertWebSocketService service = new TradeRestrictingAlertWebSocketService(new SubscriberRepository(), alertService);

        assertThrows(IllegalArgumentException.class, () -> service.getJsonPayload(Optional.empty()));
    }

    @Test
    void getJsonPayloadPrefersHaltTradingOverRequireVersionEvenWhenOlder() throws Exception {
        AlertService alertService = mock(AlertService.class);
        ObservableSet<AuthorizedAlertData> alerts = new ObservableSet<>();
        alerts.add(createAlert("halt-old", AppType.MOBILE_CLIENT, 10L, AlertType.EMERGENCY, true, false));
        alerts.add(createAlert("require-latest", AppType.MOBILE_CLIENT, 60L, AlertType.EMERGENCY, false, true, Optional.of("2.1.5")));
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(alerts);

        TradeRestrictingAlertWebSocketService service = new TradeRestrictingAlertWebSocketService(new SubscriberRepository(), alertService);

        String json = service.getJsonPayload(Optional.of("MOBILE_CLIENT")).orElseThrow();

        AuthorizedAlertDataDto payload = JsonMapperProvider.get().readValue(json, AuthorizedAlertDataDto.class);
        assertThat(payload.id()).isEqualTo("halt-old");
        assertThat(payload.haltTrading()).isTrue();
    }

    @Test
    void getJsonPayloadReturnsEmptyWhenNoTradeRestrictingAlertPresent() {
        AlertService alertService = mock(AlertService.class);
        ObservableSet<AuthorizedAlertData> alerts = new ObservableSet<>();
        alerts.add(createAlert("desktop-info", AppType.DESKTOP, 10L, AlertType.INFO, false, false));
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(alerts);

        TradeRestrictingAlertWebSocketService service = new TradeRestrictingAlertWebSocketService(new SubscriberRepository(), alertService);

        Optional<String> json = service.getJsonPayload(Optional.of("desktop"));

        assertThat(json).isEmpty();
    }

    @Test
    void getJsonPayloadRejectsInvalidAppType() {
        AlertService alertService = mock(AlertService.class);
        when(alertService.getAuthorizedAlertDataSet()).thenReturn(new ObservableSet<>());
        TradeRestrictingAlertWebSocketService service = new TradeRestrictingAlertWebSocketService(new SubscriberRepository(), alertService);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getJsonPayload(Optional.of("invalid")));

        assertThat(exception).hasMessage("Invalid appType: invalid");
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
