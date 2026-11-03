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

package bisq.application;

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MetaData is a property of a type. Each storable type declares it once as the constant META_DATA. The class name in
 * MetaData selects the store and the name of the store file, so it must be the simple name of the declaring type.
 * A copied constant with the name of another type would put the data into the store of that other type.
 */
class StorableTypeMetaDataTest {
    @Test
    void everyStorableTypeDeclaresMetaDataWithItsOwnClassName() throws Exception {
        List<Class<?>> storableTypes = StorableTypes.find();
        // The scan must see the types of all modules. If it finds only a few, the classpath scan does not work.
        assertTrue(storableTypes.size() > 50, "Found only " + storableTypes.size() + " storable types");

        List<String> errors = new ArrayList<>();
        for (Class<?> type : storableTypes) {
            Field field;
            try {
                field = type.getDeclaredField("META_DATA");
            } catch (NoSuchFieldException e) {
                errors.add(type.getName() + " does not declare META_DATA");
                continue;
            }
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) ||
                    field.getType() != MetaData.class) {
                errors.add(type.getName() + ".META_DATA is not a public static final MetaData");
                continue;
            }
            String className = ((MetaData) field.get(null)).getClassName();
            if (!type.getSimpleName().equals(className)) {
                errors.add(type.getName() + ".META_DATA has className " + className);
            }
        }
        assertEquals(List.of(), errors);
    }

    /**
     * A MetaData on a type which the network storage never stores has no effect, while it reads as if the type were
     * kept for that ttl and ordered by that priority. AuthorizeAccountTimestampV1Payload and V2Payload carried one
     * from the commit which split them out of a MailboxMessage until it was removed again, and no test noticed.
     * <p>
     * Only a concrete type is checked. An abstract base or an interface may hold one for its subtypes.
     */
    @Test
    void onlyAStorableTypeDeclaresMetaData() throws Exception {
        Set<Class<?>> storableTypes = Set.copyOf(StorableTypes.find());
        List<String> errors = new ArrayList<>();
        for (Class<?> type : StorableTypes.findAll()) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers()) || storableTypes.contains(type)) {
                continue;
            }
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() == MetaData.class && Modifier.isStatic(field.getModifiers())) {
                    errors.add(type.getName() + " declares " + field.getName() + ", but is neither a DistributedData"
                            + " nor a MailboxMessage, so the storage never stores it");
                }
            }
        }
        assertEquals(List.of(), errors);
    }

    /**
     * The network storage does not accept a type which is missing in the StorableTypesRegistry. A type which is added
     * to the code but not to ResolverConfig could not be stored.
     */
    @Test
    void resolverConfigRegistersEveryStorableTypeWithItsMetaData() throws Exception {
        ResolverConfig.config();

        List<Class<?>> storableTypes = StorableTypes.find();
        List<String> errors = new ArrayList<>();
        for (Class<?> type : storableTypes) {
            MetaData metaData = (MetaData) type.getDeclaredField("META_DATA").get(null);
            if (StorableTypesRegistry.findMetaData(type.getSimpleName()).orElse(null) != metaData) {
                errors.add(type.getName() + " is not registered with its META_DATA");
            }
        }
        assertEquals(List.of(), errors);

        Set<String> storableClassNames = storableTypes.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        Set<String> registeredWithoutStorableType = StorableTypesRegistry.getClassNames().stream()
                .filter(className -> !storableClassNames.contains(className))
                .collect(Collectors.toSet());
        assertEquals(Set.of(), registeredWithoutStorableType);
    }
}
