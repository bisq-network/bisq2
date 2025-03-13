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
import bisq.network.http.utils.HttpMethod;
import bisq.network.http.utils.Socks5ProxyProvider;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.*;

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
        log.debug("doRequestWithProxy: baseUrl={}, param={}, httpMethod={}", baseUrl, param, httpMethod);

        InetSocketAddress socksAddress = new InetSocketAddress(socks5Proxy.getInetAddress(), socks5Proxy.getPort());
        // Use this to test with system-wide Tor proxy, or change port for another proxy.
        // SocketAddress socksAddress = new InetSocketAddress("127.0.0.1", 9050);
        var cm = new PoolingTorHttpClientConnectionManager();
        cm.setDefaultSocketConfig(SocketConfig.custom()
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
            var request = new HttpGet("/" + param);
            optionalHeader.ifPresent(header -> request.setHeader(header.getFirst(), header.getSecond()));
            var target = new HttpHost(uri.getScheme(), uri.getHost());
            return closeableHttpClient.execute(target, request, response -> {
                String responseString = inputStreamToString(response.getEntity().getContent());
                int statusCode = response.getCode();
                if (isSuccess(statusCode)) {
                    log.debug("Response from {} took {} ms. Data size:{}, response: {}, param: {}",
                            baseUrl,
                            System.currentTimeMillis() - ts,
                            StringUtils.fromBytes(responseString.getBytes().length),
                            StringUtils.truncate(response, 2000),
                            param);
                    return responseString;
                }
                log.info("Received errorMsg '{}' with statusCode {} from {}. Response took: {} ms. param: {}",
                        responseString,
                        statusCode,
                        baseUrl,
                        System.currentTimeMillis() - ts,
                        param);
                throw new RuntimeException(responseString);
            });
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
}
