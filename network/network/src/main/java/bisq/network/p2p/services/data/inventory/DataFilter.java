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


import bisq.common.proto.NetworkProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
public final class DataFilter implements NetworkProto {
    // FilterEntry has about 24 bytes (hash of 20 bytes + integer). 200_000 items are about 4.8 MB)
    // We should aim to be much below that limit.
    public final static int MAX_ENTRIES = 200_000;

    private final List<FilterEntry> filterEntries;

    public DataFilter(List<FilterEntry> filterEntries) {
        this.filterEntries = filterEntries;
        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.filterEntries);

        verify();
    }

    @Override
    public void verify() {
        checkArgument(filterEntries.size() < MAX_ENTRIES);
    }

    @Override
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