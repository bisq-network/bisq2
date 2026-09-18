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
import bisq.network.p2p.services.data.storage.StoragePolicyAware;
import bisq.network.p2p.services.data.storage.StoragePolicy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Storage properties are declared per type with {@link StoragePolicy} and resolved by
 * {@link MetaData#from(Class)}. A concrete {@link StoragePolicyAware} type which neither declares the annotation nor
 * overrides {@code getMetaData()} compiles fine and only fails once a payload of that type is handled, on every node
 * running the build, so it is checked here instead.
 * <p>
 * The types which legitimately override rather than declare are the wrappers: AuthenticatedData and its subclasses
 * delegate to the payload they hold, MailboxData carries the metadata it received over the wire, and
 * ConfidentialMessage cannot know its content type.
 */
class StoragePolicyCoverageTest {
    @Test
    void everyStoredTypeDeclaresItsStorageProperties() throws Exception {
        List<String> undeclared = new ArrayList<>();
        int concreteTypes = 0;
        for (Class<?> clazz : BisqClasses.bisqClasses()) {
            if (!StoragePolicyAware.class.isAssignableFrom(clazz)
                    || clazz.isInterface()
                    || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            concreteTypes++;
            boolean overrides = clazz.getMethod("getMetaData").getDeclaringClass() != StoragePolicyAware.class;
            if (!overrides && clazz.getAnnotation(StoragePolicy.class) == null) {
                undeclared.add(clazz.getName());
            }
        }
        assertTrue(concreteTypes > 50, "Classpath scan found only " + concreteTypes + " types, it is not working");
        assertEquals(List.of(), undeclared, "Types missing @StoragePolicy");
    }

    /**
     * The className picks the store file, so a subclass inheriting the policy of an abstract base must still resolve
     * its own name. Getting this wrong would merge every subclass into one store.
     */
    @Test
    void everyTypeResolvesItsOwnStoreName() throws Exception {
        for (Class<?> clazz : BisqClasses.bisqClasses()) {
            if (!StoragePolicyAware.class.isAssignableFrom(clazz)
                    || clazz.isInterface()
                    || Modifier.isAbstract(clazz.getModifiers())
                    || clazz.getMethod("getMetaData").getDeclaringClass() != StoragePolicyAware.class) {
                continue;
            }
            assertEquals(clazz.getSimpleName(), MetaData.from(clazz).getClassName());
        }
    }
}
