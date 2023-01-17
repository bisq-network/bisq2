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

package bisq.wallets.elementsd.zmq;

import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.bitcoind.zmq.ZmqTopicProcessors;
import bisq.wallets.elementsd.SharedElementsdInstanceTests;
import bisq.wallets.elementsd.rpc.ElementsdRawTxProcessor;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AbstractElementsdZeroMqTests extends SharedElementsdInstanceTests {

    private static final int ONE_SECOND_IN_MILLIS = 1000;
    protected static final int TWO_MINUTES_IN_MILLIS = ONE_SECOND_IN_MILLIS * 60 * 2;

    protected ZmqConnection minerWalletZmqConnection;

    protected ElementsdWallet receiverWallet;
    protected ZmqConnection receiverWalletZmqConnection;

    @BeforeAll
    @Override
    public void start() throws IOException, InterruptedException {
        super.start();
        peginBtc(30);

        CountDownLatch waitToMineBlockLatch = new CountDownLatch(1);
        CountDownLatch waitUntilAllZmqReady = new CountDownLatch(2);

        minerWalletZmqConnection = createAndInitializeZmqConnection(elementsdMinerWallet);
        minerWalletZmqConnection.getListeners()
                .registerNewBlockMinedListener((blockHash) -> waitUntilAllZmqReady.countDown());

        receiverWallet = elementsdRegtestSetup.createNewWallet("receiver_wallet");
        receiverWalletZmqConnection = createAndInitializeZmqConnection(receiverWallet);
        minerWalletZmqConnection.getListeners()
                .registerNewBlockMinedListener((blockHash) -> waitUntilAllZmqReady.countDown());

        waitToMineBlockLatch.countDown();
        waitUntilConnected(waitToMineBlockLatch, waitUntilAllZmqReady);
    }

    @AfterAll
    public void shutdown() {
        minerWalletZmqConnection.shutdown();
    }

    ZmqConnection createAndInitializeZmqConnection(ElementsdWallet elementsdWallet) {
        var zmqListeners = new ZmqListeners();
        var rawTxProcessor = new ElementsdRawTxProcessor(elementsdDaemon, elementsdWallet, zmqListeners);

        var zmqTopicProcessors = new ZmqTopicProcessors(rawTxProcessor, zmqListeners);
        var zmqConnection = new ZmqConnection(zmqTopicProcessors, zmqListeners);

        List<BitcoindGetZmqNotificationsResponse.Entry> zmqNotifications = elementsdDaemon.getZmqNotifications();
        zmqConnection.initialize(zmqNotifications);

        return zmqConnection;
    }

    void waitUntilConnected(CountDownLatch waitToMineBlockLatch,
                            CountDownLatch waitUntilAllZmqReady) throws InterruptedException {
        createAndStartThread(() -> {
            try {
                log.info("Waiting until other thread is connected with bitcoind.");
                boolean await = waitToMineBlockLatch.await(1, TimeUnit.MINUTES);
                if (!await) {
                    throw new IllegalStateException("Didn't connect to bitcoind after 1 minute.");
                }

                log.info("The other thread connected to bitcoind! Let's start!");

                while (waitUntilAllZmqReady.getCount() != 0) {
                    elementsdRegtestSetup.mineOneBlock();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).join(TWO_MINUTES_IN_MILLIS);
    }

    private Thread createAndStartThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }
}
