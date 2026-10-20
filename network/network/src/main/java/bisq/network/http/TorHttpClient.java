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
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.StringUtils;
import bisq.network.http.utils.HttpException;
import bisq.network.http.utils.HttpLogSanitizer;
import bisq.network.http.utils.HttpMethod;
import bisq.network.http.utils.Socks5ProxyProvider;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.*;

@Slf4j
public class TorHttpClient extends BaseHttpClient {
    private final Socks5ProxyProvider socks5ProxyProvider;
    private CloseableHttpClient closeableHttpClient;
    private volatile boolean shutdownStarted;
    private volatile HttpUriRequestBase pendingRequest;

    public TorHttpClient(String baseUrl, String logBaseUrl, String userAgent, Socks5ProxyProvider socks5ProxyProvider) {
        super(baseUrl, logBaseUrl, userAgent);
        this.socks5ProxyProvider = socks5ProxyProvider;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        // The volatile pendingRequest is read first: doRequest writes it after closeableHttpClient, so a
        // pendingRequest seen here also makes the client of that request visible to this thread
        if (pendingRequest == null && closeableHttpClient == null) {
            hasPendingRequest = false;
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        try {
                            // Cancelling the request closes its connection without the lock which
                            // BasicHttpClientConnectionManager holds while the SOCKS handshake waits for Tor to
                            // build the circuit. Closing the client alone would wait for that lock.
                            HttpUriRequestBase request = pendingRequest;
                            if (request != null) {
                                request.cancel();
                            }
                            if (closeableHttpClient != null) {
                                closeableHttpClient.close();
                                closeableHttpClient = null;
                            }
                        } catch (IOException ignore) {
                        }
                        return true;
                    } catch (Exception e) {
                        log.error("Error at shutdown", e);
                        return false;
                    }
                }, ExecutorFactory.newSingleThreadExecutor("TorHttpClient-shutdown"))
                .orTimeout(500, TimeUnit.MILLISECONDS);
        hasPendingRequest = false;
        return future;
    }

    @Override
    protected String doRequest(String param,
                               HttpMethod httpMethod,
                               Optional<Pair<String, String>> optionalHeader) throws IOException {
        checkArgument(!hasPendingRequest, "We got called on the same HttpClient again while a request is still open.");
        if (shutdownStarted) {
            return "";
        }

        hasPendingRequest = true;
        Socks5Proxy socks5Proxy = socks5ProxyProvider.getSocks5Proxy();

        long ts = System.currentTimeMillis();
        // Safe-to-log representation of param. For POST 'param' is the request
        // body (may contain plaintext payload), so log only its size. For GET
        // 'param' is the path; current callers do not embed secrets there, but
        // sensitive GET paths should be redacted by the caller via the
        // descriptor's logPath before reaching this layer.
        String safeParam = httpMethod == HttpMethod.POST
                ? "[body " + param.length() + " chars]"
                : param;
        log.debug("doRequestWithProxy: baseUrl={}, param={}, httpMethod={}", logBaseUrl, safeParam, httpMethod);

        InetSocketAddress socksAddress = new InetSocketAddress(socks5Proxy.getInetAddress(), socks5Proxy.getPort());
        // Use this to test with system-wide Tor proxy, or change port for another proxy.
        // SocketAddress socksAddress = new InetSocketAddress("127.0.0.1", 9050);
        // The client and its manager are built for this request and closed in finally, so a pooling manager never
        // got the chance to reuse a connection for a later request
        var cm = new BasicHttpClientConnectionManager(new TorHttpClientConnectionOperator(null,
                RegistryBuilder.<TlsSocketStrategy>create()
                        .register(URIScheme.HTTPS.id, DefaultClientTlsStrategy.createDefault())
                        .build()),
                null);
        cm.setSocketConfig(SocketConfig.custom()
                .setSocksProxyAddress(socksAddress)
                .setSoTimeout(Timeout.ofSeconds(30))
                .build());
        try {
            closeableHttpClient = checkNotNull(HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setResponseTimeout(Timeout.ofSeconds(30))
                            .build()) // Timeout waiting for response
                    .build());
            var uri = URI.create(baseUrl);

            // Create the appropriate HTTP request based on the method
            // Following the same pattern as ClearNetHttpClient:
            // - For GET: param is the URL path/query, appended to baseUrl
            // - For POST: param is the request body, baseUrl includes the full path
            HttpUriRequestBase request;
            if (httpMethod == HttpMethod.POST) {
                // For POST, use the full URI path from baseUrl
                String path = uri.getPath();
                if (path == null || path.isEmpty()) {
                    path = "/";
                }
                HttpPost postRequest = new HttpPost(path);
                postRequest.setEntity(new StringEntity(param, StandardCharsets.UTF_8));
                request = postRequest;
            } else {
                // For GET, append param to the path
                request = new HttpGet("/" + param);
            }

            optionalHeader.ifPresent(header -> request.setHeader(header.getFirst(), header.getSecond()));
            pendingRequest = request;
            var target = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
            return closeableHttpClient.execute(target, request, response -> processResponse(response, safeParam, ts));
        } catch (Throwable t) {
            String message = "Error at doRequestWithProxy with url " + logBaseUrl + " and param " + safeParam +
                    ". Throwable=" + t.getMessage();
            throw new IOException(message, t);
        } finally {
            pendingRequest = null;
            if (closeableHttpClient != null) {
                closeableHttpClient.close();
                closeableHttpClient = null;
            }
            hasPendingRequest = false;
        }
    }

    /**
     * See {@link HttpLogSanitizer}: the shared sanitizer for bodies headed into a log line.
     * The raw body keeps flowing to callers through {@link HttpException#getBody()}.
     */
    static String loggableBody(String responseBody) {
        return HttpLogSanitizer.loggableBody(responseBody);
    }

    /**
     * The status code is read before the body so a failed body read cannot demote a server
     * answer to a transport failure: a non-2xx whose body is unreadable still surfaces as an
     * {@link HttpException} with its status, keeping its status-based classification and retry
     * gate. Only a 2xx with an unreadable body stays an IOException — there is no result to
     * return and the caller may treat it as transport-level.
     */
    String processResponse(ClassicHttpResponse response, String safeParam, long ts) throws IOException {
        int statusCode = response.getCode();
        String responseString;
        try {
            responseString = readBody(response);
        } catch (IOException e) {
            if (isSuccess(statusCode)) {
                throw e;
            }
            log.info("Received unreadable body ({}) with statusCode {} from {}. param: {}",
                    e.getClass().getSimpleName(),
                    statusCode,
                    logBaseUrl,
                    safeParam);
            throw httpResponseFailure(statusCode, "");
        }
        if (isSuccess(statusCode)) {
            log.debug("Response from {} took {} ms. Data size:{}, response: {}, param: {}",
                    logBaseUrl,
                    System.currentTimeMillis() - ts,
                    StringUtils.fromBytes(responseString.getBytes().length),
                    loggableBody(responseString),
                    safeParam);
            return responseString;
        }
        log.info("Received errorMsg '{}' with statusCode {} from {}. Response took: {} ms. param: {}",
                loggableBody(responseString),
                statusCode,
                logBaseUrl,
                System.currentTimeMillis() - ts,
                safeParam);
        throw httpResponseFailure(statusCode, responseString);
    }

    /**
     * Responses without a body (204, some error responses) carry a null entity — normalized
     * to an empty body so the status code still reaches {@link #httpResponseFailure} instead
     * of an NPE escaping the response handler.
     */
    String readBody(ClassicHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        return entity == null ? "" : inputStreamToString(entity.getContent());
    }

    /**
     * Non-2xx responses surface with their status code and body in an {@link HttpException},
     * matching ClearNetHttpClient, so that {@link HttpRequestService} classifies them as
     * server-level (by status code and request method, see its class Javadoc) rather than as a
     * retriable transport failure. HttpException is checked and Apache's response handler only
     * permits IOException, so it travels as the cause; root-cause extraction recovers it upstream.
     */
    static IOException httpResponseFailure(int statusCode, String responseBody) {
        return new IOException(new HttpException(responseBody, statusCode));
    }
}
