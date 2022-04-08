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
import bisq.wallets.zmq.ZmqConnection;
import bisq.wallets.zmq.ZmqListeners;
import bisq.wallets.zmq.ZmqTopicProcessors;
import bisq.wallets.elementsd.SharedElementsdInstanceTests;
import bisq.wallets.elementsd.rpc.ElementsdRawTxProcessor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ElementsdZeroMqBlockHashIntegrationIntegrationTests extends SharedElementsdInstanceTests {

    private final Set<String> minedBlockHashes = new CopyOnWriteArraySet<>();

    private final AtomicBoolean didListenerReceiveBlockHash = new AtomicBoolean();
    private final CountDownLatch listenerReceivedBlockHashLatch = new CountDownLatch(1);

    @Test
    void blockHashNotification() throws InterruptedException {
        var zmqListeners = new ZmqListeners();
        var rawTxProcessor = new ElementsdRawTxProcessor(elementsdDaemon, elementsdMinerWallet, zmqListeners);

        var zmqTopicProcessors = new ZmqTopicProcessors(rawTxProcessor, zmqListeners);
        var zmqConnection = new ZmqConnection(zmqTopicProcessors, zmqListeners);

        List<BitcoindGetZmqNotificationsResponse> zmqNotifications = elementsdDaemon.getZmqNotifications();
        zmqConnection.initialize(zmqNotifications);

        zmqConnection.getListeners().registerNewBlockMinedListener((blockHash) -> {
            log.info("Notification: New block with hash " + blockHash);
            if (minedBlockHashes.contains(blockHash)) {
                didListenerReceiveBlockHash.set(true);
                listenerReceivedBlockHashLatch.countDown();
            }
        });

        createAndStartDaemonThread(() -> {
            while (!didListenerReceiveBlockHash.get()) {
                List<String> blockHashes = elementsdRegtestSetup.mineOneBlock();
                minedBlockHashes.addAll(blockHashes);
                log.info("Mined Block: " + blockHashes);
            }
        });

        boolean await = listenerReceivedBlockHashLatch.await(1, TimeUnit.MINUTES);
        if (!await) {
            throw new IllegalStateException("Didn't connect to bitcoind after 1 minute.");
        }

        zmqConnection.close();
    }

    private void createAndStartDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }
}
