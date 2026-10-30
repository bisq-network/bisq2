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
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ClearNetHttpClientTest {
    private static final int MAX_RESPONSE_BODY_BYTES = 1024;

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        serve("/limit", 200, "a".repeat(MAX_RESPONSE_BODY_BYTES));
        serve("/aboveLimit", 200, "a".repeat(MAX_RESPONSE_BODY_BYTES + 1));
        serve("/error", 503, "overloaded");
        serve("/errorAboveLimit", 503, "a".repeat(MAX_RESPONSE_BODY_BYTES + 1));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsABodyOfExactlyTheLimit() throws IOException {
        ClearNetHttpClient client = new ClearNetHttpClient(baseUrl, baseUrl, "test-agent", MAX_RESPONSE_BODY_BYTES);

        assertThat(client.get("limit", Optional.empty())).hasSize(MAX_RESPONSE_BODY_BYTES);
    }

    @Test
    void failsABodyAboveTheLimit() {
        ClearNetHttpClient client = new ClearNetHttpClient(baseUrl, baseUrl, "test-agent", MAX_RESPONSE_BODY_BYTES);

        Throwable thrown = catchThrowable(() -> client.get("aboveLimit", Optional.empty()));

        assertThat(thrown).isInstanceOf(IOException.class);
        assertThat(ExceptionUtil.getRootCause(thrown)).isInstanceOf(ResponseBodyTooLargeException.class);
    }

    // The status code of an error response decides about retries, see HttpRequestService
    @Test
    void errorResponseKeepsItsStatusAndBody() {
        ClearNetHttpClient client = new ClearNetHttpClient(baseUrl, baseUrl, "test-agent", MAX_RESPONSE_BODY_BYTES);

        Throwable thrown = catchThrowable(() -> client.get("error", Optional.empty()));

        assertThat(ExceptionUtil.getRootCause(thrown)).isInstanceOfSatisfying(HttpException.class, httpException -> {
            assertThat(httpException.getResponseCode()).isEqualTo(503);
            assertThat(httpException.getBody()).isEqualTo("overloaded");
        });
    }

    // As in TorHttpClient: a body which the client does not read does not turn the answer into a transport failure
    @Test
    void errorResponseWhoseBodyIsAboveTheLimitKeepsItsStatus() {
        ClearNetHttpClient client = new ClearNetHttpClient(baseUrl, baseUrl, "test-agent", MAX_RESPONSE_BODY_BYTES);

        Throwable thrown = catchThrowable(() -> client.get("errorAboveLimit", Optional.empty()));

        assertThat(ExceptionUtil.getRootCause(thrown)).isInstanceOfSatisfying(HttpException.class, httpException -> {
            assertThat(httpException.getResponseCode()).isEqualTo(503);
            assertThat(httpException.getBody()).isEmpty();
        });
    }

    private void serve(String path, int statusCode, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.US_ASCII);
        server.createContext(path, exchange -> {
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
    }
}
