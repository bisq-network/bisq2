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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MobileNotificationService implements Service {
    @Getter
    private final DeviceRegistrationService deviceRegistrationService;
    private final MobileNotificationRelayClient mobileNotificationRelayClient;
    /**
     * Device ids already warned about the ECIES fallback. The condition is a property of how the device
     * registered, so it holds for every notification that device will ever receive — warning per
     * dispatch would repeat the same line for the life of the pairing and bury everything around it.
     * Cleared for a device the moment it is seen with a symmetric key, so that re-pairing — which is the
     * fix the warning tells the operator to apply — re-arms it rather than silencing it should the same
     * device ever register without a key again.
     */
    private final Set<String> eciesFallbackWarnedDeviceIds = ConcurrentHashMap.newKeySet();

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
        // Logged by category and by which fields are set, never by title: a chat notification's title
        // embeds the counterparty's user name and its message is the chat body. Both are already on
        // this machine in cleartext — ChatNotification serialises them into the notification store —
        // so the point is not secrecy from the host, it is that logs get pasted into bug reports and
        // issue trackers, which data dirs do not. The presence flags keep the line worth having: they
        // are what tells you whether a payload went out incomplete.
        if (profiles.isEmpty()) {
            log.debug("No mobile devices registered — skipping {} push notification", notification.getCategory());
            return;
        }
        log.info("Dispatching {} push notification to {} registered device(s) " +
                        "(hasTradeId={}, hasChannelId={}, hasPeerUserName={})",
                notification.getCategory(),
                profiles.size(),
                notification.getTradeId().isPresent(),
                notification.getChannelId().isPresent(),
                notification.getPeerUserName().isPresent());

        // Built once rather than per device: neither the payload nor its json depends on the device,
        // and a serialisation failure is one failure that leaves no device serviceable — not one
        // failure per device, logged N times.
        String json;
        try {
            json = JsonMapperProvider.get().writeValueAsString(MobileNotificationPayload.from(notification));
        } catch (Exception e) {
            log.error("Could not build the {} push notification payload; no device will receive it",
                    notification.getCategory(), e);
            return;
        }

        profiles.forEach(mobileDeviceProfile -> {
                    boolean isAndroid = mobileDeviceProfile.getPlatform() == MobileDevicePlatform.ANDROID;
                    String deviceToken = mobileDeviceProfile.getDeviceToken();
                    String platform = isAndroid ? "Android" : "iOS";
                    try {
                        // AES-GCM whenever the device registered a symmetric key, which both current
                        // clients do.
                        String encryptedBase64;
                        boolean mutableContent = false;
                        if (mobileDeviceProfile.hasSymmetricKey()) {
                            eciesFallbackWarnedDeviceIds.remove(mobileDeviceProfile.getDeviceId());
                            encryptedBase64 = MobileNotificationEncryption.encryptWithSymmetricKey(
                                    mobileDeviceProfile.getSymmetricKeyBase64().orElseThrow(), json);
                            mutableContent = true;
                        } else {
                            // Warned rather than taken quietly, because it is a dead end on both
                            // platforms: the Android FCM service only implements GCM and cannot decrypt
                            // an ECIES payload, and on iOS this branch leaves mutableContent false,
                            // which is the flag that lets the NSE run and rewrite the banner at all.
                            // Either way the user sees the relay's placeholder text, and the node reads
                            // as success — the relay answers 2xx for a payload nobody can open.
                            if (eciesFallbackWarnedDeviceIds.add(mobileDeviceProfile.getDeviceId())) {
                                log.warn("{} device registered without a symmetric key; falling back to ECIES, " +
                                        "which no current client can render — the banner will show the relay's " +
                                        "placeholder text. Re-pairing the device registers a symmetric key. " +
                                        "Reported once per device.", platform);
                            }
                            encryptedBase64 = MobileNotificationEncryption.encrypt(
                                    mobileDeviceProfile.getPublicKeyBase64(), json);
                        }

                        // Send Base64-encoded encrypted payload directly to the relay's v1 POST endpoint.
                        // The v1 endpoint accepts JSON with the encrypted field as Base64, avoiding the
                        // hex-encoding round-trip of the legacy GET /relay endpoint which corrupts binary data.
                        mobileNotificationRelayClient.sendToRelayServer(isAndroid,
                                        deviceToken,
                                        encryptedBase64,
                                        mutableContent)
                                .whenComplete((success, throwable) -> {
                                    if (throwable != null) {
                                        log.warn("Failed to send push notification to {} device", platform, throwable);
                                    } else if (Boolean.TRUE.equals(success)) {
                                        log.info("Push notification sent to {} device (token: {}...)", platform, deviceToken.substring(0, Math.min(8, deviceToken.length())));
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
