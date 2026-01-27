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
import bisq.bonded_roles.mobile_notification_relay.PushNotificationResult;
import bisq.common.application.Service;
import bisq.notifications.mobile.registration.DeviceRegistrationService;
import bisq.notifications.Notification;
import bisq.persistence.PersistenceService;
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
        //todo
        String encryptedPayload = "todo";
        String deviceId = "todo";
        boolean isUrgent = true;
        CompletableFuture<PushNotificationResult> result = mobileNotificationRelayClient.sendToRelayServer(encryptedPayload, deviceId, isUrgent);
    }

}
