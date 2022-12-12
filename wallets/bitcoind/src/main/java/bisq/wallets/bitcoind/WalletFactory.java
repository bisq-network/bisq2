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

package bisq.wallets.bitcoind;

import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqConnectionFactory;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.json_rpc.JsonRpcClient;

import java.util.List;

public class WalletFactory {

    public static BitcoinWallet createBitcoinWallet(RpcConfig rpcConfig,
                                                    String walletName,
                                                    BitcoinWalletStore bitcoinWalletStore) {
        BitcoindDaemon daemon = createBitcoindDaemon(rpcConfig);
        ZmqConnection zmqConnection = initializeBitcoindZeroMq(daemon);
        return new BitcoinWallet(walletName, rpcConfig, daemon, bitcoinWalletStore.getReceiveAddresses(), zmqConnection);
    }

    private static BitcoindDaemon createBitcoindDaemon(RpcConfig rpcConfig) {
        JsonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        return new BitcoindDaemon(rpcClient);
    }

    private static ZmqConnection initializeBitcoindZeroMq(BitcoindDaemon bitcoindDaemon) {
        var bitcoindZeroMq = ZmqConnectionFactory.createForBitcoind(bitcoindDaemon);
        List<BitcoindGetZmqNotificationsResponse.Entry> zmqNotifications = bitcoindDaemon.getZmqNotifications();
        bitcoindZeroMq.initialize(zmqNotifications);
        return bitcoindZeroMq;
    }
}
