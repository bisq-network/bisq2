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

package bisq.network.p2p.services.data.filter;


import bisq.common.data.ByteArray;
import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
public final class DataFilter implements Proto {
    private final List<FilterEntry> filterEntries;

    public DataFilter(List<FilterEntry> filterEntries) {
        this.filterEntries = filterEntries;
        // We need to sort deterministically as the data is used in the proof of work check
        this.filterEntries.sort(Comparator.comparing((FilterEntry e) -> new ByteArray(e.serialize())));
    }

    public bisq.network.protobuf.DataFilter toProto() {
        return bisq.network.protobuf.DataFilter.newBuilder()
                .addAllFilterEntries(filterEntries.stream()
                        .map(FilterEntry::toProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static DataFilter fromProto(bisq.network.protobuf.DataFilter proto) {
        return new DataFilter(proto.getFilterEntriesList().stream()
                .map(FilterEntry::fromProto)
                .collect(Collectors.toList()));
    }
}