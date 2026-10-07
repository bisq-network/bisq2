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

package bisq.api.web_socket.domain.release_notifications;

import bisq.api.dto.release.ReleaseNotificationDto;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.common.json.JsonMapperProvider;
import bisq.common.observable.collection.ObservableSet;
import com.fasterxml.jackson.databind.JsonNode;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleaseNotificationsWebSocketServiceTest {
    private static final String RELEASE_MANAGER_PROFILE_ID = "0123456789abcdef0123456789abcdef01234567";
    private static final long SEND_TIMEOUT_MS = 1000;

    @Test
    void getJsonPayloadReturnsOnlyNotificationsOfExactlyTheSubscribedAppType() throws Exception {
        ReleaseNotificationsWebSocketService service = serviceWith(
                notification("mobile-a", "2.1.13", false, AppType.MOBILE_CLIENT),
                notification("mobile-b", "2.1.12", false, AppType.MOBILE_CLIENT),
                notification("unspecified", "2.1.14", false, AppType.UNSPECIFIED),
                notification("desktop", "2.1.15", false, AppType.DESKTOP),
                notification("mobile-node", "2.1.16", false, AppType.MOBILE_NODE));

        List<ReleaseNotificationDto> payload = readPayload(service.getJsonPayload(Optional.of("mobile_client")));

        assertThat(payload)
                .as("UNSPECIFIED is not a wildcard for release notifications, unlike alerts")
                .extracting(ReleaseNotificationDto::id)
                .containsExactly("mobile-a", "mobile-b");
        assertThat(payload).allSatisfy(dto -> assertThat(dto.appType()).isEqualTo(AppType.MOBILE_CLIENT));
    }

    @Test
    void getJsonPayloadSortsByReleaseVersionNewestFirst() throws Exception {
        // 2.1.9 sorts above 2.1.10 as a string, so this fails if versionString were compared instead.
        ReleaseNotificationsWebSocketService service = serviceWith(
                notification("v2.1.9", "2.1.9", false, AppType.MOBILE_CLIENT),
                notification("v2.1.13", "2.1.13", false, AppType.MOBILE_CLIENT),
                notification("v2.1.10", "2.1.10", false, AppType.MOBILE_CLIENT));

        List<ReleaseNotificationDto> payload = readPayload(service.getJsonPayload(Optional.of("MOBILE_CLIENT")));

        assertThat(payload)
                .extracting(ReleaseNotificationDto::versionString)
                .containsExactly("2.1.13", "2.1.10", "2.1.9");
    }

    @Test
    void getJsonPayloadKeepsPreReleasesAndFlagsThemOnTheWire() throws Exception {
        ReleaseNotificationsWebSocketService service = serviceWith(
                notification("release", "2.1.13", false, AppType.MOBILE_CLIENT),
                notification("pre-release", "2.2.0", true, AppType.MOBILE_CLIENT));

        JsonNode payload = JsonMapperProvider.get().readTree(service.getJsonPayload(Optional.of("MOBILE_CLIENT")).orElseThrow());

        assertThat(payload).hasSize(2);
        assertThat(payload.get(0).get("id").asText()).isEqualTo("pre-release");
        assertThat(payload.get(0).get("isPreRelease").asBoolean()).isTrue();
        assertThat(payload.get(1).get("id").asText()).isEqualTo("release");
        assertThat(payload.get(1).get("isPreRelease").asBoolean()).isFalse();
    }

    @Test
    void getJsonPayloadRejectsInvalidAppType() {
        ReleaseNotificationsWebSocketService service = serviceWith();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getJsonPayload(Optional.of("invalid")));

        assertThat(exception).hasMessage("Invalid appType: invalid");
    }

    @Test
    void getJsonPayloadRejectsMissingAppType() {
        ReleaseNotificationsWebSocketService service = serviceWith();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getJsonPayload(Optional.empty()));

        assertThat(exception).hasMessage("appType parameter is required");
    }

    @Test
    void validateRejectsMissingOrInvalidAppType() {
        ReleaseNotificationsWebSocketService service = serviceWith();

        assertThrows(IllegalArgumentException.class, () -> service.validate(subscriptionRequest("r1", null)));
        assertThrows(IllegalArgumentException.class, () -> service.validate(subscriptionRequest("r2", "")));
        assertThrows(IllegalArgumentException.class, () -> service.validate(subscriptionRequest("r3", "invalid")));
    }

    @Test
    void canonicalizeParameterNormalizesToCanonicalAppTypeName() {
        ReleaseNotificationsWebSocketService service = serviceWith();

        assertThat(service.canonicalizeParameter(Optional.of("mobile_client"))).isEqualTo(Optional.of("MOBILE_CLIENT"));
        assertThat(service.canonicalizeParameter(Optional.of("Mobile_Client"))).isEqualTo(Optional.of("MOBILE_CLIENT"));
        assertThat(service.canonicalizeParameter(Optional.of("desktop"))).isEqualTo(Optional.of("DESKTOP"));
    }

    @Test
    void parameterlessPayloadReturnsAllNotificationsNewestFirst() throws Exception {
        ReleaseNotificationsWebSocketService service = serviceWith(
                notification("mobile", "2.1.13", false, AppType.MOBILE_CLIENT),
                notification("desktop", "2.1.14", false, AppType.DESKTOP));

        List<ReleaseNotificationDto> payload = readPayload(service.getJsonPayload());

        assertThat(payload).extracting(ReleaseNotificationDto::id).containsExactly("desktop", "mobile");
    }

    @Test
    void aNewNotificationIsPushedToEachSubscriberFilteredByItsAppType() {
        ObservableSet<ReleaseNotification> notifications = new ObservableSet<>();
        SubscriberRepository subscriberRepository = new SubscriberRepository();
        ReleaseNotificationsWebSocketService service =
                new ReleaseNotificationsWebSocketService(subscriberRepository, releaseNotificationsService(notifications));
        service.initialize();
        List<String> mobileSends = Collections.synchronizedList(new ArrayList<>());
        List<String> desktopSends = Collections.synchronizedList(new ArrayList<>());
        WebSocket mobileSocket = recordingWebSocket(mobileSends);
        WebSocket desktopSocket = recordingWebSocket(desktopSends);
        subscriberRepository.add(subscriptionRequest("r1", "MOBILE_CLIENT"), Optional.of("MOBILE_CLIENT"), mobileSocket);
        subscriberRepository.add(subscriptionRequest("r2", "DESKTOP"), Optional.of("DESKTOP"), desktopSocket);

        notifications.add(notification("mobile-release", "2.1.13", false, AppType.MOBILE_CLIENT));

        verify(mobileSocket, timeout(SEND_TIMEOUT_MS)).send(anyString());
        verify(desktopSocket, timeout(SEND_TIMEOUT_MS)).send(anyString());
        assertThat(mobileSends).singleElement().asString().contains("mobile-release");
        assertThat(desktopSends).singleElement().asString().doesNotContain("mobile-release");
    }

    private static List<ReleaseNotificationDto> readPayload(Optional<String> json) throws Exception {
        return JsonMapperProvider.get().readerForListOf(ReleaseNotificationDto.class).readValue(json.orElseThrow());
    }

    private static ReleaseNotificationsWebSocketService serviceWith(ReleaseNotification... notifications) {
        return new ReleaseNotificationsWebSocketService(new SubscriberRepository(),
                releaseNotificationsService(new ObservableSet<>(List.of(notifications))));
    }

    private static ReleaseNotificationsService releaseNotificationsService(ObservableSet<ReleaseNotification> notifications) {
        ReleaseNotificationsService releaseNotificationsService = mock(ReleaseNotificationsService.class);
        when(releaseNotificationsService.getReleaseNotifications()).thenReturn(notifications);
        return releaseNotificationsService;
    }

    private static WebSocket recordingWebSocket(List<String> sentMessages) {
        WebSocket webSocket = mock(WebSocket.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> {
            sentMessages.add(invocation.getArgument(0));
            return ReadyFutureImpl.create(null);
        }).when(webSocket).send(anyString());
        return webSocket;
    }

    private static SubscriptionRequest subscriptionRequest(String requestId, String parameter) {
        String parameterPart = parameter != null ? ",\"parameter\":\"" + parameter + "\"" : "";
        String json = "{\"type\":\"SubscriptionRequest\",\"requestId\":\"" + requestId
                + "\",\"topic\":\"RELEASE_NOTIFICATIONS\"" + parameterPart + "}";
        return SubscriptionRequest.fromJson(json).orElseThrow();
    }

    private static ReleaseNotification notification(String id, String version, boolean isPreRelease, AppType appType) {
        return new ReleaseNotification(id,
                System.currentTimeMillis(),
                isPreRelease,
                false,
                "Release notes " + id,
                version,
                RELEASE_MANAGER_PROFILE_ID,
                false,
                appType);
    }
}
