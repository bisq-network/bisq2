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

import bisq.wallets.exceptions.CannotConnectToWalletException;
import bisq.wallets.exceptions.InvalidRpcCredentialsException;
import bisq.wallets.exceptions.RpcCallFailureException;
import bisq.wallets.rpc.call.DaemonRpcCall;
import bisq.wallets.rpc.call.RpcCall;
import bisq.wallets.rpc.call.WalletRpcCall;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import java.net.ConnectException;

public class RpcClient {

    private final JsonRpcHttpClient daemonJsonRpcClient;
    private final JsonRpcHttpClient walletJsonRpcClient;

    RpcClient(JsonRpcHttpClient daemonJsonRpcClient, JsonRpcHttpClient walletJsonRpcClient) {
        this.daemonJsonRpcClient = daemonJsonRpcClient;
        this.walletJsonRpcClient = walletJsonRpcClient;
    }

    public <T, R> R invokeAndValidate(DaemonRpcCall<T, R> rpcCall) {
        R response = invoke(rpcCall);
        validateRpcCall(rpcCall, response);
        return response;
    }

    public <T, R> R invokeAndValidate(WalletRpcCall<T, R> rpcCall) {
        R response = invoke(rpcCall);
        validateRpcCall(rpcCall, response);
        return response;
    }

    private <T, R> R invoke(DaemonRpcCall<T, R> rpcCall) {
        return invokeAndHandleExceptions(daemonJsonRpcClient, rpcCall);
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

    private <T, R> R invokeAndHandleExceptions(JsonRpcHttpClient jsonRpcHttpClient, RpcCall<T, R> rpcCall) {
        String rpcCallMethodName = rpcCall.getRpcMethodName();
        try {
            return jsonRpcHttpClient.invoke(
                    rpcCallMethodName,
                    rpcCall.getRequest(),
                    rpcCall.getRpcResponseClass()
            );
        } catch (ConnectException e) {
            throw new CannotConnectToWalletException(e);
        } catch (Throwable t) {
            if (rpcAuthenticationFailed(t)) {
                throw new InvalidRpcCredentialsException("Invalid RPC credentials", t);
            }
            throw new RpcCallFailureException("RPC call to " + rpcCallMethodName + " failed.", t);
        }
    }

    private boolean rpcAuthenticationFailed(Throwable t) {
        Throwable cause = t.getCause();
        return cause != null && cause.toString().contains("401");
    }
}
