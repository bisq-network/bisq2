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
import bisq.network.http.utils.*;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.*;

// TODO (Critical) close connection if failing
@Slf4j
public class TorHttpClient extends BaseHttpClient {
    private final Socks5ProxyProvider socks5ProxyProvider;
    private CloseableHttpClient closeableHttpClient;
    private volatile boolean shutdownStarted;

    public TorHttpClient(String baseUrl, String userAgent, Socks5ProxyProvider socks5ProxyProvider) {
        super(baseUrl, userAgent);
        this.socks5ProxyProvider = socks5ProxyProvider;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        if (closeableHttpClient == null) {
            hasPendingRequest = false;
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        try {
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
    protected String doRequest(String param, HttpMethod httpMethod, Optional<Pair<String, String>> optionalHeader) throws IOException {
        checkArgument(!hasPendingRequest, "We got called on the same HttpClient again while a request is still open.");
        if (shutdownStarted) {
            return "";
        }

        hasPendingRequest = true;
        Socks5Proxy socks5Proxy = socks5ProxyProvider.getSocks5Proxy();

        long ts = System.currentTimeMillis();
        log.debug("doRequestWithProxy: baseUrl={}, param={}, httpMethod={}", baseUrl, param, httpMethod);
        // This code is adapted from:
        //  http://stackoverflow.com/a/25203021/5616248

        // Register our own SocketFactories to override createSocket() and connectSocket().
        // connectSocket does NOT resolve hostname before passing it to proxy.
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new SocksConnectionSocketFactory())
                .register("https", new SocksSSLConnectionSocketFactory(SSLContexts.createSystemDefault())).build();

        // Use FakeDNSResolver if not resolving DNS locally.
        // This prevents a local DNS lookup (which would be ignored anyway)
        PoolingHttpClientConnectionManager cm = socks5Proxy.resolveAddrLocally() ?
                new PoolingHttpClientConnectionManager(reg) :
                new PoolingHttpClientConnectionManager(reg, new FakeDnsResolver());
        try {
            closeableHttpClient = checkNotNull(HttpClients.custom().setConnectionManager(cm).build());
            InetSocketAddress socksAddress = new InetSocketAddress(socks5Proxy.getInetAddress(), socks5Proxy.getPort());

            // Use this to test with system-wide Tor proxy, or change port for another proxy.
            // InetSocketAddress socksAddress = new InetSocketAddress("127.0.0.1", 9050);

            HttpClientContext context = HttpClientContext.create();
            context.setAttribute("socks.address", socksAddress);

            HttpUriRequest request = getHttpUriRequest(httpMethod, baseUrl, param);
            optionalHeader.ifPresent(header -> request.setHeader(header.getFirst(), header.getSecond()));

            try (CloseableHttpResponse httpResponse = closeableHttpClient.execute(request, context)) {
                String response = inputStreamToString(httpResponse.getEntity().getContent());
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (isSuccess(statusCode)) {
                    log.debug("Response from {} took {} ms. Data size:{}, response: {}, param: {}",
                            baseUrl,
                            System.currentTimeMillis() - ts,
                            StringUtils.fromBytes(response.getBytes().length),
                            StringUtils.truncate(response, 2000),
                            param);
                    return response;
                }

                log.info("Received errorMsg '{}' with statusCode {} from {}. Response took: {} ms. param: {}",
                        response,
                        statusCode,
                        baseUrl,
                        System.currentTimeMillis() - ts,
                        param);
                throw new HttpException(response, statusCode);
            }
        } catch (Throwable t) {
            String message = "Error at doRequestWithProxy with url " + baseUrl + " and param " + param +
                    ". Throwable=" + t.getMessage();
            throw new IOException(message, t);
        } finally {
            if (closeableHttpClient != null) {
                closeableHttpClient.close();
                closeableHttpClient = null;
            }
            hasPendingRequest = false;
        }
    }

    protected HttpUriRequest getHttpUriRequest(HttpMethod httpMethod, String baseUrl, String param)
            throws UnsupportedEncodingException {
        switch (httpMethod) {
            case GET:
                return new HttpGet(baseUrl + "/" + param);
            case POST:
                HttpPost httpPost = new HttpPost(baseUrl);
                HttpEntity httpEntity = new StringEntity(param);
                httpPost.setEntity(httpEntity);
                return httpPost;

            default:
                throw new IllegalArgumentException("HttpMethod not supported: " + httpMethod);
        }
    }
}
