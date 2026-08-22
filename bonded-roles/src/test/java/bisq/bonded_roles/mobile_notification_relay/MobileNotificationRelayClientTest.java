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

import bisq.common.network.TransportType;
import bisq.network.NetworkService;
import bisq.network.http.HttpRequest;
import bisq.network.http.HttpRequestServiceConfig;
import bisq.network.http.HttpRequestUrlProvider;
import bisq.network.http.utils.HttpMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MobileNotificationRelayClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<TestableRelayClient> openedClients = new ArrayList<>();

    @AfterEach
    void tearDown() {
        // Each newClient() spins up an executor inside the parent service.
        // Without explicit shutdown the threads linger, which adds up across
        // CI runs.
        openedClients.forEach(c -> c.shutdown().join());
        openedClients.clear();
    }

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

    @Test
    void buildRequest_androidTargetsFcmPath() throws Exception {
        TestableRelayClient client = newClient();

        HttpRequest req = client.publicBuildRequest(stubProvider(),
                new MobileNotificationRelayClient.RequestData(true, "tok-123", "ZW5jcnlwdGVk", false));

        assertThat(req.method()).isEqualTo(HttpMethod.POST);
        assertThat(req.path()).isEqualTo("/v1/fcm/device/tok-123");
        assertThat(req.body()).isPresent();
        JsonNode body = objectMapper.readTree(req.body().orElseThrow());
        assertThat(body.get("encrypted").asText()).isEqualTo("ZW5jcnlwdGVk");
        assertThat(body.get("isMutableContent").asBoolean()).isFalse();
        assertThat(req.header()).isPresent();
        assertThat(req.header().orElseThrow().getFirst()).isEqualTo("Content-Type");
        assertThat(req.header().orElseThrow().getSecond()).isEqualTo("application/json");
    }

    @Test
    void buildRequest_iosTargetsApnsPath() throws Exception {
        TestableRelayClient client = newClient();

        HttpRequest req = client.publicBuildRequest(stubProvider(),
                new MobileNotificationRelayClient.RequestData(false, "ios-tok", "Ym9keQ==", true));

        assertThat(req.path()).isEqualTo("/v1/apns/device/ios-tok");
        JsonNode body = objectMapper.readTree(req.body().orElseThrow());
        assertThat(body.get("isMutableContent").asBoolean()).isTrue();
    }

    @Test
    void buildRequest_redactsDeviceTokenFromLogPath() {
        TestableRelayClient client = newClient();

        HttpRequest req = client.publicBuildRequest(stubProvider(),
                new MobileNotificationRelayClient.RequestData(true, "secret-token-abc", "x", false));

        assertThat(req.path())
                .as("real path carries the token over the wire")
                .endsWith("secret-token-abc");
        assertThat(req.logPath())
                .as("logPath must not contain the device token")
                .doesNotContain("secret-token-abc")
                .isEqualTo("/v1/fcm/device/<redacted>");
    }

    @Test
    void buildRequest_optsIntoServerErrorRetry() {
        TestableRelayClient client = newClient();

        HttpRequest req = client.publicBuildRequest(stubProvider(),
                new MobileNotificationRelayClient.RequestData(true, "tok", "x", false));

        // Push delivery is best-effort and 5xx from FCM/APNS is typically transient,
        // so we opt into at-least-once semantics: a rare duplicate banner beats a silent drop.
        assertThat(req.retryOnServerError()).isTrue();
    }

    @Test
    void parseResult_reportsSuccess_whenAnyBodyReceived() {
        TestableRelayClient client = newClient();

        assertThat(client.publicParseResult("any-body")).isTrue();
        assertThat(client.publicParseResult("")).isTrue();
    }

    private TestableRelayClient newClient() {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.CLEAR));
        HttpRequestServiceConfig conf = new HttpRequestServiceConfig(60L,
                Set.of(stubProvider()),
                Set.of());
        TestableRelayClient client = new TestableRelayClient(conf, networkService);
        openedClients.add(client);
        return client;
    }

    private static HttpRequestUrlProvider stubProvider() {
        return new HttpRequestUrlProvider("https://relay.example/", "operator", "/legacy", TransportType.CLEAR);
    }

    /** Exposes the protected hooks so unit tests don't need network IO. */
    private static final class TestableRelayClient extends MobileNotificationRelayClient {
        TestableRelayClient(HttpRequestServiceConfig conf, NetworkService networkService) {
            super(conf, networkService);
        }

        HttpRequest publicBuildRequest(HttpRequestUrlProvider provider,
                                       MobileNotificationRelayClient.RequestData data) {
            return buildRequest(provider, data);
        }

        Boolean publicParseResult(String json) {
            return parseResult(json);
        }
    }
}
