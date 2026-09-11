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

import bisq.common.data.Pair;
import bisq.common.facades.FacadeProvider;
import bisq.common.facades.android.AndroidJdkFacade;
import bisq.common.network.TransportType;
import bisq.common.util.ExceptionUtil;
import bisq.network.NetworkService;
import bisq.network.http.utils.HttpException;
import bisq.network.http.utils.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the retry/failover classification documented in {@link HttpRequestService}'s JavaDoc:
 * HTTP responses are server-level (4xx never retried; 5xx gated on the request descriptor),
 * everything else is transport-level and always retried. Both transports must feed the same
 * exception shape in — an {@link HttpException} root cause carrying status code and body —
 * for the classification to hold; these tests exercise the framework with exactly the shape
 * ClearNetHttpClient and TorHttpClient produce.
 */
class HttpRequestServiceTest {

    private static final String REJECTION_BODY =
            "{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\",\"isUnregistered\":true}";

    private final List<TestHttpRequestService> openedServices = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        // request() routes exceptionallyCompose through the JdkFacade, which is normally
        // registered at application bootstrap. The Android variant runs on any JVM.
        FacadeProvider.setJdkFacade(new AndroidJdkFacade(0));
    }

    @AfterEach
    void tearDown() {
        openedServices.forEach(s -> s.shutdown().join());
        openedServices.clear();
    }

    @Test
    void http400_isNotRetried_andRootCauseCarriesStatusAndBody() {
        AtomicInteger attempts = new AtomicInteger();
        TestHttpRequestService service = newService(HttpMethod.POST, false, attempts,
                () -> {
                    throw asClientWouldThrow(400, REJECTION_BODY);
                });

        CompletableFuture<String> future = service.request("data");

        assertThatThrownBy(future::join).satisfies(throwable -> {
            Throwable rootCause = ExceptionUtil.getRootCause(throwable);
            assertThat(rootCause).isInstanceOf(HttpException.class);
            HttpException httpException = (HttpException) rootCause;
            assertThat(httpException.getResponseCode()).isEqualTo(400);
            assertThat(httpException.getMessage()).isEqualTo(REJECTION_BODY);
        });
        assertThat(attempts).as("400 is a caller error — must not fail over to the second provider").hasValue(1);
    }

    @Test
    void http500_onNonIdempotentPost_isNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        TestHttpRequestService service = newService(HttpMethod.POST, false, attempts,
                () -> {
                    throw asClientWouldThrow(500, "internal error");
                });

        CompletableFuture<String> future = service.request("data");

        assertThatThrownBy(future::join).isNotNull();
        assertThat(attempts)
                .as("a 5xx POST without retryOnServerError may already have applied its side effect — must not retry")
                .hasValue(1);
    }

    @Test
    void http500_onPostOptedIntoServerErrorRetry_failsOverToOtherProvider() {
        AtomicInteger attempts = new AtomicInteger();
        TestHttpRequestService service = newService(HttpMethod.POST, true, attempts,
                () -> {
                    throw asClientWouldThrow(500, "internal error");
                });

        CompletableFuture<String> future = service.request("data");

        assertThatThrownBy(future::join).isNotNull();
        assertThat(attempts).hasValue(2);
    }

    @Test
    void http500_onGet_failsOverToOtherProvider() {
        AtomicInteger attempts = new AtomicInteger();
        TestHttpRequestService service = newService(HttpMethod.GET, false, attempts,
                () -> {
                    throw asClientWouldThrow(500, "internal error");
                });

        CompletableFuture<String> future = service.request("data");

        assertThatThrownBy(future::join).isNotNull();
        assertThat(attempts).hasValue(2);
    }

    @Test
    void transportFailure_onNonIdempotentPost_stillFailsOver() {
        AtomicInteger attempts = new AtomicInteger();
        TestHttpRequestService service = newService(HttpMethod.POST, false, attempts,
                () -> {
                    // No HttpException in the chain: the request never reached the server,
                    // so retrying cannot duplicate a side effect.
                    throw new IOException("connect: connection refused");
                });

        CompletableFuture<String> future = service.request("data");

        assertThatThrownBy(future::join).isNotNull();
        assertThat(attempts).hasValue(2);
    }

    @Test
    void http2xx_returnsParsedBody_withoutFailover() {
        AtomicInteger attempts = new AtomicInteger();
        TestHttpRequestService service = newService(HttpMethod.POST, false, attempts, () -> "ok-body");

        String result = service.request("data").join();

        assertThat(result).isEqualTo("parsed:ok-body");
        assertThat(attempts).hasValue(1);
    }

    /**
     * The exception shape both HTTP clients produce for a non-2xx response: the
     * {@link HttpException} carrying status and body, wrapped in the IOException their
     * catch-all rethrows (ClearNetHttpClient line pattern; TorHttpClient via
     * {@code httpResponseFailure} plus its outer wrap).
     */
    private static IOException asClientWouldThrow(int statusCode, String body) {
        return new IOException("Request failed", new HttpException(body, statusCode));
    }

    private TestHttpRequestService newService(HttpMethod method,
                                              boolean retryOnServerError,
                                              AtomicInteger attempts,
                                              StubResponse stubResponse) {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.CLEAR));
        when(networkService.getHttpClient(any(), any(), any(), any()))
                .thenAnswer(invocation -> new StubHttpClient(attempts, stubResponse));
        // Two providers so a failover is observable as a second attempt.
        HttpRequestServiceConfig conf = new HttpRequestServiceConfig(60L,
                Set.of(new HttpRequestUrlProvider("https://provider-1.example/", "op1", "/legacy", TransportType.CLEAR),
                        new HttpRequestUrlProvider("https://provider-2.example/", "op2", "/legacy", TransportType.CLEAR)),
                Set.of());
        TestHttpRequestService service = new TestHttpRequestService(conf, networkService, method, retryOnServerError);
        openedServices.add(service);
        return service;
    }

    @FunctionalInterface
    private interface StubResponse {
        String respond() throws IOException;
    }

    private static final class StubHttpClient extends BaseHttpClient {
        private final AtomicInteger attempts;
        private final StubResponse stubResponse;

        StubHttpClient(AtomicInteger attempts, StubResponse stubResponse) {
            super("https://stub.example", "https://stub.example", "test-agent");
            this.attempts = attempts;
            this.stubResponse = stubResponse;
        }

        @Override
        protected String doRequest(String param,
                                   HttpMethod httpMethod,
                                   Optional<Pair<String, String>> optionalHeader) throws IOException {
            attempts.incrementAndGet();
            return stubResponse.respond();
        }

        @Override
        public CompletableFuture<Boolean> shutdown() {
            return CompletableFuture.completedFuture(true);
        }
    }

    private static final class TestHttpRequestService extends HttpRequestService<String, String> {
        private final HttpMethod method;
        private final boolean retryOnServerError;

        TestHttpRequestService(HttpRequestServiceConfig conf,
                               NetworkService networkService,
                               HttpMethod method,
                               boolean retryOnServerError) {
            super(conf, networkService, Executors.newSingleThreadExecutor());
            this.method = method;
            this.retryOnServerError = retryOnServerError;
        }

        @Override
        protected String parseResult(String json) {
            return "parsed:" + json;
        }

        @Override
        protected HttpRequest buildRequest(HttpRequestUrlProvider provider, String requestData) {
            return method == HttpMethod.GET
                    ? HttpRequest.get("api/path")
                    : HttpRequest.post("/api/path", "{}", new Pair<>("Content-Type", "application/json"), retryOnServerError);
        }
    }
}
