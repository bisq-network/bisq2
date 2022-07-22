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

import bisq.wallets.core.exceptions.RpcCallFailureException;
import bisq.wallets.core.rpc.call.RpcCall;
import bisq.wallets.core.rpc.call.WalletRpcCall;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import java.nio.file.Path;

public class WalletRpcClient extends AbstractRpcClient {

    private final JsonRpcHttpClient walletJsonRpcClient;

    WalletRpcClient(JsonRpcHttpClient walletJsonRpcClient) {
        this.walletJsonRpcClient = walletJsonRpcClient;
    }

    public <T, R> R invokeAndValidate(WalletRpcCall<T, R> rpcCall) {
        R response = invoke(rpcCall);
        validateRpcCall(rpcCall, response);
        return response;
    }

    private <T, R> R invoke(WalletRpcCall<T, R> rpcCall) {
        return invokeAndHandleExceptions(walletJsonRpcClient, rpcCall);
    }

    private <T, R> void validateRpcCall(RpcCall<T, R> rpcCall, R response) {
        boolean isValid = rpcCall.isResponseValid(response);
        if (!isValid) {
            throw new RpcCallFailureException("RPC Call to '" + rpcCall.getRpcMethodName() + "' failed. " +
                    response.toString());
        }
    }

    public Path getWalletPath() {
        // URL looks like: http://127.0.0.1:45775/wallet//tmp/2035361932108224852/miner_wallet
        String filePart = walletJsonRpcClient.getServiceUrl().getFile();
        int startIndex = "/wallet/".length();
        String walletPath = filePart.substring(startIndex);
        return Path.of(walletPath);
    }

}
