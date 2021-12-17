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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import network.misq.network.p2p.message.Proto;

@EqualsAndHashCode
@Getter
public class MetaData implements Proto {
    private final long ttl;
    private final int maxSizeInBytes;
    private final String fileName;

    public MetaData(int maxSizeInBytes, String fileName) {
        this(-1, maxSizeInBytes, fileName);
    }

    public MetaData(long ttl, int maxSizeInBytes, String fileName) {
        this.ttl = ttl;
        this.maxSizeInBytes = maxSizeInBytes;
        this.fileName = fileName;
    }
}
