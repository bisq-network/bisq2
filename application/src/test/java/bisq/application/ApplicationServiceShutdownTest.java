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

package bisq.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The applications other than the desktop application shut their services down with
 * {@link ApplicationService#shutdownInOrder}. A service which fails, for example the network when Tor processes survive
 * the shutdown of Tor, must not keep the later services from shutting down.
 */
class ApplicationServiceShutdownTest {
    @Test
    void theStepsAfterAFailedStepStillRunAndTheShutdownReportsTheFailure() throws Exception {
        List<String> steps = new ArrayList<>();

        boolean result = ApplicationService.shutdownInOrder(List.of(
                () -> step(steps, "identity", CompletableFuture.completedFuture(true)),
                () -> step(steps, "network", CompletableFuture.failedFuture(new IllegalStateException("Tor still runs"))),
                () -> step(steps, "security", CompletableFuture.completedFuture(true)),
                () -> step(steps, "memoryReport", CompletableFuture.completedFuture(true))
        )).get(5, TimeUnit.SECONDS);

        assertEquals(List.of("identity", "network", "security", "memoryReport"), steps);
        assertFalse(result);
    }

    @Test
    void aStepWhichThrowsInsteadOfReturningAFailedFutureDoesNotStopTheShutdown() throws Exception {
        List<String> steps = new ArrayList<>();

        boolean result = ApplicationService.shutdownInOrder(List.of(
                () -> {
                    throw new IllegalStateException("Service was not created");
                },
                () -> step(steps, "security", CompletableFuture.completedFuture(true))
        )).get(5, TimeUnit.SECONDS);

        assertEquals(List.of("security"), steps);
        assertFalse(result);
    }

    // As before, a step which completes with false does not change the result unless it is the last one.
    @Test
    void withoutFailureTheResultIsTheResultOfTheLastStep() throws Exception {
        assertTrue(ApplicationService.shutdownInOrder(List.of(
                () -> CompletableFuture.completedFuture(false),
                () -> CompletableFuture.completedFuture(true)
        )).get(5, TimeUnit.SECONDS));
        assertFalse(ApplicationService.shutdownInOrder(List.of(
                () -> CompletableFuture.completedFuture(true),
                () -> CompletableFuture.completedFuture(false)
        )).get(5, TimeUnit.SECONDS));
        assertTrue(ApplicationService.shutdownInOrder(List.of()).get(5, TimeUnit.SECONDS));
    }

    private static CompletableFuture<Boolean> step(List<String> steps, String name, CompletableFuture<Boolean> result) {
        steps.add(name);
        return result;
    }
}
