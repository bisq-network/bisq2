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

package bisq.network.http;

import bisq.common.util.ExceptionUtil;
import bisq.network.http.utils.HttpException;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TorHttpClientTest {

    @Test
    void loggableBody_neutralizesControlCharactersAndCapsLength() {
        assertThat(TorHttpClient.loggableBody("{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\"}"))
                .isEqualTo("{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\"}");
        // CR/LF forge fake log lines; ESC starts terminal escape sequences in a followed console.
        assertThat(TorHttpClient.loggableBody("x\r\n2026-09-11 INFO forged line\u001B[31mred\ttab"))
                .doesNotContain("\r", "\n", "\u001B", "\t");
        assertThat(TorHttpClient.loggableBody("A".repeat(5000))).hasSize(2000);
    }

    @Test
    void readBody_returnsContent_whenEntityPresent() throws IOException {
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent", null);
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(400);
        response.setEntity(new StringEntity("{\"wasAccepted\":false}", StandardCharsets.UTF_8));

        assertThat(client.readBody(response)).isEqualTo("{\"wasAccepted\":false}");
    }

    @Test
    void readBody_normalizesMissingEntityToEmptyBody() throws IOException {
        // Responses without a body (204, some error responses) have a null entity; the
        // status code must still reach httpResponseFailure instead of an NPE in the handler.
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent", null);

        assertThat(client.readBody(new BasicClassicHttpResponse(500))).isEmpty();
    }

    @Test
    void httpResponseFailure_preservesStatusCodeAndBody() {
        String body = "{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\",\"isUnregistered\":true}";

        IOException failure = TorHttpClient.httpResponseFailure(400, body);

        assertThat(failure.getCause()).isInstanceOf(HttpException.class);
        HttpException httpException = (HttpException) failure.getCause();
        assertThat(httpException.getResponseCode()).isEqualTo(400);
        assertThat(httpException.getMessage()).isEqualTo(body);
    }

    @Test
    void httpResponseFailure_preservesEmptyBody() {
        IOException failure = TorHttpClient.httpResponseFailure(503, "");

        HttpException httpException = (HttpException) failure.getCause();
        assertThat(httpException.getResponseCode()).isEqualTo(503);
        assertThat(httpException.getMessage()).isEmpty();
    }

    @Test
    void httpResponseFailure_isRecoverableAsRootCauseThroughOuterWrapping() {
        // doRequest's catch (Throwable t) re-wraps everything in an IOException with the
        // original as cause; HttpRequestService's server-level classification depends on
        // finding the HttpException at the root of that chain.
        IOException outer = new IOException("Error at doRequestWithProxy with url http://x.onion",
                TorHttpClient.httpResponseFailure(500, "overloaded"));

        Throwable rootCause = ExceptionUtil.getRootCause(outer);

        assertThat(rootCause).isInstanceOf(HttpException.class);
        assertThat(((HttpException) rootCause).getResponseCode()).isEqualTo(500);
        assertThat(rootCause.getMessage()).isEqualTo("overloaded");
    }
}
