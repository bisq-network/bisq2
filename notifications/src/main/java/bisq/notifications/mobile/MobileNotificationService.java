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
import bisq.common.application.Service;
import bisq.common.encoding.Base64;
import bisq.common.encoding.Hex;
import bisq.common.json.JsonMapperProvider;
import bisq.notifications.Notification;
import bisq.notifications.mobile.registration.DeviceRegistrationService;
import bisq.notifications.mobile.registration.MobileDevicePlatform;
import bisq.persistence.PersistenceService;
import bisq.security.mobile_notifications.MobileNotificationEncryption;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MobileNotificationService implements Service {
    @Getter
    private final DeviceRegistrationService deviceRegistrationService;
    private final MobileNotificationRelayClient mobileNotificationRelayClient;

    public MobileNotificationService(PersistenceService persistenceService,
                                     MobileNotificationRelayClient mobileNotificationRelayClient) {
        deviceRegistrationService = new DeviceRegistrationService(persistenceService);
        this.mobileNotificationRelayClient = mobileNotificationRelayClient;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return deviceRegistrationService.initialize()
                .thenCompose(e -> mobileNotificationRelayClient.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return deviceRegistrationService.shutdown()
                .thenCompose(e -> mobileNotificationRelayClient.shutdown());
    }

    public void dispatchNotification(Notification notification) {
        var profiles = deviceRegistrationService.getMobileDeviceProfiles();
        if (profiles.isEmpty()) {
            log.debug("No mobile devices registered — skipping push notification for '{}'", notification.getTitle());
            return;
        }
        log.info("Dispatching push notification '{}' to {} registered device(s)", notification.getTitle(), profiles.size());
        profiles.forEach(mobileDeviceProfile -> {
                    boolean isAndroid = mobileDeviceProfile.getPlatform() == MobileDevicePlatform.ANDROID;
                    String deviceTokenHex = mobileDeviceProfile.getDeviceToken();
                    String platform = isAndroid ? "Android" : "iOS";
                    MobileNotificationPayload payload = new MobileNotificationPayload(notification.getId(),
                            notification.getTitle(),
                            notification.getMessage());
                    try {
                        String json = JsonMapperProvider.get().writeValueAsString(payload);
                        String encryptedBase64 = MobileNotificationEncryption.encrypt(mobileDeviceProfile.getPublicKeyBase64(), json);
                        // The bisq-relay server expects hex-encoded encrypted messages.
                        // MobileNotificationEncryption.encrypt() returns Base64, so we
                        // convert here to match the relay's expected format.
                        String encryptedMessageHex = Hex.encode(Base64.decode(encryptedBase64));
                        mobileNotificationRelayClient.sendToRelayServer(isAndroid,
                                        deviceTokenHex,
                                        encryptedMessageHex)
                                .whenComplete((success, throwable) -> {
                                    if (throwable != null) {
                                        log.warn("Failed to send push notification to {} device", platform, throwable);
                                    } else if (Boolean.TRUE.equals(success)) {
                                        log.info("Push notification sent to {} device (token: {}...)", platform, deviceTokenHex.substring(0, Math.min(8, deviceTokenHex.length())));
                                    } else {
                                        log.warn("Push notification relay returned failure for {} device", platform);
                                    }
                                });
                    } catch (Exception e) {
                        log.error("Could not send notification to relay server for {} device", platform, e);
                    }
                });
    }
}
