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

package bisq.common.fsm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Events can be network messages whose toString contains private data, for example the payment account data of a
 * trade peer. Fsm logs and reports only the type of an event.
 */
@Isolated("Captures the log of Fsm")
class FsmEventLoggingTest {
    private static final String PRIVATE_VALUE = "private-sentinel-value";

    private final Logger logger = (Logger) LoggerFactory.getLogger(Fsm.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void eventInFinalStateIsLoggedByType() {
        SimpleFsm<FsmTest.MockModel> fsm = new SimpleFsm<>(new FsmTest.MockModel(FsmTest.MockState.COMPLETED));

        fsm.handle(new PrivateEvent());

        assertLoggedWithoutPrivateValue();
    }

    @Test
    void eventWithoutTransitionIsLoggedAndReportedByType() {
        Fsm<FsmTest.MockModel> fsm = new SimpleFsm<>(new FsmTest.MockModel(FsmTest.MockState.INIT)) {
            @Override
            public void handle(Event event) {
                try {
                    super.handle(event);
                } catch (FsmException fsmException) {
                    assertFalse(stackTraceWithMessages(fsmException).contains(PRIVATE_VALUE));
                }
            }
        };

        fsm.handle(new PrivateEvent());

        assertLoggedWithoutPrivateValue();
    }

    private void assertLoggedWithoutPrivateValue() {
        assertFalse(appender.list.isEmpty());
        assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("PrivateEvent")));
        for (ILoggingEvent event : appender.list) {
            assertFalse(event.getFormattedMessage().contains(PRIVATE_VALUE), event.getFormattedMessage());
            if (event.getThrowableProxy() != null) {
                String stackTrace = ThrowableProxyUtil.asString(event.getThrowableProxy());
                assertFalse(stackTrace.contains(PRIVATE_VALUE), stackTrace);
            }
        }
    }

    private static String stackTraceWithMessages(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            sb.append(t).append('\n');
        }
        return sb.toString();
    }

    private static class PrivateEvent implements Event {
        @Override
        public String toString() {
            return "PrivateEvent{accountData=" + PRIVATE_VALUE + "}";
        }
    }
}
