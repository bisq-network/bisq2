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

package bisq.support.mediation.mu_sig;

import bisq.contract.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class MuSigMediationCaseTest {
    @Test
    void addIssues_keepsAllUniqueIssues_whenCalledConcurrently() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            for (int round = 0; round < 1000; round++) {
                MuSigMediationCase mediationCase = new MuSigMediationCase(null);
                CountDownLatch ready = new CountDownLatch(4);
                CountDownLatch start = new CountDownLatch(1);

                Future<?> makerMaker = executor.submit(() -> addIssueAfterStart(ready, start, mediationCase,
                        new MuSigMediationIssue(Role.MAKER, MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH)));
                Future<?> takerMaker = executor.submit(() -> addIssueAfterStart(ready, start, mediationCase,
                        new MuSigMediationIssue(Role.TAKER, MuSigMediationIssueType.MAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH)));
                Future<?> makerTaker = executor.submit(() -> addIssueAfterStart(ready, start, mediationCase,
                        new MuSigMediationIssue(Role.MAKER, MuSigMediationIssueType.TAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH)));
                Future<?> takerTaker = executor.submit(() -> addIssueAfterStart(ready, start, mediationCase,
                        new MuSigMediationIssue(Role.TAKER, MuSigMediationIssueType.TAKER_ACCOUNT_PAYLOAD_HASH_MISMATCH)));

                ready.await();
                start.countDown();

                makerMaker.get();
                takerMaker.get();
                makerTaker.get();
                takerTaker.get();

                assertThat(mediationCase.getIssues().get())
                        .as("round %s", round)
                        .hasSize(4);
            }
        }
    }

    private void addIssueAfterStart(CountDownLatch ready,
                                    CountDownLatch start,
                                    MuSigMediationCase mediationCase,
                                    MuSigMediationIssue issue) {
        ready.countDown();
        await(start);
        Thread.yield();
        mediationCase.addIssues(List.of(issue));
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
