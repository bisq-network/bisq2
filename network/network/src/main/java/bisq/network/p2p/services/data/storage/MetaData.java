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

import bisq.common.proto.NetworkProto;
import bisq.common.util.MathUtils;
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
public final class MetaData implements NetworkProto {
    public static final long TTL_2_DAYS = TimeUnit.DAYS.toMillis(2);
    public static final long TTL_10_DAYS = TimeUnit.DAYS.toMillis(10);
    public static final long TTL_15_DAYS = TimeUnit.DAYS.toMillis(15);
    public static final long TTL_30_DAYS = TimeUnit.DAYS.toMillis(30);
    public static final long TTL_100_DAYS = TimeUnit.DAYS.toMillis(100);

    public static final int MAX_MAP_SIZE_100 = 100;
    public static final int MAX_MAP_SIZE_1000 = 1000;
    public static final int MAX_MAP_SIZE_5000 = 5000;
    public static final int MAX_MAP_SIZE_10_000 = 10_000;
    public static final int MAX_MAP_SIZE_50_000 = 50_000;

    public static final int LOW_PRIORITY = -1;
    public static final int DEFAULT_PRIORITY = 0;
    public static final int HIGH_PRIORITY = 1;
    public static final int HIGHEST_PRIORITY = 2;

    // How long data are kept in the storage map
    private final long ttl;
    // Used for inventory request priority of delivery if inventory size exceeds limit
    private final int priority;
    // Used for name of storage file, for lookup of the store for a given distributedData object and for logging
    private final String className;
    // Max file size of the storage file
    private final int maxMapSize;

    public MetaData(String className) {
        this(TTL_10_DAYS, className);
    }

    public MetaData(long ttl, String className) {
        this(ttl, className, MAX_MAP_SIZE_1000);
    }

    public MetaData(long ttl, int priority, String className) {
        this(ttl, priority, className, MAX_MAP_SIZE_1000);
    }

    public MetaData(long ttl, String className, int maxMapSize) {
        this(ttl, DEFAULT_PRIORITY, className, maxMapSize);
    }

    public MetaData(long ttl, int priority, String className, int maxMapSize) {
        this.ttl = ttl;
        this.priority = priority;
        this.className = className;
        this.maxMapSize = maxMapSize;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(className, 50);
    }

    @Override
    public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.MetaData.newBuilder()
                .setTtl(ttl)
                .setPriority(priority)
                .setClassName(className)
                .setMaxMapSize(maxMapSize);
    }

    public static MetaData fromProto(bisq.network.protobuf.MetaData proto) {
        return new MetaData(proto.getTtl(), proto.getPriority(), proto.getClassName(), proto.getMaxMapSize());
    }

    public double getCostFactor() {
        double ttlImpact = MathUtils.bounded(0, 1, ttl / (double) TTL_100_DAYS);
        double mapSizeImpact = MathUtils.bounded(0, 1, maxMapSize / (double) MAX_MAP_SIZE_10_000);
        double impact = ttlImpact + mapSizeImpact;
        return MathUtils.bounded(0, 1, impact);
    }
}
