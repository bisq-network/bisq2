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
        deviceRegistrationService.getMobileDeviceProfiles()
                .forEach(mobileDeviceProfile -> {
                    boolean isAndroid = mobileDeviceProfile.getPlatform() == MobileDevicePlatform.ANDROID;
                    String deviceTokenHex = mobileDeviceProfile.getDeviceToken();
                    MobileNotificationPayload payload = new MobileNotificationPayload(notification.getId(),
                            notification.getTitle(),
                            notification.getMessage());
                    try {
                        String json = JsonMapperProvider.get().writeValueAsString(payload);
                        String encryptedMessageHex = MobileNotificationEncryption.encrypt(mobileDeviceProfile.getPublicKeyBase64(), json);
                        mobileNotificationRelayClient.sendToRelayServer(isAndroid,
                                deviceTokenHex,
                                encryptedMessageHex);
                    } catch (Exception e) {
                        log.error("Could not send notification to relay server", e);
                    }
                });
    }
}
