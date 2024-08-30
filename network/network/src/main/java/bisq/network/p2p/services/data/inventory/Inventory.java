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
import bisq.network.p2p.services.data.DataRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
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

    private Inventory(Collection<? extends DataRequest> entries, boolean maxSizeReached, Optional<Integer> cachedSerializedSize) {
        this.entries = new ArrayList<>(entries);
        this.maxSizeReached = maxSizeReached;
        this.cachedSerializedSize = cachedSerializedSize;

        // We need to sort deterministically as the data is used in the proof of work check
        // TODO (optimize, low prio) dataRequest.serialize() is expensive. We have the hash of the data in most DataRequest implementations.
        //  This could be used and combined with the other remaining data, like signature and pubkey
        // We set serializeForHash to false to ensure that we get the same order in case the peer has different data in the
        // annotated fields
        this.entries.sort(Comparator.comparing((DataRequest dataRequest) -> new ByteArray(dataRequest.serializeForHash())));

        verify();
    }

    @Override
    public void verify() {
        // We tolerate up to double of our max size
        cachedSerializedSize.ifPresent(size -> checkArgument(size <= maxSize * 2));
    }

    @Override
    public bisq.network.protobuf.Inventory toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.Inventory.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.Inventory.newBuilder()
                .addAllEntries(entries.stream()
                        .map(e -> e.toProto(serializeForHash).getDataRequest())
                        .collect(Collectors.toList()))
                .setMaxSizeReached(maxSizeReached);
    }

    public static Inventory fromProto(bisq.network.protobuf.Inventory proto) {
        List<bisq.network.protobuf.DataRequest> entriesList = proto.getEntriesList();
        List<DataRequest> entries = entriesList.stream()
                .map(DataRequest::fromProto)
                .collect(Collectors.toList());
        return new Inventory(entries, proto.getMaxSizeReached(), Optional.of(proto.getSerializedSize()));
    }

    public boolean allDataReceived() {
        return !maxSizeReached;
    }
}
