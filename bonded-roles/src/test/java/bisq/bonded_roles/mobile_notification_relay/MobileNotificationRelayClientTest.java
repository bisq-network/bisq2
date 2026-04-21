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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MobileNotificationRelayClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildJsonBody_withAllFields() throws Exception {
        String json = MobileNotificationRelayClient.buildJsonBody("dGVzdA==", true, true);

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("encrypted").asText()).isEqualTo("dGVzdA==");
        assertThat(node.get("isUrgent").asBoolean()).isTrue();
        assertThat(node.get("isMutableContent").asBoolean()).isTrue();
    }

    @Test
    void buildJsonBody_withMutableContentFalse() throws Exception {
        String json = MobileNotificationRelayClient.buildJsonBody("abc123==", true, false);

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("encrypted").asText()).isEqualTo("abc123==");
        assertThat(node.get("isUrgent").asBoolean()).isTrue();
        assertThat(node.get("isMutableContent").asBoolean()).isFalse();
    }

    @Test
    void buildJsonBody_producesValidJson_withUrgentFalse() throws Exception {
        String json = MobileNotificationRelayClient.buildJsonBody("payload==", false, true);

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.size()).isEqualTo(3);
        assertThat(node.get("isUrgent").asBoolean()).isFalse();
        assertThat(node.get("isMutableContent").asBoolean()).isTrue();
    }

    @Test
    void buildJsonBody_preservesBase64Padding() throws Exception {
        // Base64 with padding characters must survive JSON serialization
        String base64 = "SGVsbG8gV29ybGQ=";
        String json = MobileNotificationRelayClient.buildJsonBody(base64, true, true);

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("encrypted").asText()).isEqualTo(base64);
    }
}
