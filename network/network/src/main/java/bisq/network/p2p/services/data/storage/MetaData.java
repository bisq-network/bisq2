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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The storage properties in effect for one stored entry: how long it is kept, how it is prioritised when an
 * inventory response is truncated, how many entries its store holds, and which store it belongs to.
 * <p>
 * Usually it is resolved locally from the {@link StoragePolicy} declared by the payload type, and is then as
 * trustworthy as the running build. It is sent between peers only where a receiver has no payload to resolve it
 * from: remove and refresh requests carry a hash rather than the payload, and MailboxData wraps an encrypted
 * message whose type it cannot know. A MetaData built by {@link #fromProto} therefore holds unverified remote
 * input, and unlike a declared policy its values are not restricted to {@link Ttl}, {@link Priority} and
 * {@link MaxMapSize}.
 */
@Slf4j
@EqualsAndHashCode
@ToString
@Getter
public final class MetaData implements NetworkProto {
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^[A-Z][A-Za-z0-9_$]*$");

    // MetaData is constant per class, so we resolve it once per class and share the instance.
    private static final Map<Class<?>, MetaData> BY_CLASS = new ConcurrentHashMap<>();

    public static MetaData from(Class<?> clazz) {
        // The get() is not redundant, do not fold it into computeIfAbsent().
        //
        // ConcurrentHashMap.computeIfAbsent returns without locking only when the key is the first node in its bin;
        // its source marks that branch "check first node without acquiring lock". A key that shares a bin with
        // another one falls through to synchronized(f) instead, on every call, whether or not the value is already
        // cached. get() is lock free for every node.
        //
        // Measured on this map: 71 payload classes land in a 128 slot table occupying 49 bins, so 22 of them, just
        // under a third, are not the first node and would take the monitor on every lookup. Which classes those
        // are depends on identity hash codes, so it is not the same set on another JVM, but the proportion is
        // stable: a uniform model of 71 keys in 128 bins gives a median of 16 and a 95th percentile of 21.
        //
        // This is not a cold path. FilterService sorts a whole store by getMetaData().getPriority() when answering
        // an inventory request, two calls per comparison, so a store at its 10 000 entry cap makes on the order of
        // 270 000 calls per request, on network threads, concurrently for different peers. Before the storage
        // policy moved onto the annotation, this was an instance field read.
        MetaData metaData = BY_CLASS.get(clazz);
        return metaData != null ? metaData : BY_CLASS.computeIfAbsent(clazz, MetaData::resolve);
    }

    /**
     * Resolves the policy now and discards the result, so a type which does not declare one fails where it is
     * registered rather than when the first payload of that type is handled.
     */
    public static void verifyStoragePolicyDeclared(Class<?> clazz) {
        from(clazz);
    }

    private static MetaData resolve(Class<?> clazz) {
        StoragePolicy annotation = clazz.getAnnotation(StoragePolicy.class);
        checkArgument(annotation != null, "%s is missing the @StoragePolicy annotation", clazz.getName());
        return new MetaData(annotation.ttl().getMillis(),
                annotation.priority().getValue(),
                clazz.getSimpleName(),
                annotation.maxMapSize().getValue());
    }

    // How long data are kept in the storage map
    private final long ttl;
    // Used for inventory request priority of delivery if inventory size exceeds limit
    private final int priority;
    // Used for name of storage file, for lookup of the store for a given distributedData object and for logging
    private final String className;
    // Max file size of the storage file
    private final int maxMapSize;

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

    public double getCostFactor() {
        double ttlImpact = MathUtils.bounded(0, 1, ttl / (double) Ttl.DAYS_100.getMillis());
        double mapSizeImpact = MathUtils.bounded(0, 1, maxMapSize / (double) MaxMapSize.SIZE_10_000.getValue());
        double impact = ttlImpact + mapSizeImpact;
        return MathUtils.bounded(0, 1, impact);
    }
}
