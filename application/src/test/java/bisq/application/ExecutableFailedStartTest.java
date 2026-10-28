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

import bisq.common.platform.PlatformUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The seed node and the oracle node exit with the failure status after a failed start, so that systemd
 * (Restart=on-failure) starts them again (docs/specifications/network/tor-startup.md).
 */
class ExecutableFailedStartTest {
    @Test
    void aFailedStartShutsDownAndExitsWithTheFailureStatus() {
        TestExecutable executable = new TestExecutable();

        executable.shutdownAfterFailedStart();

        verify(executable.applicationService).shutdown();
        assertEquals(List.of(PlatformUtils.EXIT_FAILURE), executable.exitStatuses);
    }

    @Test
    void aShutdownWhichRunsAlreadyKeepsItsExitStatus() {
        TestExecutable executable = new TestExecutable();
        CompletableFuture<Boolean> runningShutdown = new CompletableFuture<>();
        when(executable.applicationService.shutdown()).thenReturn(runningShutdown);

        executable.shutdown();
        executable.shutdownAfterFailedStart();
        runningShutdown.complete(true);

        verify(executable.applicationService, times(1)).shutdown();
        assertEquals(List.of(PlatformUtils.EXIT_SUCCESS), executable.exitStatuses);
    }

    @Test
    void aShutdownAfterAFailedStartIsNotRepeated() {
        TestExecutable executable = new TestExecutable();

        executable.shutdownAfterFailedStart();
        executable.shutdown();

        verify(executable.applicationService, times(1)).shutdown();
        assertEquals(List.of(PlatformUtils.EXIT_FAILURE), executable.exitStatuses);
    }

    @Test
    void aStartFailsWithAnExceptionOrWithoutSuccess() {
        assertFalse(Executable.hasStartFailed(true, null));
        assertTrue(Executable.hasStartFailed(false, null));
        assertTrue(Executable.hasStartFailed(null, null));
        assertTrue(Executable.hasStartFailed(null, new IllegalStateException("Tor did not start")));
    }

    private static class TestExecutable extends Executable<ApplicationService> {
        private final List<Integer> exitStatuses = new CopyOnWriteArrayList<>();

        TestExecutable() {
            super(new String[0]);
        }

        @Override
        protected ApplicationService createApplicationService(String[] args) {
            ApplicationService applicationService = mock(ApplicationService.class);
            when(applicationService.pruneAllBackups()).thenReturn(CompletableFuture.completedFuture(null));
            when(applicationService.readAllPersisted()).thenReturn(CompletableFuture.completedFuture(true));
            when(applicationService.initialize()).thenReturn(new CompletableFuture<>());
            when(applicationService.shutdown()).thenReturn(CompletableFuture.completedFuture(true));
            return applicationService;
        }

        @Override
        protected void setDefaultUncaughtExceptionHandler() {
        }

        @Override
        protected void keepRunning() {
        }

        @Override
        protected void exitJvm() {
            exitStatuses.add(getExitStatus());
        }
    }
}
