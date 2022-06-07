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

package bisq.security.pow;

import bisq.common.threading.ExecutorFactory;
import bisq.common.util.ByteArrayUtils;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;

// Taken from: https://github.com/bisq-network/bisq
@Slf4j
public abstract class ProofOfWorkService implements PersistenceClient<ProofOfWorkStore> {
    public final static int MIN_DIFFICULTY = 16384;

    private long targetDuration = 100;
    @Getter
    private final ProofOfWorkStore persistableStore = new ProofOfWorkStore();
    @Getter
    private final Persistence<ProofOfWorkStore> persistence;

    public ProofOfWorkService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    public CompletableFuture<Boolean> initialize() {
       /* if (persistableStore.getNymDifficulty() < 0) {
            persistableStore.setNymDifficulty(65536);
        }*/
        return CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Double> evaluateNymDifficulty(AtomicBoolean isCanceled) {
        if (persistableStore.getNymDifficulty() >= 0) {
            return CompletableFuture.completedFuture(persistableStore.getNymDifficulty());
        }

        return CompletableFuture.supplyAsync(() -> {
            int exponent = 14;
            double candidateDifficulty = Math.pow(2, exponent);
            int numIterations = 5;
            AtomicLong durations = new AtomicLong(0);
            long average = 0;
            // Warm up with min difficulty. first few calls take much longer and would distort average calculation
            for (int i = 0; i < 5 && !isCanceled.get(); i++) {
                byte[] hash = DigestUtil.hash(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                mintNymProofOfWork(hash, 0).join();
            }

            while (average < targetDuration && !isCanceled.get()) {
                for (int i = 0; i < numIterations && !isCanceled.get(); i++) {
                    long ts = System.currentTimeMillis();
                    byte[] hash = DigestUtil.hash(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                    mintNymProofOfWork(hash, candidateDifficulty).join();
                    long duration = System.currentTimeMillis() - ts;
                    durations.getAndAdd(duration);
                }

                average = (long) (durations.get() / (double) numIterations);
                log.debug("Average duration for {} iterations with difficulty `2 pow {}` ({}): {} ms. targetDuration: {}",
                        numIterations, exponent, candidateDifficulty, average, targetDuration);

                if (average < targetDuration) {
                    durations.set(0);
                    exponent++;
                    candidateDifficulty = Math.pow(2, exponent);
                }
            }
            if (candidateDifficulty < MIN_DIFFICULTY) {
                candidateDifficulty = MIN_DIFFICULTY;
            }
            checkArgument(!isCanceled.get(), "Task got cancelled");
            log.info("We found a difficulty at average duration {} ms. difficulty `2 pow {}` ({})",
                    average, exponent, candidateDifficulty);
            persistableStore.setNymDifficulty(candidateDifficulty);
            persist();
            return candidateDifficulty;
        }, ExecutorFactory.newSingleThreadExecutor("evaluateNymDifficultyThread"));
    }

    public abstract CompletableFuture<ProofOfWork> mint(byte[] payload, byte[] challenge, double difficulty);

    public abstract boolean verify(ProofOfWork proofOfWork);

    public byte[] asUtf8Bytes(String itemId) {
        return itemId.getBytes(StandardCharsets.UTF_8);
    }

    public abstract byte[] getChallenge(String itemId, String ownerId);

    public CompletableFuture<ProofOfWork> mint(String itemId, String ownerId, double difficulty) {
        return mint(asUtf8Bytes(itemId), getChallenge(itemId, ownerId), difficulty);
    }

    public CompletableFuture<ProofOfWork> mintNymProofOfWork(byte[] pubKeyHash, AtomicBoolean isCanceled) {
        return evaluateNymDifficulty(isCanceled)
                .thenCompose(nymDifficulty -> mintNymProofOfWork(pubKeyHash, nymDifficulty));
    }

    public CompletableFuture<ProofOfWork> mintNymProofOfWork(byte[] pubKeyHash, double nymDifficulty) {
        return mint(pubKeyHash, ByteArrayUtils.copyOf(pubKeyHash), nymDifficulty);
    }

    public boolean verify(ProofOfWork proofOfWork,
                          String itemId,
                          String ownerId,
                          double controlDifficulty) {
        byte[] controlChallenge = getChallenge(itemId, ownerId);
        return Arrays.equals(proofOfWork.getChallenge(), controlChallenge) &&
                proofOfWork.getDifficulty() >= controlDifficulty &&
                verify(proofOfWork);
    }
}
