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
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.DataRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
@Slf4j
public final class Inventory implements NetworkProto {
    @Setter
    public static int maxSize;

    private final List<? extends DataRequest> entries;
    private final boolean maxSizeReached;
    // transient fields are excluded by default for EqualsAndHashCode
    private transient final Optional<Integer> cachedSerializedSize;

    public Inventory(Collection<? extends DataRequest> entries, boolean maxSizeReached) {
        this(entries, maxSizeReached, Optional.empty());
    }

    private Inventory(Collection<? extends DataRequest> entries,
                      boolean maxSizeReached,
                      Optional<Integer> cachedSerializedSize) {
        this.entries = sortByHashSerialization(entries);
        this.maxSizeReached = maxSizeReached;
        this.cachedSerializedSize = cachedSerializedSize;

        verify();
    }

    @Override
    public void verify() {
        // We tolerate up to double of our max size
        cachedSerializedSize.ifPresent(Inventory::verifySerializedSize);
    }

    private static void verifySerializedSize(int serializedSize) {
        checkArgument(serializedSize <= maxSize * 2, "The inventory is larger than twice the maximum size");
    }

    // We need to sort deterministically as the data is used in the proof of work check. We sort by serializeForHash(),
    // which leaves out the fields annotated with @ExcludeForHash, to ensure that we get the same order in case the peer
    // has different data in the annotated fields. serializeForHash() is expensive, so it runs once per entry and not
    // in each comparison of the sort. The sort is stable, as the sort with a comparator was.
    private static List<DataRequest> sortByHashSerialization(Collection<? extends DataRequest> entries) {
        record KeyedEntry(ByteArray key, DataRequest dataRequest) {
        }
        return entries.stream()
                .map(dataRequest -> new KeyedEntry(new ByteArray(dataRequest.serializeForHash()), dataRequest))
                .sorted(Comparator.comparing(KeyedEntry::key))
                .map(KeyedEntry::dataRequest)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public bisq.network.protobuf.Inventory toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.Inventory.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.Inventory.newBuilder()
                .addAllEntries(entries.stream()
                        .map(e -> e.toProto(serializeForHash).getDataRequest())
                        .collect(Collectors.toList()))
                .setMaxSizeReached(maxSizeReached);
    }

    public static Inventory fromProto(bisq.network.protobuf.Inventory proto) throws UnresolvableProtobufException {
        try {
            // Checked before the entries are converted and sorted, which costs much more than this check. A node parses
            // a message before it checks its proof of work (see the reasons of rule 6 of
            // docs/specifications/network/validation-at-parsing.md).
            verifySerializedSize(proto.getSerializedSize());
            // The entries are requests of other nodes, which the responder only stores. A responder of another version
            // can hold a request which this node cannot parse. We skip such an entry and keep the other entries (see
            // docs/specifications/network/inventory-hash-set-filter.md).
            List<bisq.network.protobuf.DataRequest> entriesList = proto.getEntriesList();
            AtomicInteger numErrors = new AtomicInteger(0);
            List<DataRequest> entries = entriesList.stream()
                    .map(dataRequest -> {
                        try {
                            return DataRequest.fromProto(dataRequest);
                        } catch (UnresolvableProtobufException ignored) {
                            // Log only the type and the size of the entry, as users share their log files
                            // (see UnresolvableProtobufMessageException). Exception causes can contain received values.
                            if (numErrors.incrementAndGet() <= 3) {
                                log.error("Could not create data request from protobuf data request. " +
                                                "messageCase={}, {} bytes",
                                        dataRequest.getMessageCase(), dataRequest.getSerializedSize());
                            }
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return new Inventory(entries, proto.getMaxSizeReached(), Optional.of(proto.getSerializedSize()));
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    // It can be a node has not sent any data but maxSizeReached is false.
    // finalDataDelivered provides the state that some data was delivered and the peer signalled to not have more data
    public boolean finalDataDelivered() {
        return !maxSizeReached && !entries.isEmpty();
    }
}
