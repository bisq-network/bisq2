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

package bisq.network.p2p.node;

import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule 8 of docs/specifications/network/validation-at-parsing.md: a peer of a later version sends features and
 * transport types which this version does not know. A handshake request alone can hold tens of thousands of them.
 */
@Isolated("Captures the log of ProtobufUtils and changes its level")
class CapabilityUnknownValuesTest {
    private static final int NUM_UNKNOWN_VALUES = 1_000;

    private final Logger logger = (Logger) LoggerFactory.getLogger(ProtobufUtils.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

    @Test
    void unknownValuesAreSkippedAndLoggedOncePerList() throws UnresolvableProtobufException {
        bisq.network.protobuf.Capability.Builder builder = Capability.myCapability(Address.from("127.0.0.1", 1000),
                        new ArrayList<>(List.of(TransportType.CLEAR)),
                        new ArrayList<>(List.of(Feature.INVENTORY_HASH_SET_V2)))
                .toProto(false)
                .toBuilder();
        for (int i = 0; i < NUM_UNKNOWN_VALUES; i++) {
            builder.addFeaturesValue(1_000 + i);
            builder.addSupportedTransportTypes("TRANSPORT_OF_A_LATER_VERSION");
        }

        Capability capability = Capability.fromProto(builder.build());

        assertThat(capability.getFeatures()).containsExactly(Feature.INVENTORY_HASH_SET_V2);
        assertThat(capability.getSupportedTransportTypes()).containsExactly(TransportType.CLEAR);
        assertThat(appender.list).hasSize(2);
        appender.list.forEach(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(event.getFormattedMessage()).contains(String.valueOf(NUM_UNKNOWN_VALUES))
                    .doesNotContain("TRANSPORT_OF_A_LATER_VERSION");
        });
    }

    @Test
    void knownValuesAreResolvedWithoutLog() throws UnresolvableProtobufException {
        Capability capability = Capability.myCapability(Address.from("127.0.0.1", 1000),
                new ArrayList<>(List.of(TransportType.TOR, TransportType.CLEAR)),
                new ArrayList<>(List.of(Feature.INVENTORY_HASH_SET,
                        Feature.AUTHORIZATION_HASH_CASH_V2,
                        Feature.INVENTORY_HASH_SET_V2)));

        Capability parsed = Capability.fromProto(capability.toProto(false));

        assertThat(parsed).isEqualTo(capability);
        assertThat(appender.list).isEmpty();
    }
}
