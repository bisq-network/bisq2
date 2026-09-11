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

import bisq.common.json.JsonMapperProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * The relay's answer to a push dispatch, mirroring bisq-relay's {@code PushNotificationResult}.
 * The relay serializes it as the response body of its v1 endpoints — HTTP 200 when the gateway
 * accepted the push, HTTP 400 with the same shape when it rejected it. {@code isUnregistered}
 * is the gateway's verdict that the device token is permanently dead (FCM {@code UNREGISTERED},
 * APNs {@code Unregistered}/{@code BadDeviceToken}) — the signal that a registration can be
 * pruned, as opposed to a transient delivery failure.
 */
@Slf4j
public record MobileNotificationRelayResult(boolean wasAccepted,
                                            Optional<String> errorCode,
                                            Optional<String> errorMessage,
                                            boolean isUnregistered) {

    public static MobileNotificationRelayResult accepted() {
        return new MobileNotificationRelayResult(true, Optional.empty(), Optional.empty(), false);
    }

    /**
     * Parses a relay response body into a result, or {@link Optional#empty()} when the body is
     * not the relay's structured shape. The boolean {@code wasAccepted} field is the required
     * marker: older relays answer with plain acks and frameworks answer errors with their own
     * JSON, and neither may fabricate a verdict — a fabricated rejection could prune a live
     * registration. Unknown fields are ignored for forward compatibility.
     */
    public static Optional<MobileNotificationRelayResult> fromJson(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode node = JsonMapperProvider.get().readTree(body);
            if (node == null || !node.isObject() || !node.path("wasAccepted").isBoolean()) {
                return Optional.empty();
            }
            // Require a real boolean — asBoolean would coerce "true" or non-zero numbers
            // into a prune verdict.
            JsonNode isUnregistered = node.path("isUnregistered");
            return Optional.of(new MobileNotificationRelayResult(
                    node.get("wasAccepted").asBoolean(),
                    textField(node, "errorCode"),
                    textField(node, "errorMessage"),
                    isUnregistered.isBoolean() && isUnregistered.asBoolean()));
        } catch (Exception e) {
            log.debug("Relay response body is not structured JSON; treating as unstructured ({})",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static Optional<String> textField(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isTextual() ? Optional.of(field.asText()) : Optional.empty();
    }
}
