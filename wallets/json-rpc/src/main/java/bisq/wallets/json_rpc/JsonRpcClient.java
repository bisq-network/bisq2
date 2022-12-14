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

package bisq.wallets.json_rpc;

import bisq.wallets.json_rpc.exceptions.InvalidRpcCredentialsException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JsonRpcClient {

    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final int HTTP_CODE_UNAUTHORIZED = 401;

    private final JsonRpcEndpointSpec rpcEndpointSpec;

    private final OkHttpClient client;
    private final MediaType jsonMediaType = MediaType.parse("application/json");

    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<JsonRpcCall> jsonRpcCallJsonAdapter = moshi.adapter(JsonRpcCall.class);

    public JsonRpcClient(JsonRpcEndpointSpec rpcEndpointSpec) {
        this.rpcEndpointSpec = rpcEndpointSpec;

        var loggingInterceptor = new HttpLoggingInterceptor(log::info);
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.redactHeader(AUTHORIZATION_HEADER_NAME);

        this.client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .callTimeout(1, TimeUnit.MINUTES)
                .build();
    }

    public <T, R extends JsonRpcResponse<?>> R call(RpcCall<T, R> rpcCall) {
        JsonRpcCall jsonRpcCall = new JsonRpcCall(rpcCall.getRpcMethodName(), rpcCall.request);
        String jsonRequest = jsonRpcCallJsonAdapter.toJson(jsonRpcCall);
        Request request = buildRequest(jsonRequest);

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == HTTP_CODE_UNAUTHORIZED) {
                throw new InvalidRpcCredentialsException();
            }

            ResponseBody responseBody = response.body();
            Objects.requireNonNull(responseBody);

            JsonAdapter<R> jsonAdapter = rpcCall.getJsonAdapter();
            R parsedJsonResponse = jsonAdapter.fromJson(responseBody.source());

            if (!rpcCall.isResponseValid(parsedJsonResponse)) {
                String message = "RPC Call to '" + rpcCall.getRpcMethodName() + "' failed. ";
                if (parsedJsonResponse != null && parsedJsonResponse.getError() != null) {
                    message += parsedJsonResponse.getError().toString();
                }
                throw new RpcCallFailureException(message);
            }

            return parsedJsonResponse;

        } catch (IOException e) {
            throw new RpcCallFailureException(
                    "RPC Call to '" + rpcCall.getRpcMethodName() + "' failed. ",
                    e
            );
        }
    }

    private Request buildRequest(String body) {
        return new Request.Builder()
                .url(rpcEndpointSpec.getUrl())
                .addHeader(AUTHORIZATION_HEADER_NAME, rpcEndpointSpec.getAuthHeaderValue())
                .post(RequestBody.create(body, jsonMediaType))
                .build();
    }
}
