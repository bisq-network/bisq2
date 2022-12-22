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
import bisq.common.proto.Proto;
import bisq.network.p2p.services.data.DataRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
@Slf4j
public final class Inventory implements Proto {
    private final List<? extends DataRequest> entries;
    private final int numDropped;

    public Inventory(Collection<? extends DataRequest> entries, int numDropped) {
        this.entries = new ArrayList<>(entries);
        this.numDropped = numDropped;

        // We need to sort deterministically as the data is used in the proof of work check
        this.entries.sort(Comparator.comparing((DataRequest e) -> new ByteArray(e.serialize())));
    }

    public bisq.network.protobuf.Inventory toProto() {
        return bisq.network.protobuf.Inventory.newBuilder()
                .addAllEntries(entries.stream().map(e -> e.toProto().getDataRequest()).collect(Collectors.toList()))
                .setNumDropped(numDropped)
                .build();
    }

    public static Inventory fromProto(bisq.network.protobuf.Inventory proto) {
        List<bisq.network.protobuf.DataRequest> entriesList = proto.getEntriesList();
        List<DataRequest> entries = entriesList.stream()
                .map(DataRequest::fromProto)
                .collect(Collectors.toList());
        return new Inventory(entries, proto.getNumDropped());
    }
}
