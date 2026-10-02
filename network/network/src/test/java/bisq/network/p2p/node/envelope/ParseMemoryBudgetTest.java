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

package bisq.network.p2p.node.envelope;

import bisq.network.p2p.message.NetworkEnvelope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// See rule 11 of docs/specifications/network/message-framing.md
class ParseMemoryBudgetTest {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ParseMemoryBudget budget = new ParseMemoryBudget(NetworkEnvelope.MAX_SERIALIZED_SIZE);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void largeMessageWaitsUntilTheBudgetHasRoom() throws Exception {
        CountDownLatch firstParseStarted = new CountDownLatch(1);
        CountDownLatch finishFirstParse = new CountDownLatch(1);
        CompletableFuture<Integer> firstParse = parseAsync(new byte[NetworkEnvelope.MAX_SERIALIZED_SIZE], message -> {
            firstParseStarted.countDown();
            finishFirstParse.await();
            return message.length;
        });
        assertThat(firstParseStarted.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Integer> secondParse = parseAsync(new byte[ParseMemoryBudget.MIN_ACCOUNTED_SIZE],
                message -> message.length);

        Thread.sleep(200);
        assertThat(secondParse).isNotDone();
        finishFirstParse.countDown();
        assertThat(secondParse.get(10, TimeUnit.SECONDS)).isEqualTo(ParseMemoryBudget.MIN_ACCOUNTED_SIZE);
        assertThat(firstParse.get(10, TimeUnit.SECONDS)).isEqualTo(NetworkEnvelope.MAX_SERIALIZED_SIZE);
        assertThat(budget.getAvailableBytes()).isEqualTo(budget.getBudget());
    }

    @Test
    void smallMessageDoesNotWait() throws Exception {
        CountDownLatch firstParseStarted = new CountDownLatch(1);
        CountDownLatch finishFirstParse = new CountDownLatch(1);
        CompletableFuture<Integer> firstParse = parseAsync(new byte[NetworkEnvelope.MAX_SERIALIZED_SIZE], message -> {
            firstParseStarted.countDown();
            finishFirstParse.await();
            return message.length;
        });
        assertThat(firstParseStarted.await(10, TimeUnit.SECONDS)).isTrue();

        int size = budget.parse(new byte[ParseMemoryBudget.MIN_ACCOUNTED_SIZE - 1], message -> message.length);

        assertThat(size).isEqualTo(ParseMemoryBudget.MIN_ACCOUNTED_SIZE - 1);
        finishFirstParse.countDown();
        firstParse.get(10, TimeUnit.SECONDS);
    }

    @Test
    void releasesTheBudgetIfTheParseFails() {
        assertThatThrownBy(() -> budget.parse(new byte[NetworkEnvelope.MAX_SERIALIZED_SIZE], message -> {
            throw new IOException("Invalid message");
        })).isInstanceOf(IOException.class);

        assertThat(budget.getAvailableBytes()).isEqualTo(budget.getBudget());
    }

    @Test
    void rejectsABudgetBelowTheSizeLimitOfAMessage() {
        assertThatThrownBy(() -> new ParseMemoryBudget(NetworkEnvelope.MAX_SERIALIZED_SIZE - 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private interface BlockingParser {
        int parse(byte[] message) throws Exception;
    }

    private CompletableFuture<Integer> parseAsync(byte[] message, BlockingParser parser) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return budget.parse(message, bytes -> {
                    try {
                        return parser.parse(bytes);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException(e);
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }, executor);
    }
}
