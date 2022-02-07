package bisq.wallets.bitcoind.rpc;

import bisq.common.encoding.Base64;
import bisq.wallets.bitcoind.BitcoindRpcEndpoint;
import bisq.wallets.exceptions.RpcCallFailureException;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

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
        } catch (Throwable t) {
            throw new RpcCallFailureException("RPC call to " + rpcEndpoint.getMethodName() + " failed.", t);
        }
    }

    private JsonRpcHttpClient createRpcClient(WalletRpcConfig walletRpcConfig) throws MalformedURLException {
        var urlSuffix = "/wallet/" + walletRpcConfig.walletPath().toString();
        return createRpcClientWithUrlSuffix(walletRpcConfig.rpcConfig(), Optional.of(urlSuffix));
    }

    private JsonRpcHttpClient createRpcClientWithUrlSuffix(RpcConfig rpcConfig, Optional<String> urlSuffix) throws MalformedURLException {
        String hostname = rpcConfig.hostname();
        int port = rpcConfig.networkType().getRpcPort();
        var url = "http://" + hostname + ":" + port;
        if (urlSuffix.isPresent()) {
            url += urlSuffix.get();
        }
        return new JsonRpcHttpClient(new URL(url), createAuthHeader(rpcConfig));
    }

    private Map<String, String> createAuthHeader(RpcConfig rpcConfig) {
        String auth = rpcConfig.user() + ":" + rpcConfig.password();
        String base64Auth = Base64.encode(auth.getBytes(StandardCharsets.UTF_8));

        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Basic " + base64Auth);
        return map;
    }
}
