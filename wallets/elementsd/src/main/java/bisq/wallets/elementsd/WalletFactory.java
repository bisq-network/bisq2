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

package bisq.wallets.elementsd;

import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.core.rpc.WalletRpcClient;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdWallet;

import java.nio.file.Path;
import java.util.List;

public class WalletFactory {

    public static LiquidWallet createLiquidWallet(RpcConfig rpcConfig,
                                                  Path walletsDataDir,
                                                  LiquidWalletStore liquidWalletStore) {
        Path walletDir = walletsDataDir.resolve("elementsd"); // directory name for bitcoind wallet

        ElementsdDaemon elementsdDaemon = createElementsdDaemon(rpcConfig);
        ElementsdWallet elementsdWallet = createElementsdWallet(rpcConfig, walletDir);
        ZmqConnection zmqConnection = initializeElementsdZeroMq(elementsdDaemon, elementsdWallet);

        return new LiquidWallet(walletDir, elementsdDaemon, elementsdWallet, liquidWalletStore, zmqConnection);
    }

    private static ElementsdDaemon createElementsdDaemon(RpcConfig rpcConfig) {
        DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        return new ElementsdDaemon(rpcClient);
    }

    private static ElementsdWallet createElementsdWallet(RpcConfig rpcConfig, Path walletPath) {
        WalletRpcClient rpcClient = RpcClientFactory.createWalletRpcClient(rpcConfig, walletPath);
        return new ElementsdWallet(rpcClient);
    }

    private static ZmqConnection initializeElementsdZeroMq(ElementsdDaemon elementsdDaemon, ElementsdWallet elementsdWallet) {
        var zmqConnection = ZmqConnectionFactory.createForElements(elementsdDaemon, elementsdWallet);
        List<BitcoindGetZmqNotificationsResponse> zmqNotifications = elementsdDaemon.getZmqNotifications();
        zmqConnection.initialize(zmqNotifications);
        return zmqConnection;
    }
}
