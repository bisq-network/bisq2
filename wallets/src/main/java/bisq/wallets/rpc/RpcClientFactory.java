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

package bisq.wallets.rpc;

import bisq.common.encoding.Base64;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RpcClientFactory {

    public static RpcClient create(RpcConfig rpcConfig) throws MalformedURLException {
        return new RpcClient(
                createDaemonRpcClient(rpcConfig),
                createWalletRpcClient(rpcConfig)
        );
    }

    private static JsonRpcHttpClient createDaemonRpcClient(RpcConfig rpcConfig) throws MalformedURLException {
        return createRpcClientWithUrlSuffix(rpcConfig, Optional.empty());
    }

    private static JsonRpcHttpClient createWalletRpcClient(RpcConfig rpcConfig) throws MalformedURLException {
        var urlSuffix = "/wallet/" + rpcConfig.walletPath().toString();
        return createRpcClientWithUrlSuffix(rpcConfig, Optional.of(urlSuffix));
    }

    private static JsonRpcHttpClient createRpcClientWithUrlSuffix(RpcConfig rpcConfig, Optional<String> urlSuffix)
            throws MalformedURLException {
        String hostname = rpcConfig.hostname();
        int port = rpcConfig.port();
        var url = "http://" + hostname + ":" + port;
        if (urlSuffix.isPresent()) {
            url += urlSuffix.get();
        }
        return new JsonRpcHttpClient(new URL(url), createAuthHeader(rpcConfig));
    }

    private static Map<String, String> createAuthHeader(RpcConfig rpcConfig) {
        String auth = rpcConfig.user() + ":" + rpcConfig.password();
        String base64Auth = Base64.encode(auth.getBytes(StandardCharsets.UTF_8));

        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Basic " + base64Auth);
        return map;
    }
}
