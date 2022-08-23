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

package bisq.wallets.regtest.bitcoind;

import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.bitcoind.zmq.listeners.NewBlockMinedListener;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.process.BisqProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BitcoindRegtestBlockMiner implements BisqProcess {
    private final BitcoindDaemon daemon;
    private final BitcoindWallet minerWallet;
    private final ZmqListeners zmqListeners;

    private final Set<String> minedBlockHashes = ConcurrentHashMap.newKeySet();
    private final NewBlockMinedListener newBlockMinedListener = minedBlockHashes::add;

    public BitcoindRegtestBlockMiner(BitcoindDaemon daemon, BitcoindWallet minerWallet, ZmqListeners zmqListeners) {
        this.daemon = daemon;
        this.minerWallet = minerWallet;
        this.zmqListeners = zmqListeners;
    }

    @Override
    public void start() throws IOException, InterruptedException {
        zmqListeners.registerNewBlockMinedListener(newBlockMinedListener);
    }

    @Override
    public void shutdown() {
        zmqListeners.unregisterNewBlockMinedListener(newBlockMinedListener);
    }

    public void mineInitialRegtestBlocks() throws InterruptedException {
        mineBlocks(101);
    }

    public List<String> mineBlocks(int numberOfBlocks) throws InterruptedException {
        String minerAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        List<String> blockHashes = daemon.generateToAddress(numberOfBlocks, minerAddress);

        CountDownLatch blocksMinedLatch = waitUntilBlocksMined(blockHashes);
        boolean allBlocksMined = blocksMinedLatch.await(15, TimeUnit.SECONDS);
        if (!allBlocksMined) {
            throw new IllegalStateException("Couldn't mine " + numberOfBlocks + " blocks");
        }
        return blockHashes;
    }

    public CountDownLatch waitUntilBlocksMined(List<String> frozenBlockHashList) {
        List<String> blockHashes = new ArrayList<>(frozenBlockHashList);
        CountDownLatch blocksMinedLatch = new CountDownLatch(1);
        new Thread(() -> {
            Set<String> foundHashes = new HashSet<>();

            while (!blockHashes.isEmpty()) {
                blockHashes.stream()
                        .filter(minedBlockHashes::contains)
                        .forEach(hash -> {
                            foundHashes.add(hash);
                            minedBlockHashes.remove(hash);
                        });

                foundHashes.forEach(blockHashes::remove);
                foundHashes.clear();
            }

            blocksMinedLatch.countDown();
        }).start();
        return blocksMinedLatch;
    }
}
