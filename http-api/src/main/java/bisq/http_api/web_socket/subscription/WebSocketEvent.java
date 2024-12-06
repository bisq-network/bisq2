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

import com.fasterxml.jackson.annotation.JsonCreator;
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
public class WebSocketEvent {
    // Client side full qualified class name required for polymorphism support
    private final String className;
    private final Topic topic;
    private final String subscriberId;
    private final String payload;
    private final ModificationType modificationType;
    private final int sequenceNumber;

    @JsonCreator
    public WebSocketEvent(@JsonProperty("className") String className,
                          @JsonProperty("topic") Topic topic,
                          @JsonProperty("subscriberId") String subscriberId,
                          @JsonProperty("payload") String payload,
                          @JsonProperty("modificationType") ModificationType modificationType,
                          @JsonProperty("sequenceNumber") int sequenceNumber) {
        this.className = className;
        this.topic = topic;
        this.subscriberId = subscriberId;
        this.payload = payload;
        this.modificationType = modificationType;
        this.sequenceNumber = sequenceNumber;
    }

    public static Optional<String> toJson(ObjectMapper objectMapper,
                                          String className,
                                          Topic topic,
                                          String subscriberId,
                                          String payload,
                                          ModificationType modificationType,
                                          int sequenceNumber) {
        try {
            var webSocketEvent = new WebSocketEvent(className, topic, subscriberId, payload, modificationType, sequenceNumber);
            return Optional.of(objectMapper.writeValueAsString(webSocketEvent));
        } catch (JsonProcessingException e) {
            log.error("Json serialisation failed", e);
        }
        return Optional.empty();
    }
}