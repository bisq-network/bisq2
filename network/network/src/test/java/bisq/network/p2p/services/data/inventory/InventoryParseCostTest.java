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

import bisq.common.data.ByteArray;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilter;
import bisq.network.protobuf.AddAppendOnlyDataRequest;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A peer can send an inventory and a filter which nobody requested, and a node parses a message before it checks its
 * proof of work. The checks of the size run before the expensive conversion of the entries, and the sort of the
 * entries serializes each entry once.
 */
@Isolated("Captures conversion logs and changes the inventory size limit")
class InventoryParseCostTest {
    private final Logger inventoryLogger = (Logger) LoggerFactory.getLogger(Inventory.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private int previousMaxSize;

    @BeforeEach
    void setUp() {
        previousMaxSize = Inventory.maxSize;
        appender.start();
        inventoryLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        inventoryLogger.detachAppender(appender);
        appender.stop();
        Inventory.setMaxSize(previousMaxSize);
    }

    @Test
    void oversizedInventoryIsRejectedBeforeItsEntriesAreConverted() {
        Inventory.setMaxSize(100);
        bisq.network.protobuf.Inventory.Builder builder = bisq.network.protobuf.Inventory.newBuilder();
        for (int i = 0; i < 10; i++) {
            // An entry which cannot be converted logs a diagnostic when the node converts it
            builder.addEntries(bisq.network.protobuf.DataRequest.newBuilder()
                    .setAddAppendOnlyDataRequest(AddAppendOnlyDataRequest.newBuilder()
                            .setAppendOnlyData(Any.newBuilder()
                                    .setTypeUrl("type.googleapis.com/test.InventoryParseCostUnknownData"))));
        }
        bisq.network.protobuf.Inventory proto = builder.build();
        assertThat(proto.getSerializedSize()).isGreaterThan(Inventory.maxSize * 2);

        assertThatThrownBy(() -> Inventory.fromProto(proto)).isInstanceOf(UnresolvableProtobufException.class);
        assertThat(appender.list).isEmpty();
    }

    @Test
    void entriesAreSortedByTheirHashSerializationWhichIsComputedOncePerEntry() {
        Random random = new Random(1);
        List<DataRequest> entries = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            byte[] hashSerialization = new byte[1 + random.nextInt(8)];
            random.nextBytes(hashSerialization);
            DataRequest entry = mock(DataRequest.class);
            when(entry.serializeForHash()).thenReturn(hashSerialization);
            entries.add(entry);
        }
        List<DataRequest> expected = new ArrayList<>(entries);
        expected.sort(Comparator.comparing(entry -> new ByteArray(entry.serializeForHash())));
        Collections.shuffle(entries, random);
        entries.forEach(org.mockito.Mockito::clearInvocations);

        Inventory inventory = new Inventory(entries, false);

        assertThat(new ArrayList<DataRequest>(inventory.getEntries())).containsExactlyElementsOf(expected);
        entries.forEach(entry -> verify(entry, times(1)).serializeForHash());
    }

    @Test
    void filterWithTooManyEntriesIsRejectedBeforeItsEntriesAreConverted() {
        bisq.network.protobuf.HashSetFilter.Builder hashSetFilter = bisq.network.protobuf.HashSetFilter.newBuilder();
        // Entries with an invalid hash fail when the node converts them
        bisq.network.protobuf.HashSetFilterEntry invalidEntry = bisq.network.protobuf.HashSetFilterEntry.newBuilder()
                .setHash(ByteString.copyFrom(new byte[3]))
                .build();
        for (int i = 0; i <= HashSetFilter.MAX_ENTRIES; i++) {
            hashSetFilter.addFilterEntries(invalidEntry);
        }
        bisq.network.protobuf.InventoryFilter proto = bisq.network.protobuf.InventoryFilter.newBuilder()
                .setInventoryFilterType(bisq.network.protobuf.InventoryFilterType.INVENTORYFILTERTYPE_HASH_SET_V2)
                .setHashSetFilter(hashSetFilter)
                .build();

        assertThatThrownBy(() -> HashSetFilter.fromProto(proto))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasRootCauseMessage("Too many filter entries: " + (HashSetFilter.MAX_ENTRIES + 1));
    }
}
