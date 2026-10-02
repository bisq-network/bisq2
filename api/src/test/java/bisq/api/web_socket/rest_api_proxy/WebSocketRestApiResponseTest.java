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
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The body of a proxied REST response can contain a private key, payment account data or chat messages. Users
 * share their log files, so a log statement which prints the response object must not write the body to the log.
 */
class WebSocketRestApiResponseTest {
    private static final String PRIVATE_VALUE = "private-sentinel-value";
    private static final String BODY = "{\"keyPair\":{\"privateKey\":{\"encoded\":\"" + PRIVATE_VALUE + "\"}}}";

    @Test
    void toStringPrintsTheLengthOfTheBodyAndNotTheBody() {
        String string = new WebSocketRestApiResponse("request-id", 200, BODY).toString();

        assertThat(string).doesNotContain(PRIVATE_VALUE);
        assertThat(string).contains("requestId=request-id", "statusCode=200", "bodyLength=" + BODY.length());
    }

    @Test
    void toStringCopesWithAMissingBody() {
        assertThat(new WebSocketRestApiResponse("request-id", 204, null).toString()).contains("bodyLength=0");
    }

    // The client still receives the body, and the message gets no property which a released client does not know
    @Test
    void jsonContainsTheBodyAndNoOtherNewProperty() throws Exception {
        String json = new WebSocketRestApiResponse("request-id", 200, BODY).toJson().orElseThrow();

        JsonNode node = JsonMapperProvider.get().readTree(json);
        assertThat(node.get("body").asText()).isEqualTo(BODY);
        assertThat(node.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("type", "requestId", "statusCode", "body");
    }
}
