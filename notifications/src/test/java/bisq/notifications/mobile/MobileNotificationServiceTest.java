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

package bisq.notifications.mobile;

import bisq.bonded_roles.mobile_notification_relay.MobileNotificationRelayClient;
import bisq.bonded_roles.mobile_notification_relay.MobileNotificationRelayResult;
import bisq.notifications.Notification;
import bisq.notifications.mobile.registration.DeviceRegistrationService;
import bisq.notifications.mobile.registration.MobileDevicePlatform;
import bisq.notifications.mobile.registration.MobileDeviceProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileNotificationServiceTest {

    private static final String DEVICE_ID = "device-1";

    @Test
    void dispatch_prunesRegistration_whenGatewayReportsTokenUnregistered() {
        Fixture fixture = new Fixture(CompletableFuture.completedFuture(
                new MobileNotificationRelayResult(false,
                        Optional.of("UNREGISTERED"),
                        Optional.of("Requested entity was not found."),
                        true)));

        fixture.service.dispatchNotification(testNotification());

        verify(fixture.deviceRegistrationService).pruneDeadRegistration(DEVICE_ID, "device-token-1");
    }

    @Test
    void dispatch_keepsRegistration_whenAccepted() {
        Fixture fixture = new Fixture(
                CompletableFuture.completedFuture(MobileNotificationRelayResult.accepted()));

        fixture.service.dispatchNotification(testNotification());

        verify(fixture.deviceRegistrationService, never()).pruneDeadRegistration(any(), any());
    }

    @Test
    void dispatch_keepsRegistration_whenRejectedForOtherReasons() {
        Fixture fixture = new Fixture(CompletableFuture.completedFuture(
                new MobileNotificationRelayResult(false,
                        Optional.of("QUOTA_EXCEEDED"),
                        Optional.empty(),
                        false)));

        fixture.service.dispatchNotification(testNotification());

        verify(fixture.deviceRegistrationService, never()).pruneDeadRegistration(any(), any());
    }

    @Test
    void dispatch_keepsRegistration_onTransientFailure() {
        Fixture fixture = new Fixture(
                CompletableFuture.failedFuture(new IOException("relay unreachable")));

        fixture.service.dispatchNotification(testNotification());

        verify(fixture.deviceRegistrationService, never()).pruneDeadRegistration(any(), any());
    }

    @Test
    void loggableErrorCode_neutralizesLogInjectionAndCapsLength() {
        assertThat(MobileNotificationService.loggableErrorCode(Optional.of("UNREGISTERED"), "fallback"))
                .isEqualTo("UNREGISTERED");
        assertThat(MobileNotificationService.loggableErrorCode(Optional.of("BadDeviceToken"), "fallback"))
                .isEqualTo("BadDeviceToken");
        assertThat(MobileNotificationService.loggableErrorCode(
                Optional.of("x\r\n2026-09-11 FORGED LOG LINE \u001B[31mred"), "fallback"))
                .doesNotContain("\r", "\n", "\u001B", " ");
        assertThat(MobileNotificationService.loggableErrorCode(Optional.of("A".repeat(500)), "fallback"))
                .hasSize(40);
        assertThat(MobileNotificationService.loggableErrorCode(Optional.empty(), "fallback"))
                .isEqualTo("fallback");
        assertThat(MobileNotificationService.loggableErrorCode(Optional.of("  "), "fallback"))
                .isEqualTo("fallback");
    }

    private static Notification testNotification() {
        return new Notification() {
            @Override
            public String getId() {
                return "notification-1";
            }

            @Override
            public String getTitle() {
                return "title";
            }

            @Override
            public String getMessage() {
                return "message";
            }
        };
    }

    private static final class Fixture {
        private final DeviceRegistrationService deviceRegistrationService = mock(DeviceRegistrationService.class);
        private final MobileNotificationRelayClient relayClient = mock(MobileNotificationRelayClient.class);
        private final MobileNotificationService service;

        /** One registered Android device with a valid symmetric key, so dispatch runs real encryption. */
        Fixture(CompletableFuture<MobileNotificationRelayResult> relayResponse) {
            String symmetricKeyBase64 = Base64.getEncoder().encodeToString(new byte[32]);
            MobileDeviceProfile profile = new MobileDeviceProfile(DEVICE_ID,
                    "device-token-1",
                    "unused-public-key",
                    "test device",
                    MobileDevicePlatform.ANDROID,
                    Optional.of(symmetricKeyBase64),
                    Optional.of("client-1"));
            when(deviceRegistrationService.getMobileDeviceProfiles()).thenReturn(Set.of(profile));
            when(relayClient.sendToRelayServer(anyBoolean(), any(), any(), anyBoolean()))
                    .thenReturn(relayResponse);
            service = new MobileNotificationService(deviceRegistrationService, relayClient);
        }
    }
}
