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
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Meta data for storage properties per DistributedData
 */
@Slf4j
@EqualsAndHashCode
@ToString
@Getter
public final class MetaData implements Proto {
    public static final long TTL_2_DAYS = TimeUnit.DAYS.toMillis(2);
    public static final long TTL_10_DAYS = TimeUnit.DAYS.toMillis(10);
    public static final long TTL_15_DAYS = TimeUnit.DAYS.toMillis(15);
    public static final long TTL_30_DAYS = TimeUnit.DAYS.toMillis(30);
    public static final long TTL_100_DAYS = TimeUnit.DAYS.toMillis(100);

    public static final int MAX_DATA_SIZE_1000 = 1000;
    public static final int MAX_DATA_SIZE_10_000 = 10_000;

    public static final int MAX_MAP_SIZE_100 = 100;
    public static final int MAX_MAP_SIZE_1000 = 1000;
    public static final int MAX_MAP_SIZE_10_000 = 10_000;

    private final long ttl;
    private final int maxDataSize;
    private final String className;

    private transient final int maxMapSize;

    public MetaData(String className) {
        this(TTL_10_DAYS, MAX_DATA_SIZE_10_000, className, MAX_MAP_SIZE_1000);
    }

    public MetaData(long ttl, String className) {
        this(ttl, MAX_DATA_SIZE_10_000, className, MAX_MAP_SIZE_1000);
    }

    public MetaData(long ttl, int maxDataSize, String className) {
        this(ttl, maxDataSize, className, MAX_MAP_SIZE_1000);
    }

    public MetaData(long ttl, String className, int maxMapSize) {
        this(ttl, MAX_DATA_SIZE_10_000, className, maxMapSize);
    }

    public MetaData(long ttl, int maxDataSize, String className, int maxMapSize) {
        this.ttl = ttl;
        this.maxDataSize = maxDataSize;
        this.className = className;
        this.maxMapSize = maxMapSize;

        NetworkDataValidation.validateText(className, 50);
    }

    public bisq.network.protobuf.MetaData toProto() {
        return bisq.network.protobuf.MetaData.newBuilder()
                .setTtl(ttl)
                .setMaxDataSize(maxDataSize)
                .setClassName(className)
                .build();
    }

    public static MetaData fromProto(bisq.network.protobuf.MetaData proto) {
        return new MetaData(proto.getTtl(), proto.getMaxDataSize(), proto.getClassName());
    }
}
