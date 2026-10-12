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

package bisq.api.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * A log capture which misses a level or a logger lets a leak pass unnoticed, so these properties are checked here
 * and not assumed by the tests which use it.
 */
@Isolated("Captures the log of every logger")
class LogCaptureTest {
    private final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    private final ExtensionContext context = mock(ExtensionContext.class);
    private final LogCapture logs = new LogCapture();
    private List<TurboFilter> turboFiltersBefore;

    @BeforeEach
    void setUp() {
        turboFiltersBefore = List.copyOf(loggerContext.getTurboFilterList());
        logs.beforeEach(context);
    }

    @AfterEach
    void tearDown() {
        logs.afterEach(context);
    }

    @Test
    void capturesTraceEventsOfLoggersInsideAndOutsideOfBisq() {
        LoggerFactory.getLogger("bisq.api.LogCaptureTest").trace("trace inside of bisq");
        LoggerFactory.getLogger("org.example.LogCaptureTest").trace("trace outside of bisq");

        assertThat(logs.lines()).contains("trace inside of bisq", "trace outside of bisq");
    }

    // For example a level from a test configuration, or from LogSetup.setLevel in a test
    @Test
    void capturesTraceEventsOfALoggerWithItsOwnLevel() {
        Logger logger = (Logger) LoggerFactory.getLogger("bisq.api.LogCaptureTest.OwnLevel");
        logger.setLevel(Level.OFF);
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("guarded trace of a logger with its own level");
            }
            logger.debug("debug of a logger with its own level");

            assertThat(logs.lines()).contains("guarded trace of a logger with its own level",
                    "debug of a logger with its own level");
        } finally {
            logger.setLevel(null);
        }
    }

    @Test
    void capturesTheMessagesOfAnExceptionAndOfItsCause() {
        LoggerFactory.getLogger(LogCaptureTest.class)
                .warn("failed", new IllegalStateException("outer message", new IllegalArgumentException("cause message")));

        assertThat(logs.lines()).singleElement()
                .satisfies(line -> assertThat(line).contains("failed", "outer message", "cause message"));
    }

    @Test
    void capturesTheMessageAsItWasWhenItWasLogged() {
        StringBuilder argument = new StringBuilder("logged value");

        LoggerFactory.getLogger(LogCaptureTest.class).info("value: {}", argument);
        argument.replace(0, argument.length(), "later value");

        assertThat(logs.lines()).containsExactly("value: logged value");
    }

    @Test
    void assertNoLineContainsFailsOnALineWithTheValue() {
        LoggerFactory.getLogger(LogCaptureTest.class).debug("line with a sentinel42");

        assertThatThrownBy(() -> logs.assertNoLineContains("sentinel42"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("line with a sentinel42");
    }

    @Test
    void awaitLineContainingWaitsForALineOfAnotherThread() throws Exception {
        CompletableFuture.runAsync(() ->
                LoggerFactory.getLogger(LogCaptureTest.class).info("from another thread, first part, second part"));

        assertThat(logs.awaitLineContaining("first part", "second part"))
                .isEqualTo("from another thread, first part, second part");
    }

    @Test
    void removesItsTurboFilterAndStopsCapturing() {
        assertThat(loggerContext.getTurboFilterList()).hasSize(turboFiltersBefore.size() + 1);

        logs.afterEach(context);
        LoggerFactory.getLogger(LogCaptureTest.class).error("after the test");

        assertThat(loggerContext.getTurboFilterList()).containsExactlyElementsOf(turboFiltersBefore);
        assertThat(logs.lines()).isEmpty();
        // Started again for tearDown
        logs.beforeEach(context);
    }
}
