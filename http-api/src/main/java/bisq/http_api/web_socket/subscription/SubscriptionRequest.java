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
public class SubscriptionRequest implements WebSocketMessage {
    private String requestId;
    private Topic topic;
    @Nullable
    private String parameter;

    public static Optional<SubscriptionRequest> fromJson(ObjectMapper objectMapper, String json) {
        try {
            return Optional.of(objectMapper.readValue(json, SubscriptionRequest.class));
        } catch (JsonProcessingException e) {
            log.error("Json deserialization failed. Message={}", json, e);
        }
        return Optional.empty();
    }

    public static boolean isExpectedJson(String message) {
        return message.contains("requestId") &&
                message.contains("topic");
    }
}