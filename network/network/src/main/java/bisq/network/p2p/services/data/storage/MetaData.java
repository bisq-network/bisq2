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

package bisq.network.p2p.services.data.storage;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

/**
 * Meta data for storage properties per DistributedData
 */
@EqualsAndHashCode
@ToString
@Getter
public final class MetaData implements Proto {
    private final long ttl;
    private final int maxSizeInBytes;
    private final String className;

    private transient final int maxMapSize;

    public MetaData(long ttl, int maxSizeInBytes, String className) {
        this(ttl, maxSizeInBytes, className, 10_000);
    }

    public MetaData(String className) {
        this(TimeUnit.DAYS.toMillis(10), 100_000, className, 10_000);
    }

    public MetaData(long ttl, int maxSizeInBytes, String className, int maxMapSize) {
        this.ttl = ttl;
        this.maxSizeInBytes = maxSizeInBytes;
        this.className = className;
        this.maxMapSize = maxMapSize;
    }

    public bisq.network.protobuf.MetaData toProto() {
        return bisq.network.protobuf.MetaData.newBuilder()
                .setTtl(ttl)
                .setMaxSizeInBytes(maxSizeInBytes)
                .setClassName(className)
                .build();
    }

    public static MetaData fromProto(bisq.network.protobuf.MetaData proto) {
        return new MetaData(proto.getTtl(), proto.getMaxSizeInBytes(), proto.getClassName());
    }
}
