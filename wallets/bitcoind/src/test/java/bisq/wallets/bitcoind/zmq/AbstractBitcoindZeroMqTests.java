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

package bisq.wallets.bitcoind.zmq;

import bisq.wallets.bitcoind.SharedBitcoindInstanceTests;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AbstractBitcoindZeroMqTests extends SharedBitcoindInstanceTests {
    protected ZmqConnection bitcoindZeroMq;

    @BeforeAll
    @Override
    public void start() throws IOException, InterruptedException {
        super.start();
        regtestSetup.mineInitialRegtestBlocks();

        CountDownLatch waitToMineBlockLatch = new CountDownLatch(1);
        AtomicBoolean isConnectedToDaemon = new AtomicBoolean();

        bitcoindZeroMq = createAndInitializeZmqConnection();
        bitcoindZeroMq.getListeners().registerNewBlockMinedListener((blockHash) -> isConnectedToDaemon.set(true));

        waitToMineBlockLatch.countDown();
        waitUntilConnected(waitToMineBlockLatch, isConnectedToDaemon);
    }

    @AfterAll
    public void shutdown() {
        bitcoindZeroMq.shutdown();
    }

    void waitUntilConnected(CountDownLatch waitToMineBlockLatch,
                            AtomicBoolean isConnectedToDaemon) throws InterruptedException {
        createAndStartThread(() -> {
            try {
                log.info("Waiting until other thread is connected with bitcoind.");
                boolean await = waitToMineBlockLatch.await(1, TimeUnit.MINUTES);
                if (!await) {
                    throw new IllegalStateException("Didn't connect to bitcoind after 1 minute.");
                }

                log.info("The other thread connected to bitcoind! Let's start!");
                while (!isConnectedToDaemon.get()) {
                    regtestSetup.mineOneBlock();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).join();
    }

    private ZmqConnection createAndInitializeZmqConnection() {
        var zmqListeners = new ZmqListeners();
        var bitcoindRawTxProcessor = new BitcoindRawTxProcessor(daemon, zmqListeners);

        var bitcoindZmqTopicProcessors = new ZmqTopicProcessors(bitcoindRawTxProcessor, zmqListeners);
        ZmqConnection zmqConnection = new ZmqConnection(bitcoindZmqTopicProcessors, zmqListeners);

        List<BitcoindGetZmqNotificationsResponse> zmqNotifications = daemon.getZmqNotifications();
        zmqConnection.initialize(zmqNotifications);
        return zmqConnection;
    }

    private Thread createAndStartThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }
}
