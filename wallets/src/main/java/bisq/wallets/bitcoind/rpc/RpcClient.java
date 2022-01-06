package bisq.wallets.bitcoind.rpc;

import bisq.common.encoding.Base64;
import bisq.wallets.bitcoind.BitcoindRpcEndpoint;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RpcClient {

    private final JsonRpcHttpClient jsonRpcClient;

    public RpcClient(RpcConfig config) throws MalformedURLException {
        jsonRpcClient = createRpcClient(config);
    }

    public <T> T invoke(BitcoindRpcEndpoint rpcEndpoint, Object argument, Class<T> clazz) throws RpcCallFailureException {
        try {
            return jsonRpcClient.invoke(rpcEndpoint.getMethodName(), argument, clazz);
        } catch (Throwable t) {
            throw new RpcCallFailureException("RPC call to " + rpcEndpoint.getMethodName() + " failed.", t);
        }
    }

    private JsonRpcHttpClient createRpcClient(RpcConfig rpcConfig) throws MalformedURLException {
        String hostname = rpcConfig.hostname();
        int port = rpcConfig.networkType().getRpcPort();
        var url = "http://" + hostname + ":" + port;
        if (rpcConfig.walletName().isPresent()) {
            url += "/wallet/" + rpcConfig.walletName().get();
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
