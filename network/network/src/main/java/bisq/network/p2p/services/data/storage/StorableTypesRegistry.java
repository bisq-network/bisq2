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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Holds every storable type, which is every type that the network storage accepts, together with its local MetaData.
 * The key is the simple class name of the type, which is also the key of its store.
 * <p>
 * Remove requests, refresh requests and mailbox data do not contain the stored object, or contain it only encrypted.
 * They carry the class name of the stored type instead. The storage layer uses that class name only as the key for
 * this registry. It never uses MetaData values received from the network for its decisions.
 * <p>
 * The application layer registers all storable types at startup. The storage layer does not accept data of a type
 * which is not registered.
 */
public final class StorableTypesRegistry {
    private record Registration(Class<?> storableType, MetaData metaData) {
    }

    private static final Map<String, Registration> registrationByClassName = new ConcurrentHashMap<>();

    private StorableTypesRegistry() {
    }

    /**
     * Registering the same type with the same MetaData again has no effect.
     *
     * @throws IllegalArgumentException if the className of the MetaData is not the simple name of the type, or if
     *                                  another type or another MetaData is registered already for that class name.
     */
    public static void register(Class<?> storableType, MetaData metaData) {
        String className = storableType.getSimpleName();
        checkArgument(className.equals(metaData.getClassName()),
                "The className of the MetaData must be the simple name of the type. type=%s; className=%s",
                storableType.getName(), metaData.getClassName());
        Registration registration = new Registration(storableType, metaData);
        Registration existing = registrationByClassName.putIfAbsent(className, registration);
        checkArgument(existing == null || existing.equals(registration),
                "Another registration exists already for className %s. existing=%s; new=%s",
                className, existing, registration);
    }

    public static Optional<MetaData> findMetaData(String className) {
        return Optional.ofNullable(registrationByClassName.get(className)).map(Registration::metaData);
    }

    /**
     * For code paths which the storage layer reaches only after it has accepted the class name (see
     * StorageService). A missing registration there is a programming error.
     */
    public static MetaData getMetaData(String className) {
        return findMetaData(className).orElseThrow(() ->
                new IllegalStateException("No MetaData registered for className " + className));
    }

    /**
     * The proof of work check needs the cost factor of a request before StorageService checks the class name. A type
     * which is not registered (e.g. added in a later version) is not stored, so its MetaData has no impact on the
     * cost. Otherwise the check could reject messages of newer nodes.
     *
     * @return the cost factor of the registered MetaData, or 0 if the class name is not registered.
     */
    public static double getCostFactor(String className) {
        return findMetaData(className).map(MetaData::getCostFactor).orElse(0d);
    }

    public static boolean isRegistered(String className) {
        return registrationByClassName.containsKey(className);
    }

    public static Set<String> getClassNames() {
        return Set.copyOf(registrationByClassName.keySet());
    }
}
