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

package bisq.network;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The network layer notifies its listeners with submit on the notify executor. The task keeps an exception of a
 * listener, so the executor logs it (see "Delivery of new messages" in docs/specifications/network/startup-replay.md).
 */
@Isolated("Uses the static NetworkExecutors and captures their log")
class NetworkExecutorsTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(NetworkExecutors.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        NetworkExecutors.shutdown();
        NetworkExecutors.initialize(4);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        NetworkExecutors.shutdown();
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void exceptionOfASubmittedListenerCallIsLogged() throws InterruptedException {
        IllegalStateException exception = new IllegalStateException("Listener failed");
        Runnable failingListenerCall = () -> {
            throw exception;
        };

        runOnNotifyExecutor(failingListenerCall);

        List<ILoggingEvent> errors = errors();
        assertEquals(1, errors.size());
        assertSame(exception, ((ThrowableProxy) errors.get(0).getThrowableProxy()).getThrowable());
    }

    @Test
    void successfulListenerCallIsNotLogged() throws InterruptedException {
        runOnNotifyExecutor(() -> {
        });

        assertTrue(errors().isEmpty());
    }

    // The executor terminates after all workers finished, and a worker calls afterExecute before it finishes
    private static void runOnNotifyExecutor(Runnable listenerCall) throws InterruptedException {
        ThreadPoolExecutor executor = NetworkExecutors.getNotifyExecutor();
        executor.submit(listenerCall);
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    private List<ILoggingEvent> errors() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .toList();
    }
}
