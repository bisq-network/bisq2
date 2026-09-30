package bisq.api.web_socket.domain.alert_notifications;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.json.JsonMapperProvider;
import bisq.common.observable.collection.ObservableSet;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertNotificationsWebSocketServiceTest {

    @Test
    void getJsonPayloadUsesSubscriberAppTypeParameter() throws Exception {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AuthorizedAlertData desktopAlert = createAlert("desktop-alert", AppType.DESKTOP, 10L, AlertType.INFO);
        when(alertNotificationsService.getUnconsumedAlertsByAppType(AppType.DESKTOP))
                .thenReturn(Stream.of(desktopAlert));

        AlertNotificationsWebSocketService service = new AlertNotificationsWebSocketService(new SubscriberRepository(), alertNotificationsService);

        String json = service.getJsonPayload(Optional.of("desktop")).orElseThrow();

        List<AuthorizedAlertDataDto> payload = JsonMapperProvider.get().readerForListOf(AuthorizedAlertDataDto.class).readValue(json);
        assertThat(payload).hasSize(1);
        assertThat(payload.getFirst().appType()).isEqualTo(AppType.DESKTOP);
        verify(alertNotificationsService).getUnconsumedAlertsByAppType(AppType.DESKTOP);
    }

    @Test
    void getJsonPayloadRejectsInvalidAppType() {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AlertNotificationsWebSocketService service = new AlertNotificationsWebSocketService(new SubscriberRepository(), alertNotificationsService);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.getJsonPayload(Optional.of("invalid")));

        assertThat(exception).hasMessage("Invalid appType: invalid");
    }

    @Test
    void getJsonPayloadSkipsUnsupportedAlertTypes() throws Exception {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AuthorizedAlertData infoAlert = createAlert("info-alert", AppType.MOBILE_CLIENT, 10L, AlertType.INFO);
        AuthorizedAlertData banAlert = createAlert("ban-alert", AppType.MOBILE_CLIENT, 30L, AlertType.BAN);
        AuthorizedAlertData bannedAccountDataAlert = createAlert("bad-account-alert", AppType.MOBILE_CLIENT, 20L, AlertType.BANNED_ACCOUNT_DATA);
        when(alertNotificationsService.getUnconsumedAlertsByAppType(AppType.MOBILE_CLIENT))
                .thenReturn(Stream.of(infoAlert, banAlert, bannedAccountDataAlert));

        AlertNotificationsWebSocketService service = new AlertNotificationsWebSocketService(new SubscriberRepository(), alertNotificationsService);

        String json = service.getJsonPayload(Optional.of("mobile_client")).orElseThrow();

        List<AuthorizedAlertDataDto> payload = JsonMapperProvider.get().readerForListOf(AuthorizedAlertDataDto.class).readValue(json);
        assertThat(payload)
                .hasSize(1)
                .allSatisfy(alert -> assertThat(alert.alertType()).isEqualTo(AlertType.INFO));
    }

    @Test
    void canonicalizeParameterNormalizesToCanonicalAppTypeName() {
        AlertNotificationsWebSocketService service = new AlertNotificationsWebSocketService(
                new SubscriberRepository(), mock(AlertNotificationsService.class));

        assertThat(service.canonicalizeParameter(Optional.of("mobile_client"))).isEqualTo(Optional.of("MOBILE_CLIENT"));
        assertThat(service.canonicalizeParameter(Optional.of("Mobile_Client"))).isEqualTo(Optional.of("MOBILE_CLIENT"));
        assertThat(service.canonicalizeParameter(Optional.of("MOBILE_CLIENT"))).isEqualTo(Optional.of("MOBILE_CLIENT"));
        assertThat(service.canonicalizeParameter(Optional.of("desktop"))).isEqualTo(Optional.of("DESKTOP"));
        assertThat(service.canonicalizeParameter(Optional.of("DESKTOP"))).isEqualTo(Optional.of("DESKTOP"));
    }

    @Test
    void onChangeQueriesOncePerDistinctAppType() {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        ObservableSet<AuthorizedAlertData> unconsumedAlerts = new ObservableSet<>();
        when(alertNotificationsService.getUnconsumedAlerts()).thenReturn(unconsumedAlerts);
        // Use thenAnswer so each call receives a fresh Stream instance
        when(alertNotificationsService.getUnconsumedAlertsByAppType(any(AppType.class)))
                .thenAnswer(inv -> Stream.empty());

        SubscriberRepository subscriberRepository = new SubscriberRepository();
        AlertNotificationsWebSocketService service = new AlertNotificationsWebSocketService(subscriberRepository, alertNotificationsService);
        service.initialize();

        // 2 DESKTOP subscribers + 1 MOBILE_CLIENT subscriber (3 total, 2 distinct AppTypes)
        SubscriptionRequest r1 = subscriptionRequestWithParameter("r1", "DESKTOP");
        SubscriptionRequest r2 = subscriptionRequestWithParameter("r2", "DESKTOP");
        SubscriptionRequest r3 = subscriptionRequestWithParameter("r3", "MOBILE_CLIENT");
        subscriberRepository.add(r1, Optional.of("DESKTOP"), mock(WebSocket.class, RETURNS_DEEP_STUBS));
        subscriberRepository.add(r2, Optional.of("DESKTOP"), mock(WebSocket.class, RETURNS_DEEP_STUBS));
        subscriberRepository.add(r3, Optional.of("MOBILE_CLIENT"), mock(WebSocket.class, RETURNS_DEEP_STUBS));

        // Trigger onChange via the underlying ObservableSet
        unconsumedAlerts.add(createAlert("any", AppType.DESKTOP, 0L, AlertType.INFO));

        // Domain query must run once per distinct AppType, not once per subscriber
        verify(alertNotificationsService, times(1)).getUnconsumedAlertsByAppType(AppType.DESKTOP);
        verify(alertNotificationsService, times(1)).getUnconsumedAlertsByAppType(AppType.MOBILE_CLIENT);
    }

    private SubscriptionRequest subscriptionRequestWithParameter(String requestId, String parameter) {
        String json = "{\"type\":\"SubscriptionRequest\",\"requestId\":\"" + requestId
                + "\",\"topic\":\"ALERT_NOTIFICATIONS\",\"parameter\":\"" + parameter + "\"}";
        return SubscriptionRequest.fromJson(json).orElseThrow();
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