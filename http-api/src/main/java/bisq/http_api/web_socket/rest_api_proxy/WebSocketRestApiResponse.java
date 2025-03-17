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

import bisq.http_api.web_socket.WebSocketMessage;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class WebSocketRestApiResponse implements WebSocketMessage {
    private final String requestId;
    private final int statusCode;
    private final String body;

    @JsonCreator
    public WebSocketRestApiResponse(@JsonProperty("requestId") String requestId,
                                    @JsonProperty("statusCode") int statusCode,
                                    @JsonProperty("body") String body) {
        this.requestId = requestId;
        this.statusCode = statusCode;
        this.body = body;
    }

    @JsonIgnore
    public Optional<String> toJson(ObjectMapper objectMapper) {
        try {
            return Optional.of(objectMapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            log.error("Json serialisation failed", e);
        }
        return Optional.empty();
    }
}