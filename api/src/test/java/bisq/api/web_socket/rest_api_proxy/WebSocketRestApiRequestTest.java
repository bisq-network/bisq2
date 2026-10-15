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

import bisq.common.json.JsonMapperProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The body of a proxied REST request can contain payment account data or a chat message. Users share their log
 * files, so a log statement which prints the request object must not write the body to the log.
 */
class WebSocketRestApiRequestTest {
    private static final String PRIVATE_VALUE = "private-sentinel-value";
    private static final String BODY = "{\"accountData\":\"" + PRIVATE_VALUE + "\"}";

    @Test
    void toStringPrintsTheLengthOfTheBodyAndNotTheBody() {
        WebSocketRestApiRequest request = parse(requestNode().put("body", BODY));

        // The proxy still gets the body for the request which it forwards
        assertThat(request.getBody()).isEqualTo(BODY);
        String string = request.toString();
        assertThat(string).doesNotContain(PRIVATE_VALUE);
        assertThat(string).contains("requestId=request-id",
                "method=POST",
                "path=/api/v1/payment-accounts/fiat",
                "bodyLength=" + BODY.length());
    }

    @Test
    void toStringCopesWithAMissingBody() {
        assertThat(parse(requestNode()).toString()).contains("bodyLength=0");
    }

    private static ObjectNode requestNode() {
        return JsonMapperProvider.get().createObjectNode()
                .put("type", "WebSocketRestApiRequest")
                .put("requestId", "request-id")
                .put("method", "POST")
                .put("path", "/api/v1/payment-accounts/fiat");
    }

    private static WebSocketRestApiRequest parse(ObjectNode requestNode) {
        return WebSocketRestApiRequest.fromJson(requestNode.toString()).orElseThrow();
    }
}
