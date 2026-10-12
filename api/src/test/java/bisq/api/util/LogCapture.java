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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Captures the events of every logger at every level during a test, in the form in which the log file shows them:
 * the formatted message, and for an event with an exception the stack trace with the messages of the exception and
 * of its causes. A turbo filter accepts every event while the capture runs, so that a logger with its own level, for
 * example from a later test configuration, cannot drop an event before the capture sees it.
 *
 * <p>Not captured: output which does not go through Logback. Grizzly and Jersey log through java.util.logging, which
 * writes to stderr and not to the log file, and the tests do not see it.
 *
 * <p>Use it to show that a secret or private data does not reach the log:
 * <ol>
 *     <li>Drive the real entry point, not a helper which it calls.</li>
 *     <li>Put a sentinel wherever the secret travels: in a request, in a response, in the message of an
 *     exception.</li>
 *     <li>Use sentinels of letters and digits only. A writer can escape other characters, for example the
 *     {@code /} of a Base64 key, and then a search for the original text does not find them.</li>
 *     <li>Wait until the code under test has logged everything for the case, check the sentinels, and then check
 *     at least one line which must be there. Without this positive control, a test which logs nothing passes.</li>
 * </ol>
 *
 * <p>Register it with {@code @RegisterExtension final LogCapture logs = new LogCapture();} and mark the test class
 * {@code @Isolated}, because it captures the log of every logger in the process.
 */
public final class LogCapture implements BeforeEachCallback, AfterEachCallback {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private final List<String> lines = new ArrayList<>();
    private final AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
        @Override
        protected void append(ILoggingEvent event) {
            // Formatted here, as the file appender does it, so that an argument which changes after the log
            // call cannot hide what the log file would contain
            String line = toLine(event);
            synchronized (lines) {
                lines.add(line);
                lines.notifyAll();
            }
        }
    };
    // Logback asks the turbo filters before it checks the level of a logger, also in isDebugEnabled() and
    // isTraceEnabled(). ACCEPT skips the level check.
    private final TurboFilter acceptEveryEvent = new TurboFilter() {
        @Override
        public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params,
                                  Throwable throwable) {
            return FilterReply.ACCEPT;
        }
    };

    @Override
    public void beforeEach(ExtensionContext context) {
        appender.setContext(rootLogger.getLoggerContext());
        appender.start();
        rootLogger.addAppender(appender);
        acceptEveryEvent.setContext(rootLogger.getLoggerContext());
        acceptEveryEvent.start();
        rootLogger.getLoggerContext().addTurboFilter(acceptEveryEvent);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        rootLogger.getLoggerContext().getTurboFilterList().remove(acceptEveryEvent);
        acceptEveryEvent.stop();
        rootLogger.detachAppender(appender);
        appender.stop();
        synchronized (lines) {
            lines.clear();
        }
    }

    public List<String> lines() {
        synchronized (lines) {
            return List.copyOf(lines);
        }
    }

    public void assertNoLineContains(String... values) {
        List<String> capturedLines = lines();
        for (String value : values) {
            assertThat(capturedLines)
                    .as("No log line may contain %s", value)
                    .noneMatch(line -> line.contains(value));
        }
    }

    /**
     * Waits until one line contains all the given texts, and returns that line. Fails with all captured lines if
     * no such line appears within the timeout.
     */
    public String awaitLineContaining(String... texts) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        synchronized (lines) {
            while (true) {
                Optional<String> match = lines.stream()
                        .filter(line -> Arrays.stream(texts).allMatch(line::contains))
                        .findFirst();
                if (match.isPresent()) {
                    return match.get();
                }
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    throw new AssertionError("No log line contains all of " + Arrays.toString(texts) + " after "
                            + TIMEOUT + ". Captured lines:\n" + String.join("\n", lines));
                }
                TimeUnit.NANOSECONDS.timedWait(lines, remainingNanos);
            }
        }
    }

    private static String toLine(ILoggingEvent event) {
        return event.getThrowableProxy() == null
                ? event.getFormattedMessage()
                : event.getFormattedMessage() + "\n" + ThrowableProxyUtil.asString(event.getThrowableProxy());
    }
}
