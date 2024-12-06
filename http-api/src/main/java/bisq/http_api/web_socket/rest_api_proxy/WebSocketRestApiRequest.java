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

package bisq.http_api.web_socket.rest_api_proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class WebSocketRestApiRequest {
    // Client side full qualified class name for response class required for polymorphism support
    private String responseClassName;
    private String requestId;
    private String path;
    private String method;
    private String body;
    // Client side full qualified class name set by serialize to support polymorphism
    private String className;

    public static boolean isExpectedJson(String message) {
        return message.contains("requestId") &&
                message.contains("path") &&
                message.contains("method") &&
                message.contains("body");
    }

    public static Optional<WebSocketRestApiRequest> fromJson(ObjectMapper objectMapper, String json) {
        try {
            return Optional.of(objectMapper.readValue(json, WebSocketRestApiRequest.class));
        } catch (JsonProcessingException e) {
            log.error("Json deserialization failed. Message={}", json, e);
        }
        return Optional.empty();
    }
}