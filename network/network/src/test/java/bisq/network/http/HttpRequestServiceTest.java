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
import bisq.common.facades.JdkFacade;
import bisq.common.facades.android.AndroidJdkFacade;
import bisq.common.network.TransportType;
import bisq.network.NetworkService;
import bisq.network.http.utils.HttpException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Isolated("Replaces the process-wide JDK facade")
class HttpRequestServiceTest {
    private static final int MAX_RESPONSE_BODY_BYTES = 4096;

    private NetworkService networkService;
    private BaseHttpClient client;
    private HttpRequestService<String, String> service;
    private JdkFacade previousJdkFacade;

    @BeforeEach
    void setUp() {
        try {
            previousJdkFacade = FacadeProvider.getJdkFacade();
        } catch (NullPointerException ignored) {
            // The facade is normally installed by the application, which this unit test does not start.
        }
        FacadeProvider.setJdkFacade(new AndroidJdkFacade(0));
        networkService = mock(NetworkService.class);
        client = mock(BaseHttpClient.class);
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.CLEAR));
        when(networkService.getHttpClient(anyString(), anyString(), anyString(), any(), anyInt())).thenReturn(client);
        when(client.shutdown()).thenReturn(CompletableFuture.completedFuture(true));
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown().join();
        }
        FacadeProvider.setJdkFacade(previousJdkFacade);
    }

    @Test
    void nonIdempotentPostDoesNotRetryWhenResponseReadTimesOut() throws IOException {
        when(client.post(anyString(), any())).thenThrow(new SocketTimeoutException("Response read timed out"));
        createService(post(false), false);

        assertThrows(CompletionException.class, () -> service.request("request").join());

        verify(client, times(1)).post(anyString(), any());
    }

    @Test
    void nonIdempotentPostDoesNotRetryWhenResponseParsingFails() throws IOException {
        when(client.post(anyString(), any())).thenReturn("malformed response");
        createService(post(false), true);

        assertThrows(CompletionException.class, () -> service.request("request").join());

        verify(client, times(1)).post(anyString(), any());
    }

    @Test
    void nonIdempotentPostDoesNotRetryServerFailure() throws IOException {
        when(client.post(anyString(), any())).thenThrow(new IOException(new HttpException("Unavailable", 503)));
        createService(post(false), false);

        assertThrows(CompletionException.class, () -> service.request("request").join());

        verify(client, times(1)).post(anyString(), any());
    }

    @Test
    void postWithRetryOptInRetriesResponseReadFailure() throws IOException {
        when(client.post(anyString(), any())).thenThrow(new SocketTimeoutException("Response read timed out"))
                .thenReturn("ok");
        createService(post(true), false);

        assertEquals("ok", service.request("request").join());

        verify(client, times(2)).post(anyString(), any());
    }

    // The provider controls the size of its responses, so each client reads at most the limit of the service
    @Test
    void clientReadsAtMostTheResponseBodyLimitOfTheService() throws IOException {
        when(client.get(anyString(), any())).thenReturn("ok");
        createService(HttpRequest.get("path"), false);

        assertEquals("ok", service.request("request").join());

        verify(networkService).getHttpClient(anyString(), anyString(), anyString(), any(), eq(MAX_RESPONSE_BODY_BYTES));
    }

    @Test
    void getRetriesResponseReadFailure() throws IOException {
        when(client.get(anyString(), any())).thenThrow(new SocketTimeoutException("Response read timed out"))
                .thenReturn("ok");
        createService(HttpRequest.get("path"), false);

        assertEquals("ok", service.request("request").join());

        verify(client, times(2)).get(anyString(), any());
    }

    @Test
    void nonIdempotentPostCanFailOverBeforeCallingBusyClient() throws IOException {
        when(client.hasPendingRequest()).thenReturn(true, false);
        when(client.post(anyString(), any())).thenReturn("ok");
        createService(post(false), false);

        assertEquals("ok", service.request("request").join());

        verify(networkService, times(2)).getHttpClient(anyString(), anyString(), anyString(), any(), anyInt());
        verify(client, times(1)).post(anyString(), any());
    }

    @Test
    void callerErrorDoesNotRetryEvenWithOptIn() throws IOException {
        when(client.post(anyString(), any())).thenThrow(new IOException(new HttpException("Bad request", 400)));
        createService(post(true), false);

        assertThrows(CompletionException.class, () -> service.request("request").join());

        verify(client, times(1)).post(anyString(), any());
    }

    @Test
    void getFailsOverAfterClientError() throws IOException {
        when(client.get(anyString(), any())).thenThrow(new IOException(new HttpException("Transaction not found", 404)))
                .thenReturn("ok");
        createService(HttpRequest.get("path"), false);

        assertEquals("ok", service.request("request").join());

        verify(client, times(2)).get(anyString(), any());
    }

    @Test
    void laterGetSkipsProviderWhichAnsweredClientError() throws IOException {
        // The only configured provider answers 404. Before, the service failed without asking the
        // fallback provider and sent every later request to the same provider again.
        BaseHttpClient fallbackClient = mock(BaseHttpClient.class);
        when(fallbackClient.shutdown()).thenReturn(CompletableFuture.completedFuture(true));
        when(fallbackClient.get(anyString(), any())).thenReturn("ok");
        when(networkService.getHttpClient(startsWith("https://fallback.example"), anyString(), anyString(), any(), anyInt()))
                .thenReturn(fallbackClient);
        when(client.get(anyString(), any())).thenThrow(new IOException(new HttpException("Not found", 404)));
        createService(HttpRequest.get("path"), false,
                Set.of(provider("https://primary.example", "primary")),
                Set.of(provider("https://fallback.example", "fallback")));

        assertEquals("ok", service.request("request").join());
        assertEquals("ok", service.request("request").join());

        verify(client, times(1)).get(anyString(), any());
        verify(fallbackClient, times(2)).get(anyString(), any());
    }

    @Test
    void shutdownStopsTheExecutorAlsoWhenTheClientFailsToShutDown() throws IOException {
        // TorHttpClient fails its shutdown after 500 ms with a TimeoutException.
        when(client.get(anyString(), any())).thenReturn("ok");
        when(client.shutdown()).thenReturn(CompletableFuture.failedFuture(new TimeoutException("timeout")));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        createService(HttpRequest.get("path"), false, Set.of(provider("https://first.example", "first")), Set.of(),
                executor);
        assertEquals("ok", service.request("request").join());

        assertFalse(service.shutdown().join());
        assertTrue(executor.isShutdown());
        service = null;
    }

    private static HttpRequest post(boolean retryOnServerError) {
        return HttpRequest.post("path", "body", new Pair<>("Content-Type", "application/json"), retryOnServerError);
    }

    private static HttpRequestUrlProvider provider(String baseUrl, String operator) {
        return new HttpRequestUrlProvider(baseUrl, operator, "", TransportType.CLEAR);
    }

    private void createService(HttpRequest request, boolean failParsing) {
        createService(request, failParsing,
                Set.of(provider("https://first.example", "first"), provider("https://second.example", "second")),
                Set.of());
    }

    private void createService(HttpRequest request,
                               boolean failParsing,
                               Set<HttpRequestUrlProvider> providers,
                               Set<HttpRequestUrlProvider> fallbackProviders) {
        createService(request, failParsing, providers, fallbackProviders, Executors.newSingleThreadExecutor());
    }

    private void createService(HttpRequest request,
                               boolean failParsing,
                               Set<HttpRequestUrlProvider> providers,
                               Set<HttpRequestUrlProvider> fallbackProviders,
                               ExecutorService executor) {
        HttpRequestServiceConfig config = new HttpRequestServiceConfig(30, providers, fallbackProviders);
        service = new HttpRequestService<>(config, networkService, executor, MAX_RESPONSE_BODY_BYTES) {
            @Override
            protected String parseResult(String json) {
                if (failParsing) {
                    throw new IllegalArgumentException("Malformed response");
                }
                return json;
            }

            @Override
            protected HttpRequest buildRequest(HttpRequestUrlProvider provider, String requestData) {
                return request;
            }
        };
    }
}
