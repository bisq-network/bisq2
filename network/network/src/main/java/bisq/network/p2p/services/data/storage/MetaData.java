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

import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Storage properties of a storable type. Each type declares one MetaData, and the storage layer takes it only from the
 * StorableTypesRegistry by class name. See docs/specifications/network/storage-metadata.md.
 */
@Slf4j
@EqualsAndHashCode
@ToString
@Getter
public final class MetaData implements NetworkProto, Canonical {
    public static final long TTL_2_DAYS = TimeUnit.DAYS.toMillis(2);
    public static final long TTL_5_DAYS = TimeUnit.DAYS.toMillis(5);
    public static final long TTL_10_DAYS = TimeUnit.DAYS.toMillis(10);
    public static final long TTL_15_DAYS = TimeUnit.DAYS.toMillis(15);
    public static final long TTL_20_DAYS = TimeUnit.DAYS.toMillis(20);
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

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^[A-Z][A-Za-z0-9_$]*$");

    // How long data are kept in the storage map
    private final long ttl;
    // Used for inventory request priority of delivery if inventory size exceeds limit
    private final int priority;
    // Not used by this version. Nodes of earlier versions use it as an input of the cost factor of messages (see
    // MessageCostPolicy). Not used as limit of the store (see DataStorageService.MAX_MAP_SIZE).
    private final int maxMapSize;
    // Used for name of storage file, for lookup of the store for a given distributedData object and for logging
    private final String className;

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
        if (!CLASS_NAME_PATTERN.matcher(className).matches()) {
            throw new IllegalArgumentException("Invalid className");
        }
    }

    @Override
    public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
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


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    public static final CanonicalSchema<MetaData> DEFAULT_SCHEMA = CanonicalSchema.<MetaData>newBuilder()
            .sint64(1, MetaData::getTtl)
            .sint32(2, MetaData::getPriority)
            .sint32(3, MetaData::getMaxMapSize)
            .string(4, MetaData::getClassName)
            .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<MetaData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> DEFAULT_SCHEMA;
            case PROOF_OF_WORK -> DEFAULT_SCHEMA;
            case TRADE_CONTRACT -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }
}
