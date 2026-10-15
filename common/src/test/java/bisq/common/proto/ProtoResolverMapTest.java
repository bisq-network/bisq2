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
import com.google.protobuf.Any;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated("Captures conversion logs")
class ProtoResolverMapTest {
    private final ProtoResolverMap<NetworkProto> resolvers = new NetworkProtoResolverMap<>();
    private final Any proto = Any.newBuilder().setTypeUrl("type.googleapis.com/test.Data").build();
    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        rootLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void unknownTypeLeavesReportingToCaller() {
        assertThrows(UnresolvableProtobufMessageException.class, () -> resolvers.fromAny(proto));

        assertTrue(appender.list.isEmpty(), "Resolving an unknown type must not log before the caller handles it");
    }

    @Test
    void checkedFailureLeavesReportingToCaller() {
        UnresolvableProtobufException failure = new UnresolvableProtobufException("Invalid received data");
        resolvers.addProtoResolver("test.Data", ignored -> {
            throw failure;
        });

        UnresolvableProtobufMessageException exception =
                assertThrows(UnresolvableProtobufMessageException.class, () -> resolvers.fromAny(proto));

        assertSame(failure, exception.getCause());
        assertTrue(appender.list.isEmpty(), "The caller owns reporting of a checked conversion failure");
    }

    @Test
    void uncheckedFailureLeavesReportingToCaller() {
        IllegalArgumentException failure = new IllegalArgumentException("Invalid received data");
        resolvers.addProtoResolver("test.Data", ignored -> {
            throw failure;
        });

        UnresolvableProtobufMessageException exception =
                assertThrows(UnresolvableProtobufMessageException.class, () -> resolvers.fromAny(proto));

        assertSame(failure, exception.getCause());
        assertTrue(appender.list.isEmpty(), "The caller owns reporting of an unchecked conversion failure");
    }
}
