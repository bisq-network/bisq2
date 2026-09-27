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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
}
