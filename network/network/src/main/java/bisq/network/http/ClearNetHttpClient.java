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
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.network.http.utils.HttpException;
import bisq.network.http.utils.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class ClearNetHttpClient extends BaseHttpClient {
    private Proxy proxy;
    private HttpURLConnection connection;

    public ClearNetHttpClient(String baseUrl, String userAgent) {
        super(baseUrl, userAgent);
    }

    public ClearNetHttpClient(String baseUrl, String userAgent, Proxy proxy) {
        super(baseUrl, userAgent);
        this.proxy = proxy;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (connection == null) {
            hasPendingRequest = false;
            return CompletableFuture.completedFuture(true);
        }
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        if (connection != null) {
                            // blocking call if connection has issues
                            connection.getInputStream().close();
                            if (connection != null) {
                                connection.disconnect();
                            }
                            return true;
                        } else {
                            return false;
                        }
                    } catch (IOException e) {
                        log.error("Error at shutdown {}", ExceptionUtil.getRootCauseMessage(e));
                        return false;
                    }
                }, ExecutorFactory.newSingleThreadExecutor("ClearNetHttpClient-shutdown"))
                .orTimeout(500, TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Error at shutdown: {}", ExceptionUtil.getRootCauseMessage(throwable));
                    }
                });
        connection = null;
        return future;
    }

    @Override
    protected String doRequest(String param, HttpMethod httpMethod, Optional<Pair<String, String>> optionalHeader) throws IOException {
        checkArgument(!hasPendingRequest, "We got called on the same HttpClient again while a request is still open.");
        hasPendingRequest = true;

        long ts = System.currentTimeMillis();
        log.debug("requestWithoutProxy: URL={}, param={}, httpMethod={}", baseUrl, param, httpMethod);
        String spec = httpMethod == HttpMethod.GET ? baseUrl + "/" + param : baseUrl;
        try {
            URL url = new URI(spec).toURL();
            if (proxy == null) {
                connection = (HttpURLConnection) url.openConnection();
            } else {
                // Allows I2P connections
                // Translation across networks happens via an HTTP proxy exposed by the I2P router
                connection = (HttpURLConnection) url.openConnection(proxy);
            }
            connection.setRequestMethod(httpMethod.name());
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(30));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));
            connection.setRequestProperty("User-Agent", userAgent);
            optionalHeader.ifPresent(header -> connection.setRequestProperty(header.getFirst(), header.getSecond()));

            if (httpMethod == HttpMethod.POST) {
                connection.setDoOutput(true);
                connection.getOutputStream().write(param.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (isSuccess(responseCode)) {
                String response = inputStreamToString(connection.getInputStream());
                log.debug("Response from {} with param {} took {} ms. Data size:{}, response: {}",
                        baseUrl,
                        param,
                        System.currentTimeMillis() - ts,
                        StringUtils.fromBytes(response.getBytes().length),
                        StringUtils.truncate(response, 100));
                return response;
            }

            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                String error = inputStreamToString(errorStream);
                errorStream.close();
                log.info("Received errorMsg '{}' with responseCode {} from {}. Response took: {} ms. param: {}",
                        error,
                        responseCode,
                        baseUrl,
                        System.currentTimeMillis() - ts,
                        param);
                throw new HttpException(error, responseCode);
            } else {
                log.info("Response with responseCode {} from {}. Response took: {} ms. param: {}",
                        responseCode,
                        baseUrl,
                        System.currentTimeMillis() - ts,
                        param);
                throw new HttpException("Request failed", responseCode);
            }
        } catch (Exception e) {
            String message = "Request to " + baseUrl + "/" + param + " failed with error: " + ExceptionUtil.getRootCauseMessage(e);
            throw new IOException(message, e);
        } finally {
            try {
                if (connection != null) {
                    connection.getInputStream().close();
                    connection.disconnect();
                    connection = null;
                }
            } catch (Throwable ignore) {
            }
            hasPendingRequest = false;
        }
    }
}
