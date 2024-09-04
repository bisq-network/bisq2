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

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonRpcClientTest {

    private final MockWebServer server = new MockWebServer();

    @BeforeEach
    void setUp() throws IOException {
        String mockResponse = "{\"result\":{\"chain\":\"regtest\",\"blocks\":102,\"headers\":102,\"bestblockhash\":\"5e5bcaaa2690bf99da1046334e4a2d783901608718a5b647846e67ffe0802bc8\",\"difficulty\":4.656542373906925e-10,\"time\":1663343392,\"mediantime\":1663060997,\"verificationprogress\":1,\"initialblockdownload\":true,\"chainwork\":\"00000000000000000000000000000000000000000000000000000000000000ce\",\"size_on_disk\":31048,\"pruned\":false,\"warnings\":\"\"},\"error\":null,\"id\":\"curltest\"}";
        server.enqueue(new MockResponse().setBody(mockResponse));
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void dummyGetBlockchainInfoTest() throws Exception {
        HttpUrl baseUrl = server.url("/");
        JsonRpcEndpointSpec endpointSpec = new JsonRpcEndpointSpec(baseUrl, "bisq", "bisq");
        JsonRpcClient jsonRpcClient = new JsonRpcClient(endpointSpec);

        var rpcCall = new DummyGetBlockChainInfoRpcCall();
        DummyJsonRpcResponse.Result dummyJsonRpcResponse = jsonRpcClient.call(rpcCall).getResult();

        assertThat(dummyJsonRpcResponse).isNotNull();
        assertThat(dummyJsonRpcResponse.chain).isEqualTo("regtest");

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat("/").isEqualTo(recordedRequest.getPath());

        String authHeader = recordedRequest.getHeader(JsonRpcClient.AUTHORIZATION_HEADER_NAME);
        assertThat(authHeader).isNotNull();
    }
}
