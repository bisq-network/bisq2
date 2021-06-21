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

package network.misq.network.http.common;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.data.Couple;
import network.misq.common.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class ClearNetHttpClient extends BaseHttpClient {
    private HttpURLConnection connection;

    public ClearNetHttpClient(String baseUrl, String userAgent) {
        super(baseUrl, userAgent);
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null) {
                connection.getInputStream().close();
                connection.disconnect();
                connection = null;
            }
        } catch (IOException ignore) {
        }
        hasPendingRequest = false;
    }

    @Override
    protected String doRequest(String param, HttpMethod httpMethod, Optional<Couple<String, String>> optionalHeader) throws IOException {
        checkArgument(!hasPendingRequest, "We got called on the same HttpClient again while a request is still open.");
        hasPendingRequest = true;

        long ts = System.currentTimeMillis();
        log.debug("requestWithoutProxy: URL={}, param={}, httpMethod={}", baseUrl, param, httpMethod);
        String spec = httpMethod == HttpMethod.GET ? baseUrl + param : baseUrl;
        try {
            URL url = new URL(spec);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(httpMethod.name());
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(30));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));
            connection.setRequestProperty("User-Agent", userAgent);
            optionalHeader.ifPresent(header -> {
                connection.setRequestProperty(header.first(), header.second());
            });

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
                        StringUtils.fileSizePrettyPrint(response.getBytes().length),
                        StringUtils.truncate(response, 2000));
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
        } catch (Throwable t) {
            String message = "Error at requestWithoutProxy with url " + baseUrl + " and param " + param +
                    ". Throwable=" + t.getMessage();
            throw new IOException(message, t);
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
