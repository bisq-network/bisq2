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

import bisq.api.web_socket.WebSocketMessage;
import bisq.api.web_socket.util.JsonUtil;
import bisq.common.json.JsonMapperProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class WebSocketRestApiRequest implements WebSocketMessage {
    // Client side full qualified class name for response class required for polymorphism support
    private String responseClassName;
    private String requestId;
    private String path;
    private String method;
    private String body;
    // Accepted but ignored. Clients used to supply the session and client id here and we forwarded
    // them to the REST API server, which let a client act under an identity it never authenticated
    // with. The identity is now taken from the authenticated upgrade request instead. Both fields
    // are kept so that already released clients, which still send them, keep working until they
    // have rolled out a version that no longer does.
    private Map<String, String> headers;
    private String clientId;

    public static Optional<WebSocketRestApiRequest> fromJson(String json) {
        try {
            return Optional.of(JsonMapperProvider.get().readValue(json, WebSocketRestApiRequest.class));
        } catch (JsonProcessingException e) {
            log.error("Json deserialization failed. Message={}", JsonUtil.redactCredentials(json), e);
        }
        return Optional.empty();
    }

    /**
     * Removes the client supplied headers, so that from here on the request cannot carry them any
     * further — not into a forwarded request, and not into a log line via {@link #toString()}. The node
     * takes the identity of the call from the authenticated upgrade request instead, so what the
     * message carries is discarded. Called once, right after deserialization.
     */
    void clearHeaders() {
        // Jackson deserializes into the field, so emptying it is what actually removes them. Emptied
        // rather than nulled: the generated getter stays safe to call for anything added later.
        headers = Map.of();
    }
}