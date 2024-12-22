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

package bisq.http_api.web_socket;

import bisq.http_api.web_socket.rest_api_proxy.WebSocketRestApiRequest;
import bisq.http_api.web_socket.rest_api_proxy.WebSocketRestApiResponse;
import bisq.http_api.web_socket.subscription.SubscriptionRequest;
import bisq.http_api.web_socket.subscription.SubscriptionResponse;
import bisq.http_api.web_socket.subscription.WebSocketEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = WebSocketRestApiRequest.class, name = "WebSocketRestApiRequest"),
        @JsonSubTypes.Type(value = WebSocketRestApiResponse.class, name = "WebSocketRestApiResponse"),
        @JsonSubTypes.Type(value = SubscriptionRequest.class, name = "SubscriptionRequest"),
        @JsonSubTypes.Type(value = SubscriptionResponse.class, name = "SubscriptionResponse"),
        @JsonSubTypes.Type(value = WebSocketEvent.class, name = "WebSocketEvent")
})
public interface WebSocketMessage {
}

