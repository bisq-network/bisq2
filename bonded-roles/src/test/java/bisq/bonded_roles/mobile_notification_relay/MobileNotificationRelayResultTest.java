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

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MobileNotificationRelayResultTest {

    @Test
    void fromJson_mapsAcceptedBody() {
        Optional<MobileNotificationRelayResult> result =
                MobileNotificationRelayResult.fromJson("{\"wasAccepted\":true,\"isUnregistered\":false}");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().wasAccepted()).isTrue();
        assertThat(result.orElseThrow().isUnregistered()).isFalse();
        assertThat(result.orElseThrow().errorCode()).isEmpty();
        assertThat(result.orElseThrow().errorMessage()).isEmpty();
    }

    @Test
    void fromJson_mapsUnregisteredRejection() {
        Optional<MobileNotificationRelayResult> result = MobileNotificationRelayResult.fromJson(
                "{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\"," +
                        "\"errorMessage\":\"Requested entity was not found.\",\"isUnregistered\":true}");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().wasAccepted()).isFalse();
        assertThat(result.orElseThrow().isUnregistered()).isTrue();
        assertThat(result.orElseThrow().errorCode()).contains("UNREGISTERED");
        assertThat(result.orElseThrow().errorMessage()).contains("Requested entity was not found.");
    }

    @Test
    void fromJson_mapsRejectionWithoutUnregisteredFlag() {
        Optional<MobileNotificationRelayResult> result = MobileNotificationRelayResult.fromJson(
                "{\"wasAccepted\":false,\"errorCode\":\"QUOTA_EXCEEDED\",\"isUnregistered\":false}");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().wasAccepted()).isFalse();
        assertThat(result.orElseThrow().isUnregistered()).isFalse();
    }

    @Test
    void fromJson_toleratesUnknownFields() {
        Optional<MobileNotificationRelayResult> result = MobileNotificationRelayResult.fromJson(
                "{\"wasAccepted\":true,\"isUnregistered\":false,\"futureField\":42}");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().wasAccepted()).isTrue();
    }

    @Test
    void fromJson_rejectsBodiesWithoutTheWasAcceptedMarker() {
        // A structured verdict requires the relay's marker field. Anything else — an older
        // relay's plain ack, a framework's default error page — must never fabricate one,
        // since a fabricated rejection could prune a live registration.
        assertThat(MobileNotificationRelayResult.fromJson("")).isEmpty();
        assertThat(MobileNotificationRelayResult.fromJson("OK")).isEmpty();
        assertThat(MobileNotificationRelayResult.fromJson("{}")).isEmpty();
        assertThat(MobileNotificationRelayResult.fromJson("{\"success\":true}")).isEmpty();
        assertThat(MobileNotificationRelayResult.fromJson(
                "{\"timestamp\":\"2026-09-11T00:00:00Z\",\"status\":400,\"error\":\"Bad Request\"," +
                        "\"path\":\"/v1/fcm/device/x\"}")).isEmpty();
        assertThat(MobileNotificationRelayResult.fromJson("{\"wasAccepted\":\"yes\"}")).isEmpty();
        assertThat(MobileNotificationRelayResult.fromJson("not json at all {{{")).isEmpty();
        assertThat(MobileNotificationRelayResult.fromJson(null)).isEmpty();
    }

    @Test
    void fromJson_ignoresNonBooleanUnregisteredFlag() {
        // Jackson's asBoolean coerces "true" and non-zero numbers; a prune verdict must
        // come from a real boolean, never from type coercion.
        Optional<MobileNotificationRelayResult> stringFlag = MobileNotificationRelayResult.fromJson(
                "{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\",\"isUnregistered\":\"true\"}");
        assertThat(stringFlag).isPresent();
        assertThat(stringFlag.orElseThrow().isUnregistered()).isFalse();

        Optional<MobileNotificationRelayResult> numericFlag = MobileNotificationRelayResult.fromJson(
                "{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\",\"isUnregistered\":1}");
        assertThat(numericFlag).isPresent();
        assertThat(numericFlag.orElseThrow().isUnregistered()).isFalse();
    }

    @Test
    void accepted_isAnAcceptedResultWithoutErrorDetails() {
        MobileNotificationRelayResult result = MobileNotificationRelayResult.accepted();

        assertThat(result.wasAccepted()).isTrue();
        assertThat(result.isUnregistered()).isFalse();
        assertThat(result.errorCode()).isEmpty();
        assertThat(result.errorMessage()).isEmpty();
    }
}
