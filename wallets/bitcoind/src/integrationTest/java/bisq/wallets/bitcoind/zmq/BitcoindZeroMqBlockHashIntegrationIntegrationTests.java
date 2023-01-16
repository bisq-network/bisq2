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

import bisq.wallets.bitcoind.regtest.BitcoindExtension;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ExtendWith(BitcoindExtension.class)
@Slf4j
public class BitcoindZeroMqBlockHashIntegrationIntegrationTests {

    private final BitcoindRegtestSetup regtestSetup;
    private final Set<String> minedBlockHashes = new CopyOnWriteArraySet<>();
    private final CountDownLatch listenerReceivedBlockHashLatch = new CountDownLatch(1);

    public BitcoindZeroMqBlockHashIntegrationIntegrationTests(BitcoindRegtestSetup regtestSetup) {
        this.regtestSetup = regtestSetup;
    }

    @Test
    void blockHashNotification() throws InterruptedException {
        ZmqListeners zmqListeners = regtestSetup.getZmqListeners();
        zmqListeners.registerNewBlockMinedListener((blockHash) -> {
            log.info("Notification: New block with hash " + blockHash);

            if (minedBlockHashes.contains(blockHash)) {
                listenerReceivedBlockHashLatch.countDown();
            } else {
                minedBlockHashes.add(blockHash);
            }
        });

        createAndStartDaemonThread(() -> {
            while (true) {
                try {
                    List<String> blockHashes = regtestSetup.mineOneBlock();
                    log.info("Mined block: " + blockHashes);

                    for (String blockHash : blockHashes) {
                        if (minedBlockHashes.contains(blockHash)) {
                            listenerReceivedBlockHashLatch.countDown();
                            return;
                        } else {
                            minedBlockHashes.add(blockHash);
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        boolean await = listenerReceivedBlockHashLatch.await(1, TimeUnit.MINUTES);
        if (!await) {
            throw new IllegalStateException("Didn't connect to bitcoind after 1 minute.");
        }
    }

    private void createAndStartDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }
}
