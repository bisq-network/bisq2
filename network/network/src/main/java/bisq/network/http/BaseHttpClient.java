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
import bisq.common.util.StringUtils;
import bisq.network.http.utils.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

@Slf4j
public abstract class BaseHttpClient implements HttpClient {
    public final String baseUrl;
    public final String userAgent;
    protected final String uid;

    public boolean hasPendingRequest;

    public BaseHttpClient(String baseUrl, String userAgent) {
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;

        uid = StringUtils.createUid();
    }

    @Override
    public String get(String param, Optional<Pair<String, String>> optionalHeader) throws IOException {
        return doRequest(param, HttpMethod.GET, optionalHeader);
    }

    @Override
    public String post(String param, Optional<Pair<String, String>> optionalHeader) throws IOException {
        return doRequest(param, HttpMethod.POST, optionalHeader);
    }

    @Override
    public boolean hasPendingRequest() {
        return hasPendingRequest;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    protected abstract String doRequest(String param,
                                        HttpMethod httpMethod,
                                        Optional<Pair<String, String>> optionalHeader) throws IOException;

    protected String inputStreamToString(InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }
    }

    protected boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
