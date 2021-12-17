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

package network.misq.network.p2p.services.data.filter;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import network.misq.network.p2p.services.data.storage.MapKey;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Getter
public class ProtectedDataFilter implements DataFilter {
    private final String dataType;
    private final Set<FilterItem> filterItems;
    transient private final Map<MapKey, Integer> filterMap;
    private final int range;
    private final int offset;

    public ProtectedDataFilter(String dataType, Set<FilterItem> filterItems) {
        this(dataType, filterItems, 100, 0);
    }

    /**
     * @param dataType    Class name
     * @param filterItems
     * @param range       0-100. percentage of the data space we want to get. e.g. 25 means 25% of all data.
     * @param offset      offset for the range. e.g. 25 means start at 25% of data range. data is sorted deterministically.
     */
    public ProtectedDataFilter(String dataType, Set<FilterItem> filterItems, int range, int offset) {
        this.dataType = dataType;
        this.filterItems = filterItems;
        filterMap = filterItems.stream()
                .collect(Collectors.toMap(e -> new MapKey(e.getHash()), FilterItem::getSequenceNumber));
        this.range = range;
        this.offset = offset;
    }
}
