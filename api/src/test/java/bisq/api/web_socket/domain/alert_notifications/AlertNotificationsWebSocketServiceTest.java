package bisq.api.web_socket.domain.alert_notifications;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.api.web_socket.subscription.Topic;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.json.JsonMapperProvider;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
        Subscriber subscriber = new Subscriber(Topic.ALERT_NOTIFICATIONS, Optional.of("desktop"), "sub-1", mock(WebSocket.class));

        String json = service.getJsonPayload(subscriber).orElseThrow();

        List<AuthorizedAlertDataDto> payload = JsonMapperProvider.get().readerForListOf(AuthorizedAlertDataDto.class).readValue(json);
        assertThat(payload).hasSize(1);
        assertThat(payload.getFirst().appType()).isEqualTo(AppType.DESKTOP);
        verify(alertNotificationsService).getUnconsumedAlertsByAppType(AppType.DESKTOP);
    }

    @Test
    void getJsonPayloadDefaultsToMobileClientWhenRequestParameterMissing() throws Exception {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AuthorizedAlertData mobileAlert = createAlert("mobile-alert", AppType.MOBILE_CLIENT, 20L, AlertType.INFO);
        when(alertNotificationsService.getUnconsumedAlertsByAppType(AppType.MOBILE_CLIENT))
                .thenReturn(Stream.of(mobileAlert));

        AlertNotificationsWebSocketService service = new AlertNotificationsWebSocketService(new SubscriberRepository(), alertNotificationsService);
        SubscriptionRequest request = SubscriptionRequest.fromJson("{\"type\":\"SubscriptionRequest\",\"requestId\":\"r1\",\"topic\":\"ALERT_NOTIFICATIONS\"}").orElseThrow();

        String json = service.getJsonPayload(request).orElseThrow();

        List<AuthorizedAlertDataDto> payload = JsonMapperProvider.get().readerForListOf(AuthorizedAlertDataDto.class).readValue(json);
        assertThat(payload).hasSize(1);
        assertThat(payload.getFirst().appType()).isEqualTo(AppType.MOBILE_CLIENT);
        verify(alertNotificationsService).getUnconsumedAlertsByAppType(AppType.MOBILE_CLIENT);
    }

    @Test
    void getJsonPayloadRejectsInvalidAppType() {
        AlertNotificationsService alertNotificationsService = mock(AlertNotificationsService.class);
        AlertNotificationsWebSocketService service = new AlertNotificationsWebSocketService(new SubscriberRepository(), alertNotificationsService);
        Subscriber subscriber = new Subscriber(Topic.ALERT_NOTIFICATIONS, Optional.of("invalid"), "sub-1", mock(WebSocket.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.getJsonPayload(subscriber));

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
        SubscriptionRequest request = SubscriptionRequest.fromJson("{\"type\":\"SubscriptionRequest\",\"requestId\":\"r1\",\"topic\":\"ALERT_NOTIFICATIONS\"}").orElseThrow();

        String json = service.getJsonPayload(request).orElseThrow();

        List<AuthorizedAlertDataDto> payload = JsonMapperProvider.get().readerForListOf(AuthorizedAlertDataDto.class).readValue(json);
        assertThat(payload)
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