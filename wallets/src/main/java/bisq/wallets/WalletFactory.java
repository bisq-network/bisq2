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

package bisq.wallets;

import bisq.wallets.bitcoind.BitcoinWallet;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.elementsd.LiquidWallet;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.exceptions.WalletInitializationFailedException;
import bisq.wallets.rpc.DaemonRpcClient;
import bisq.wallets.rpc.RpcClientFactory;
import bisq.wallets.rpc.WalletRpcClient;
import bisq.wallets.stores.BitcoinWalletStore;
import bisq.wallets.stores.LiquidWalletStore;
import bisq.wallets.zmq.ZmqConnection;
import bisq.wallets.zmq.ZmqConnectionFactory;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

public class WalletFactory {

    public static BitcoinWallet createBitcoinWallet(RpcConfig rpcConfig,
                                                    Path walletsDataDir,
                                                    BitcoinWalletStore bitcoinWalletStore) {
        Path bitcoindDataDir = walletsDataDir.resolve("bitcoind"); // directory name for bitcoind wallet

        BitcoindDaemon daemon = createBitcoindDaemon(rpcConfig);
        ZmqConnection zmqConnection = initializeBitcoindZeroMq(daemon);
        return new BitcoinWallet(bitcoindDataDir, rpcConfig, daemon, bitcoinWalletStore.getReceiveAddresses(), zmqConnection);
    }

    public static LiquidWallet createLiquidWallet(RpcConfig rpcConfig,
                                                  Path walletsDataDir,
                                                  LiquidWalletStore liquidWalletStore) {
        Path walletDir = walletsDataDir.resolve("elementsd"); // directory name for bitcoind wallet

        ElementsdDaemon elementsdDaemon = createElementsdDaemon(rpcConfig);
        ElementsdWallet elementsdWallet = createElementsdWallet(rpcConfig, walletDir);
        ZmqConnection zmqConnection = initializeElementsdZeroMq(elementsdDaemon, elementsdWallet);

        return new LiquidWallet(walletDir, elementsdDaemon, elementsdWallet, liquidWalletStore, zmqConnection);
    }

    private static BitcoindDaemon createBitcoindDaemon(RpcConfig rpcConfig) {
        try {
            DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
            return new BitcoindDaemon(rpcClient);
        } catch (MalformedURLException e) {
            throw new WalletInitializationFailedException("Couldn't initialize BitcoinWalletService", e);
        }
    }

    private static ElementsdDaemon createElementsdDaemon(RpcConfig rpcConfig) {
        try {
            DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
            return new ElementsdDaemon(rpcClient);
        } catch (MalformedURLException e) {
            throw new WalletInitializationFailedException("Couldn't initialize BitcoinWalletService", e);
        }
    }

    private static ElementsdWallet createElementsdWallet(RpcConfig rpcConfig, Path walletPath) {
        try {
            WalletRpcClient rpcClient = RpcClientFactory.createWalletRpcClient(rpcConfig, walletPath);
            return new ElementsdWallet(rpcClient);
        } catch (MalformedURLException e) {
            throw new WalletInitializationFailedException("Couldn't initialize BitcoinWalletService", e);
        }
    }

    private static ZmqConnection initializeBitcoindZeroMq(BitcoindDaemon bitcoindDaemon) {
        var bitcoindZeroMq = ZmqConnectionFactory.createForBitcoind(bitcoindDaemon);
        List<BitcoindGetZmqNotificationsResponse> zmqNotifications = bitcoindDaemon.getZmqNotifications();
        bitcoindZeroMq.initialize(zmqNotifications);
        return bitcoindZeroMq;
    }

    private static ZmqConnection initializeElementsdZeroMq(ElementsdDaemon elementsdDaemon, ElementsdWallet elementsdWallet) {
        var zmqConnection = ZmqConnectionFactory.createForElements(elementsdDaemon, elementsdWallet);
        List<BitcoindGetZmqNotificationsResponse> zmqNotifications = elementsdDaemon.getZmqNotifications();
        zmqConnection.initialize(zmqNotifications);
        return zmqConnection;
    }
}
