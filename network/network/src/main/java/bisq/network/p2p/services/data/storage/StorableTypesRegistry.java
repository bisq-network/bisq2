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

import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Holds every storable type, which is every type that the network storage accepts, together with its local MetaData.
 * The key is the simple class name of the type, which is also the key of its store.
 * <p>
 * Each type belongs to exactly one store type, which the registry derives from the Java type: a MailboxMessage to the
 * mailbox store, an AppendOnlyData to the append-only store and any other DistributedData to the authenticated data
 * store. A store accepts only class names of its own store type.
 * <p>
 * Remove requests, refresh requests and mailbox data do not contain the stored object, or contain it only encrypted.
 * They carry the class name of the stored type instead. The storage layer uses that class name only as the key for
 * this registry. It never uses MetaData values received from the network for its decisions.
 * <p>
 * The application layer registers all storable types at startup. The storage layer does not accept data of a type
 * which is not registered.
 */
public final class StorableTypesRegistry {
    private record Registration(Class<?> storableType, StoreType storeType, MetaData metaData) {
    }

    private static final Map<String, Registration> registrationByClassName = new ConcurrentHashMap<>();

    private StorableTypesRegistry() {
    }

    /**
     * Registering the same type with the same MetaData again has no effect.
     *
     * @throws IllegalArgumentException if the type is neither a MailboxMessage nor a DistributedData, or is both, if
     *                                  the className of the MetaData is not the simple name of the type, or if
     *                                  another type or another MetaData is registered already for that class name.
     */
    public static void register(Class<?> storableType, MetaData metaData) {
        String className = storableType.getSimpleName();
        checkArgument(className.equals(metaData.getClassName()),
                "The className of the MetaData must be the simple name of the type. type=%s; className=%s",
                storableType.getName(), metaData.getClassName());
        Registration registration = new Registration(storableType, toStoreType(storableType), metaData);
        Registration existing = registrationByClassName.putIfAbsent(className, registration);
        checkArgument(existing == null || existing.equals(registration),
                "Another registration exists already for className %s. existing=%s; new=%s",
                className, existing, registration);
    }

    public static Optional<MetaData> findMetaData(String className) {
        return Optional.ofNullable(registrationByClassName.get(className)).map(Registration::metaData);
    }

    /**
     * Mailbox data, remove requests and refresh requests carry the deprecated field metaData for nodes of earlier
     * versions. Those nodes use it for store selection, expiry and the cost factor of the message. A node sends the
     * MetaData which it registered for the class name, also when it relays a message which a sender or another relay
     * filled with other values.
     *
     * @return the registered MetaData of the class name, or the received MetaData if the class name is not registered.
     */
    public static MetaData getMetaDataForEarlierVersions(String className, MetaData receivedMetaData) {
        return findMetaData(className).orElse(receivedMetaData);
    }

    /**
     * For code paths which the storage layer reaches only after it has accepted the class name (see
     * StorageService). A missing registration there is a programming error.
     */
    public static MetaData getMetaData(String className) {
        return findMetaData(className).orElseThrow(() ->
                new IllegalStateException("No MetaData registered for className " + className));
    }

    public static boolean isRegistered(String className, StoreType storeType) {
        Registration registration = registrationByClassName.get(className);
        return registration != null && registration.storeType() == storeType;
    }

    public static Set<String> getClassNames() {
        return Set.copyOf(registrationByClassName.keySet());
    }

    public static Set<String> getClassNames(StoreType storeType) {
        return registrationByClassName.entrySet().stream()
                .filter(entry -> entry.getValue().storeType() == storeType)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    // Mailbox data and remove requests carry only the class name, and the signature does not cover it. The store type
    // prevents that such a request names a type of another store and so gets its MetaData, e.g. a longer time to live.
    private static StoreType toStoreType(Class<?> storableType) {
        boolean isMailboxMessage = MailboxMessage.class.isAssignableFrom(storableType);
        boolean isDistributedData = DistributedData.class.isAssignableFrom(storableType);
        checkArgument(isMailboxMessage != isDistributedData,
                "A storable type must be either a MailboxMessage or a DistributedData. type=%s",
                storableType.getName());
        if (isMailboxMessage) {
            return StoreType.MAILBOX_DATA_STORE;
        }
        return AppendOnlyData.class.isAssignableFrom(storableType)
                ? StoreType.APPEND_ONLY_DATA_STORE
                : StoreType.AUTHENTICATED_DATA_STORE;
    }
}
