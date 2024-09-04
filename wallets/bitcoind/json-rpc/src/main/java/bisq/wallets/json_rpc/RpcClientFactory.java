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

import java.util.Optional;

public class RpcClientFactory {

    public static JsonRpcClient createDaemonRpcClient(RpcConfig rpcConfig) {
        return createJsonRpcClientWithUrlSuffix(rpcConfig, Optional.empty());
    }

    public static JsonRpcClient createWalletRpcClient(RpcConfig rpcConfig, String walletName) {
        var urlSuffix = "/wallet/" + walletName;
        return createJsonRpcClientWithUrlSuffix(rpcConfig, Optional.of(urlSuffix));
    }

    private static JsonRpcClient createJsonRpcClientWithUrlSuffix(RpcConfig rpcConfig, Optional<String> urlSuffix) {
        String url = createRpcUrlWithWithSuffix(rpcConfig, urlSuffix);
        JsonRpcEndpointSpec endpointSpec = new JsonRpcEndpointSpec(url, rpcConfig.getUser(), rpcConfig.getPassword());
        return new JsonRpcClient(endpointSpec);
    }

    private static String createRpcUrlWithWithSuffix(RpcConfig rpcConfig, Optional<String> urlSuffix) {
        String hostname = rpcConfig.getHostname();
        int port = rpcConfig.getPort();
        @SuppressWarnings("HttpUrlsUsage")
        var url = "http://" + hostname + ":" + port;
        if (urlSuffix.isPresent()) {
            url += urlSuffix.get();
        }
        return url;
    }
}
