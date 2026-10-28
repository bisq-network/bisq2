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
import bisq.network.http.utils.ResponseBodyTooLargeException;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TorHttpClientResponseTest {
    private static final int MAX_RESPONSE_BODY_BYTES = 1024;

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
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent", 1024, null);
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(400);
        response.setEntity(new StringEntity("{\"wasAccepted\":false}", StandardCharsets.UTF_8));

        assertThat(client.readBody(response)).isEqualTo("{\"wasAccepted\":false}");
    }

    @Test
    void readBody_normalizesMissingEntityToEmptyBody() throws IOException {
        // Responses without a body (204, some error responses) have a null entity; the
        // status code must still reach httpResponseFailure instead of an NPE in the handler.
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent", 1024, null);

        assertThat(client.readBody(new BasicClassicHttpResponse(500))).isEmpty();
    }

    @Test
    void processResponse_keepsTheStatusWhenTheErrorBodyIsUnreadable() {
        // A failed body read must not demote a server answer to a retriable transport
        // failure: the 400 still surfaces as an HttpException so retry gating holds.
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent", 1024, null);
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(400);
        response.setEntity(new InputStreamEntity(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("stream reset");
            }
        }, ContentType.APPLICATION_JSON));

        Throwable thrown = catchThrowable(() -> client.processResponse(response, "param", 0));

        assertThat(ExceptionUtil.getRootCause(thrown)).isInstanceOfSatisfying(HttpException.class,
                httpException -> assertThat(httpException.getResponseCode()).isEqualTo(400));
    }

    @Test
    void processResponse_keepsAnUnreadableSuccessBodyTransportLevel() {
        // A 2xx with an unreadable body has no result to return; it stays an IOException
        // (transport-level) rather than fabricating an HTTP failure.
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent", 1024, null);
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
        response.setEntity(new InputStreamEntity(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("stream reset");
            }
        }, ContentType.APPLICATION_JSON));

        Throwable thrown = catchThrowable(() -> client.processResponse(response, "param", 0));

        assertThat(thrown).isInstanceOf(IOException.class);
        assertThat(ExceptionUtil.getRootCause(thrown)).isNotInstanceOf(HttpException.class);
    }

    @Test
    void readBody_returnsABodyOfExactlyTheLimit() throws IOException {
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent",
                MAX_RESPONSE_BODY_BYTES, null);
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
        response.setEntity(new StringEntity("a".repeat(MAX_RESPONSE_BODY_BYTES), StandardCharsets.UTF_8));

        assertThat(client.readBody(response)).hasSize(MAX_RESPONSE_BODY_BYTES);
    }

    // A provider controls the size of its responses. The client stops at the limit instead of reading an endless body.
    @Test
    void readBody_stopsAtTheLimitOfAnEndlessBody() {
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent",
                MAX_RESPONSE_BODY_BYTES, null);
        AtomicLong numBytesServed = new AtomicLong();
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
        response.setEntity(new InputStreamEntity(endlessBody(numBytesServed), ContentType.APPLICATION_JSON));

        Throwable thrown = catchThrowable(() -> client.readBody(response));

        assertThat(thrown).isInstanceOf(ResponseBodyTooLargeException.class);
        // At most the limit and one buffer of the reader
        assertThat(numBytesServed.get()).isLessThan(64 * 1024);
    }

    @Test
    void processResponse_failsASuccessBodyAboveTheLimitAtTransportLevel() {
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent",
                MAX_RESPONSE_BODY_BYTES, null);
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
        response.setEntity(new StringEntity("a".repeat(MAX_RESPONSE_BODY_BYTES + 1), StandardCharsets.UTF_8));

        Throwable thrown = catchThrowable(() -> client.processResponse(response, "param", 0));

        assertThat(ExceptionUtil.getRootCause(thrown)).isInstanceOf(ResponseBodyTooLargeException.class);
    }

    @Test
    void processResponse_keepsTheStatusWhenTheErrorBodyIsAboveTheLimit() {
        TorHttpClient client = new TorHttpClient("http://x.onion", "http://x.onion", "test-agent",
                MAX_RESPONSE_BODY_BYTES, null);
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(503);
        response.setEntity(new InputStreamEntity(endlessBody(new AtomicLong()), ContentType.TEXT_HTML));

        Throwable thrown = catchThrowable(() -> client.processResponse(response, "param", 0));

        assertThat(ExceptionUtil.getRootCause(thrown)).isInstanceOfSatisfying(HttpException.class, httpException -> {
            assertThat(httpException.getResponseCode()).isEqualTo(503);
            assertThat(httpException.getBody()).isEmpty();
        });
    }

    @Test
    void httpResponseFailure_preservesStatusCodeAndBody() {
        String body = "{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\",\"isUnregistered\":true}";

        IOException failure = TorHttpClient.httpResponseFailure(400, body);

        assertThat(failure.getCause()).isInstanceOf(HttpException.class);
        HttpException httpException = (HttpException) failure.getCause();
        assertThat(httpException.getResponseCode()).isEqualTo(400);
        assertThat(httpException.getBody()).isEqualTo(body);
    }

    @Test
    void httpResponseFailure_preservesEmptyBody() {
        IOException failure = TorHttpClient.httpResponseFailure(503, "");

        HttpException httpException = (HttpException) failure.getCause();
        assertThat(httpException.getResponseCode()).isEqualTo(503);
        assertThat(httpException.getBody()).isEmpty();
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

    private static InputStream endlessBody(AtomicLong numBytesServed) {
        return new InputStream() {
            @Override
            public int read() {
                numBytesServed.incrementAndGet();
                return 'a';
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                Arrays.fill(buffer, offset, offset + length, (byte) 'a');
                numBytesServed.addAndGet(length);
                return length;
            }
        };
    }
}
