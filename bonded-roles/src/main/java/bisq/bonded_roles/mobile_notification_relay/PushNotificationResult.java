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

package bisq.bonded_roles.mobile_notification_relay;

import lombok.Getter;

/**
 * Result of sending a push notification.
 * Indicates success/failure and whether the device should be unregistered.
 */
@Getter
public class PushNotificationResult {
    private final boolean success;
    private final boolean shouldUnregister;
    private final String deviceToken;

    private PushNotificationResult(boolean success, boolean shouldUnregister, String deviceToken) {
        this.success = success;
        this.shouldUnregister = shouldUnregister;
        this.deviceToken = deviceToken;
    }

    /**
     * Create a successful result.
     */
    public static PushNotificationResult success(String deviceToken) {
        return new PushNotificationResult(true, false, deviceToken);
    }

    /**
     * Create a failed result that should trigger device unregistration.
     * Used when the relay reports BadDeviceToken or isUnregistered=true.
     */
    public static PushNotificationResult failedShouldUnregister(String deviceToken) {
        return new PushNotificationResult(false, true, deviceToken);
    }

    /**
     * Create a failed result (temporary failure, device should not be unregistered).
     */
    public static PushNotificationResult failed(String deviceToken) {
        return new PushNotificationResult(false, false, deviceToken);
    }
}

