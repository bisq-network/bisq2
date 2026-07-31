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

package bisq.api.web_socket.rest_api_proxy;

import bisq.api.web_socket.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Clients released before the node derived the request identity from the WebSocket handshake still put
 * their session id into every message. The node ignores it, and must additionally make sure it neither
 * travels any further into the request it forwards nor ends up readable in a log file.
 */
class ClientSuppliedCredentialsTest {
    private static final String SESSION_ID = "session-id-value";

    private static String requestJson(String headers) {
        return "{\"type\":\"WebSocketRestApiRequest\","
                + "\"requestId\":\"request-id\",\"method\":\"GET\",\"path\":\"/api/v1/settings/version\","
                + "\"body\":\"\",\"headers\":" + headers + "}";
    }

    @Test
    void credentialsAreStrippedFromTheRequest() {
        WebSocketRestApiRequest request = parse(requestJson(
                "{\"Bisq-Session-Id\":\"" + SESSION_ID + "\",\"Bisq-Client-Id\":\"client-id\"}"));

        request.clearHeaders();

        // Emptied rather than nulled, so the generated getter stays safe for anything added later
        assertThat(request.getHeaders()).isEmpty();
    }

    @Test
    void strippingCopesWithAMessageThatCarriesNoHeaders() {
        WebSocketRestApiRequest withoutHeaders = parse(requestJson("null"));

        withoutHeaders.clearHeaders();

        assertThat(withoutHeaders.getHeaders()).isEmpty();
    }

    @Test
    void strippedRequestCannotLeakTheSessionIdThroughToString() {
        WebSocketRestApiRequest request = parse(requestJson("{\"Bisq-Session-Id\":\"" + SESSION_ID + "\"}"));
        assertThat(request.toString()).contains(SESSION_ID);

        request.clearHeaders();

        assertThat(request.toString()).doesNotContain(SESSION_ID);
    }

    @Test
    void rawMessagesAreRedactedBeforeLogging() {
        String json = requestJson("{\"Bisq-Session-Id\":\"" + SESSION_ID + "\",\"Bisq-Client-Id\":\"client-id\"}");

        String redacted = JsonUtil.redactCredentials(json);

        assertThat(redacted).doesNotContain(SESSION_ID);
        assertThat(redacted).contains("\"Bisq-Session-Id\":\"***\"");
        // The client id is an identifier the node logs elsewhere; keeping it is what makes the line useful
        assertThat(redacted).contains("client-id");
        assertThat(redacted).contains("/api/v1/settings/version");
    }

    @Test
    void redactionCopesWithFormattingAndCasing() {
        assertThat(JsonUtil.redactCredentials("{\"bisq-session-id\" : \"" + SESSION_ID + "\"}"))
                .doesNotContain(SESSION_ID);
        assertThat(JsonUtil.redactCredentials("{\"Bisq-Session-Id\":\"\"}")).isEqualTo("{\"Bisq-Session-Id\":\"***\"}");
        assertThat(JsonUtil.redactCredentials("no json at all")).isEqualTo("no json at all");
    }

    /**
     * The shared mapper has FAIL_ON_UNKNOWN_PROPERTIES enabled, so the field cannot simply be deleted
     * once clients stop sending credentials: a message of an already released client would then fail to
     * parse and be dropped without any response, leaving the client waiting for a reply that never comes.
     * Removing it has to wait until no released client sends the property at all.
     */
    @Test
    void theHeadersFieldCannotBeRemovedWhileClientsStillSendIt() {
        assertThat(WebSocketRestApiRequest.fromJson(requestJson("{\"Bisq-Session-Id\":\"" + SESSION_ID + "\"}")))
                .isPresent();
        assertThat(WebSocketRestApiRequest.fromJson("{\"type\":\"WebSocketRestApiRequest\","
                + "\"requestId\":\"request-id\",\"method\":\"GET\",\"path\":\"/p\",\"body\":\"\","
                + "\"aPropertyThisNodeDoesNotKnow\":1}"))
                .isEmpty();
    }

    private static WebSocketRestApiRequest parse(String json) {
        Optional<WebSocketRestApiRequest> request = WebSocketRestApiRequest.fromJson(json);
        assertThat(request).isPresent();
        return request.get();
    }
}
