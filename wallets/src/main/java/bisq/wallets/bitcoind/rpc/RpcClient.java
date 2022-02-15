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

package bisq.wallets.bitcoind.rpc;

import bisq.common.encoding.Base64;
import bisq.wallets.bitcoind.BitcoindRpcEndpoint;
import bisq.wallets.exceptions.CannotConnectToWalletException;
import bisq.wallets.exceptions.InvalidRpcCredentialsException;
import bisq.wallets.exceptions.RpcCallFailureException;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RpcClient {

    private final JsonRpcHttpClient jsonRpcClient;

    public RpcClient(WalletRpcConfig config) throws MalformedURLException {
        jsonRpcClient = createRpcClient(config);
    }

    public RpcClient(RpcConfig config) throws MalformedURLException {
        jsonRpcClient = createRpcClientWithUrlSuffix(config, Optional.empty());
    }

    public <T> T invoke(BitcoindRpcEndpoint rpcEndpoint, Object argument, Class<T> clazz) {
        try {
            return jsonRpcClient.invoke(rpcEndpoint.getMethodName(), argument, clazz);
        } catch (ConnectException e) {
            throw new CannotConnectToWalletException(e);
        }
        catch (Throwable t) {
            if (rpcAuthenticationFailed(t)) {
                throw new InvalidRpcCredentialsException("Invalid RPC credentials", t);
            }
            throw new RpcCallFailureException("RPC call to " + rpcEndpoint.getMethodName() + " failed.", t);
        }
    }

    private JsonRpcHttpClient createRpcClient(WalletRpcConfig walletRpcConfig) throws MalformedURLException {
        var urlSuffix = "/wallet/" + walletRpcConfig.walletPath().toString();
        return createRpcClientWithUrlSuffix(walletRpcConfig.rpcConfig(), Optional.of(urlSuffix));
    }

    private JsonRpcHttpClient createRpcClientWithUrlSuffix(RpcConfig rpcConfig, Optional<String> urlSuffix) throws MalformedURLException {
        String hostname = rpcConfig.hostname();
        int port = rpcConfig.port();
        var url = "http://" + hostname + ":" + port;
        if (urlSuffix.isPresent()) {
            url += urlSuffix.get();
        }
        return new JsonRpcHttpClient(new URL(url), createAuthHeader(rpcConfig));
    }

    private boolean rpcAuthenticationFailed(Throwable t) {
        return t.getCause().toString().contains("401");
    }

    private Map<String, String> createAuthHeader(RpcConfig rpcConfig) {
        String auth = rpcConfig.user() + ":" + rpcConfig.password();
        String base64Auth = Base64.encode(auth.getBytes(StandardCharsets.UTF_8));

        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Basic " + base64Auth);
        return map;
    }
}
