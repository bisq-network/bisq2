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

package bisq.wallets.core.rpc;

import bisq.common.encoding.Base64;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.json_rpc.JsonRpcClient;
import bisq.wallets.json_rpc.JsonRpcEndpointSpec;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RpcClientFactory {

    public static JsonRpcClient createDaemonRpcClient(RpcConfig rpcConfig) {
        return createJsonRpcClientWithUrlSuffix(rpcConfig, Optional.empty());
    }

    public static DaemonRpcClient createLegacyDaemonRpcClient(RpcConfig rpcConfig) {
        try {
            return new DaemonRpcClient(
                    createLegacyJsonRpcClientWithUrlSuffix(rpcConfig, Optional.empty())
            );
        } catch (MalformedURLException e) {
            throw new RpcClientCreationFailureException("Couldn't create RpcClient with config: " + rpcConfig, e);
        }
    }

    public static WalletRpcClient createWalletRpcClient(RpcConfig rpcConfig, String walletName) {
        try {
            var urlSuffix = "/wallet/" + walletName;
            return new WalletRpcClient(
                    createLegacyJsonRpcClientWithUrlSuffix(rpcConfig, Optional.of(urlSuffix))
            );
        } catch (MalformedURLException e) {
            throw new RpcClientCreationFailureException("Couldn't create RpcClient with config: " + rpcConfig, e);
        }
    }

    private static JsonRpcClient createJsonRpcClientWithUrlSuffix(RpcConfig rpcConfig, Optional<String> urlSuffix) {
        String url = createRpcUrlWithWithSuffix(rpcConfig, urlSuffix);
        JsonRpcEndpointSpec endpointSpec = new JsonRpcEndpointSpec(url, rpcConfig.getUser(), rpcConfig.getPassword());
        return new JsonRpcClient(endpointSpec);
    }

    private static JsonRpcHttpClient createLegacyJsonRpcClientWithUrlSuffix(RpcConfig rpcConfig, Optional<String> urlSuffix)
            throws MalformedURLException {
        String url = createRpcUrlWithWithSuffix(rpcConfig, urlSuffix);
        return new JsonRpcHttpClient(new URL(url), createAuthHeader(rpcConfig));
    }

    private static String createRpcUrlWithWithSuffix(RpcConfig rpcConfig, Optional<String> urlSuffix) {
        String hostname = rpcConfig.getHostname();
        int port = rpcConfig.getPort();
        var url = "http://" + hostname + ":" + port;
        if (urlSuffix.isPresent()) {
            url += urlSuffix.get();
        }
        return url;
    }

    private static Map<String, String> createAuthHeader(RpcConfig rpcConfig) {
        String auth = rpcConfig.getUser() + ":" + rpcConfig.getPassword();
        String base64Auth = Base64.encode(auth.getBytes(StandardCharsets.UTF_8));

        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Basic " + base64Auth);
        return map;
    }
}
