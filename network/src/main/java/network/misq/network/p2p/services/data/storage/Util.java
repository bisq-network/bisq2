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

package network.misq.network.p2p.services.data.storage;

import network.misq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class Util {
    public static List<? extends AuthenticatedDataRequest> getSubSet(List<? extends AuthenticatedDataRequest> map, int filterOffset, int filterRange, int maxItems) {
        int size = map.size();
        checkArgument(filterOffset >= 0);
        checkArgument(filterOffset <= 100);
        checkArgument(filterRange >= 0);
        checkArgument(filterRange <= 100);
        checkArgument(filterOffset + filterRange <= 100);
        int offset = size * filterOffset / 100;
        int range = size * filterRange / 100;
        return map.stream()
                .sorted(Comparator.comparingLong(AuthenticatedDataRequest::getCreated))
                .skip(offset)
                .limit(range)
                .limit(maxItems)
                .collect(Collectors.toList());
    }
}
