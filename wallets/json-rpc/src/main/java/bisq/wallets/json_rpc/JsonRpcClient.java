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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okhttp3.*;

import java.io.IOException;
import java.util.Objects;

public class JsonRpcClient {

    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private final JsonRpcEndpointSpec rpcEndpointSpec;

    private final OkHttpClient client = new OkHttpClient();
    private final MediaType jsonMediaType = MediaType.parse("application/json");

    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<JsonRpcCall> jsonRpcCallJsonAdapter = moshi.adapter(JsonRpcCall.class);

    public JsonRpcClient(JsonRpcEndpointSpec rpcEndpointSpec) {
        this.rpcEndpointSpec = rpcEndpointSpec;
    }

    public <T, R> R call(RpcCall<T, R> rpcCall) throws IOException {
        JsonRpcCall jsonRpcCall = new JsonRpcCall(rpcCall.getRpcMethodName(), rpcCall.request);
        String jsonRequest = jsonRpcCallJsonAdapter.toJson(jsonRpcCall);
        Request request = buildRequest(jsonRequest);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            ResponseBody responseBody = response.body();
            Objects.requireNonNull(responseBody);

            JsonAdapter<R> jsonAdapter = rpcCall.getJsonAdapter();
            return jsonAdapter.fromJson(responseBody.source());
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
