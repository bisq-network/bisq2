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

package bisq.network.p2p.services.data.inventory;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.storage.DistributedDataResolver;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.protobuf.AddAppendOnlyDataRequest;
import bisq.network.protobuf.DataRequest;
import bisq.network.protobuf.MetaData;
import bisq.network.protobuf.RemoveAuthenticatedDataRequest;
import bisq.security.keys.KeyGeneration;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated("Captures conversion logs and changes the inventory size limit")
class InventoryLoggingTest {
    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private int previousMaxSize;

    @BeforeEach
    void setUp() {
        previousMaxSize = Inventory.maxSize;
        Inventory.setMaxSize(2_000_000);
        appender.start();
        rootLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(appender);
        appender.stop();
        Inventory.setMaxSize(previousMaxSize);
    }

    @Test
    void unknownTypesProduceAtMostThreeDiagnosticsPerResponse() throws UnresolvableProtobufException {
        assertOnlyThreeDiagnosticsPerResponse(appendOnlyRequest("test.InventoryLoggingUnknownData"));
    }

    @Test
    void nestedResolverFailuresProduceAtMostThreeDiagnosticsPerResponse() throws UnresolvableProtobufException {
        String type = "test.InventoryLoggingRejectedData";
        DistributedDataResolver.addResolver(type, AppendOnlyData.class, ignored -> {
            throw new UnresolvableProtobufException(new IllegalArgumentException("Rejected test data"));
        });

        assertOnlyThreeDiagnosticsPerResponse(appendOnlyRequest(type));
    }

    @Test
    void receivedValuesAreAbsentFromMessagesAndThrowableOutput() throws UnresolvableProtobufException {
        String marker = "RECEIVED_FIELD_MUST_NOT_BE_LOGGED\n".repeat(4);
        // Metadata is validated before the other request fields, after decoding the owner's key.
        @SuppressWarnings("deprecation")
        DataRequest invalid = DataRequest.newBuilder()
                .setRemoveAuthenticatedDataRequest(RemoveAuthenticatedDataRequest.newBuilder()
                        .setOwnerPublicKeyBytes(ByteString.copyFrom(
                                KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded()))
                        .setMetaData(MetaData.newBuilder().setClassName(marker)))
                .build();

        Inventory inventory = Inventory.fromProto(bisq.network.protobuf.Inventory.newBuilder()
                .addEntries(invalid)
                .build());

        assertTrue(inventory.getEntries().isEmpty());
        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.getFirst();
        String rendered = event.getFormattedMessage();
        if (event.getThrowableProxy() != null) {
            rendered += ThrowableProxyUtil.asString(event.getThrowableProxy());
        }
        assertFalse(rendered.contains("RECEIVED_FIELD_MUST_NOT_BE_LOGGED"));
        assertNull(event.getThrowableProxy(), "Raw exception causes can contain received values");
        assertTrue(rendered.contains(invalid.getMessageCase().name()));
        assertTrue(rendered.contains(invalid.getSerializedSize() + " bytes"));
    }

    private void assertOnlyThreeDiagnosticsPerResponse(DataRequest invalid) throws UnresolvableProtobufException {
        bisq.network.protobuf.Inventory.Builder builder = bisq.network.protobuf.Inventory.newBuilder()
                .setMaxSizeReached(true);
        for (int i = 0; i < 10; i++) {
            builder.addEntries(invalid);
        }
        bisq.network.protobuf.Inventory proto = builder.build();
        // The reporting budget resets for each response, not for each resolver or for the connection.
        for (int response = 0; response < 2; response++) {
            appender.list.clear();
            Inventory inventory = Inventory.fromProto(proto);

            assertTrue(inventory.getEntries().isEmpty());
            assertTrue(inventory.isMaxSizeReached());
            assertEquals(3, appender.list.size(), "Nested resolvers must not bypass the response's diagnostic limit");
            assertTrue(appender.list.stream().allMatch(event -> event.getLoggerName().equals(Inventory.class.getName())));
        }
    }

    private static DataRequest appendOnlyRequest(String type) {
        return DataRequest.newBuilder()
                .setAddAppendOnlyDataRequest(AddAppendOnlyDataRequest.newBuilder()
                        .setAppendOnlyData(Any.newBuilder().setTypeUrl("type.googleapis.com/" + type)))
                .build();
    }
}
