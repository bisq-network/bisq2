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
    private static final Map<Class<? extends StoragePolicyAware>, MetaData> BY_CLASS = new ConcurrentHashMap<>();

    /**
     * The storage policy declared by a payload type, resolved once per class and shared from then on.
     * <p>
     * Keep the {@code get()}, do not fold it into {@code computeIfAbsent}: {@code computeIfAbsent} returns without
     * locking only when the key is the first node in its bin, and 22 of the 71 payload classes are not.
     * {@code get()} never locks.
     * <p>
     * It is worth the care because this runs per comparison while {@code FilterService} sorts a store by priority to
     * answer an inventory request, on network threads. Sorting 10 000 entries: about 0.55 ms back when the policy
     * was an instance field, about 0.9 ms as written, about 1.4 ms with the {@code get()} folded away, and the last
     * of those degrades further when several peers are served at once.
     */
    public static MetaData from(Class<? extends StoragePolicyAware> clazz) {
        MetaData metaData = BY_CLASS.get(clazz);
        return metaData != null ? metaData : BY_CLASS.computeIfAbsent(clazz, MetaData::resolve);
    }

    /**
     * Resolves the policy now and discards the result, so a type which does not declare one fails where it is
     * registered rather than when the first payload of that type is handled.
     * <p>
     * A type which takes its properties from elsewhere overrides {@code getMetaData()} instead of declaring a
     * policy, which is what the wrappers do, so there is nothing to resolve and nothing to check.
     */
    public static void verifyStoragePolicyDeclared(Class<? extends StoragePolicyAware> clazz) {
        if (!overridesAccessor(clazz)) {
            from(clazz);
        }
    }

    private static boolean overridesAccessor(Class<? extends StoragePolicyAware> clazz) {
        try {
            return clazz.getMethod("getMetaData").getDeclaringClass() != StoragePolicyAware.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static MetaData resolve(Class<? extends StoragePolicyAware> clazz) {
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
