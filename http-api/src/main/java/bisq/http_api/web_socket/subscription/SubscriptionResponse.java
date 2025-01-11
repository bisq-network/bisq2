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

package bisq.http_api.web_socket.subscription;

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

import javax.annotation.Nullable;
import java.util.Optional;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class SubscriptionResponse implements WebSocketMessage {
    private final String requestId;
    @Nullable
    private final String payload;
    @Nullable
    private final String errorMessage;

    @JsonCreator
    public SubscriptionResponse(@JsonProperty("requestId") String requestId,
                                @JsonProperty("payload") @Nullable String payload,
                                @JsonProperty("errorMessage") @Nullable String errorMessage) {
        this.requestId = requestId;
        this.payload = payload;
        this.errorMessage = errorMessage;
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