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

package bisq.common.proto;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persisted protos can hold private keys, payment account details or trade data, and exception messages can contain
 * values of them. Users share their log files, so the helpers which skip invalid entries log only the type and the
 * size of a proto and a stack trace without messages, for at most three entries, and then the number of skipped
 * entries.
 */
@Isolated("Captures the log of ProtobufUtils")
class ProtobufUtilsLoggingTest {
    private static final String PRIVATE_VALUE = "private-sentinel-value";
    // The private value is in the received proto, in the message of the exception and in the message of its cause
    private static final ProtoConverter<StringValue, String> FAILING_CONVERTER = proto -> {
        throw new UnresolvableProtobufException("Invalid " + proto.getValue(),
                new IllegalArgumentException("Cause " + proto.getValue()));
    };
    private static final StringValue PRIVATE_PROTO = StringValue.of(PRIVATE_VALUE);

    private final Logger logger = (Logger) LoggerFactory.getLogger(ProtobufUtils.class);
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
    void skippedEntryIsLoggedWithTypeSizeAndStackTraceWithoutMessages() {
        assertEquals(List.of(), ProtobufUtils.fromProtoStreamSkippingInvalid(List.of(PRIVATE_PROTO), FAILING_CONVERTER)
                .collect(Collectors.toList()));

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        String message = event.getFormattedMessage();
        assertTrue(message.contains("type=google.protobuf.StringValue, " + PRIVATE_PROTO.getSerializedSize() + " bytes"),
                message);
        // The class name of the root cause and the stack frames, but not the message of any exception
        assertTrue(message.contains(IllegalArgumentException.class.getName()), message);
        assertTrue(message.contains("\n    at "), message);
        assertNoPrivateValue(event);
    }

    @Test
    void manySkippedEntriesAreLoggedAtMostThreeTimesAndThenCounted() {
        List<StringValue> protos = IntStream.range(0, 10).mapToObj(i -> PRIVATE_PROTO).collect(Collectors.toList());

        ProtobufUtils.fromProtoStreamSkippingInvalid(protos, FAILING_CONVERTER).forEach(ignored -> {
        });

        assertEquals(4, appender.list.size());
        assertTrue(appender.list.get(3).getFormattedMessage().contains("We skipped 10 of 10 entries"));
        appender.list.forEach(ProtobufUtilsLoggingTest::assertNoPrivateValue);
    }

    @Test
    void skippedMapEntryIsLoggedWithoutPrivateValues() {
        ProtobufUtils.fromProtoEntryStreamSkippingInvalid(Map.of("key", PRIVATE_PROTO), FAILING_CONVERTER)
                .forEach(ignored -> {
                });

        assertEquals(1, appender.list.size());
        assertNoPrivateValue(appender.list.get(0));
    }

    private static void assertNoPrivateValue(ILoggingEvent event) {
        assertFalse(event.getFormattedMessage().contains(PRIVATE_VALUE), event.getFormattedMessage());
        // A logged throwable would be written with its messages
        assertNull(event.getThrowableProxy());
    }
}
