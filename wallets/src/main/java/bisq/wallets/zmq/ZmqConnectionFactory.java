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

package bisq.wallets.zmq;

import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.zmq.BitcoindRawTxProcessor;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdRawTxProcessor;
import bisq.wallets.elementsd.rpc.ElementsdWallet;

public class ZmqConnectionFactory {

    public static ZmqConnection createForBitcoind(BitcoindDaemon daemon) {
        var zmqListeners = new ZmqListeners();
        ZmqRawTxProcessor bitcoindRawTxProcessor = createBitcoindRawTxProcessor(daemon, zmqListeners);
        return createZmqConnection(bitcoindRawTxProcessor, zmqListeners);
    }

    public static ZmqConnection createForElements(ElementsdDaemon daemon, ElementsdWallet wallet) {
        var zmqListeners = new ZmqListeners();
        ZmqRawTxProcessor rawTxProcessor = createElementsdRawTxProcessor(daemon, wallet, zmqListeners);
        return createZmqConnection(rawTxProcessor, zmqListeners);
    }

    private static ZmqRawTxProcessor createBitcoindRawTxProcessor(BitcoindDaemon daemon, ZmqListeners listeners) {
        return new BitcoindRawTxProcessor(daemon, listeners);
    }

    private static ZmqRawTxProcessor createElementsdRawTxProcessor(ElementsdDaemon daemon,
                                                                   ElementsdWallet wallet,
                                                                   ZmqListeners listeners) {
        return new ElementsdRawTxProcessor(daemon, wallet, listeners);
    }

    private static ZmqConnection createZmqConnection(ZmqRawTxProcessor rawTxProcessor, ZmqListeners listeners) {
        var zmqTopicProcessors = new ZmqTopicProcessors(rawTxProcessor, listeners);
        return new ZmqConnection(zmqTopicProcessors, listeners);
    }
}
