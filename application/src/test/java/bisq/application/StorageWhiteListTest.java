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

import bisq.common.proto.NetworkStorageWhiteList;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A payload type whose simple name is not in {@link NetworkStorageWhiteList} cannot be stored at all:
 * {@code StorageService} rejects the store key and returns a failed future, so for a mailbox message the delivery
 * fails instead of being kept until the peer returns.
 * <p>
 * The list is maintained by hand, and {@code ResolverConfig} warns that subclasses of a registered abstract type
 * have to be added one by one. {@code ResolverConfigTest} pins what is registered, which confirms the entries that
 * exist but cannot report one that was never written. No compile time check sees an absent entry in a list either,
 * so the two sets are compared here.
 */
class StorageWhiteListTest {
    @Test
    void everyStoredTypeIsAKnownStoreKey() throws Exception {
        ResolverConfig.config();
        Set<String> allowed = NetworkStorageWhiteList.getClassNames();
        List<Class<?>> scanned = BisqClasses.bisqClasses();

        // Guards against the rest of this passing on an empty scan. Anchored to the registered names rather than to
        // a count, which would drift as types are added and would still pass if one source stopped being scanned.
        Set<String> scannedNames = scanned.stream().map(Class::getSimpleName).collect(Collectors.toSet());
        assertEquals(List.of(), allowed.stream().filter(name -> !scannedNames.contains(name)).sorted().toList(),
                "The classpath scan did not find these registered types, so it is not seeing everything");

        List<String> unknown = new ArrayList<>();
        for (Class<?> clazz : scanned) {
            if (clazz.isInterface()
                    || Modifier.isAbstract(clazz.getModifiers())
                    || !isStored(clazz)
                    // The encrypted envelope takes the properties of the MailboxData holding it, never its own, so
                    // it is not a store key. Its getMetaData() throws for that reason.
                    || clazz == ConfidentialMessage.class) {
                continue;
            }
            if (!allowed.contains(clazz.getSimpleName())) {
                unknown.add(clazz.getName());
            }
        }
        assertEquals(List.of(), unknown,
                "Stored types missing from NetworkStorageWhiteList. StorageService refuses their store key, so "
                        + "storing them fails. Add them to ResolverConfig");
    }

    private static boolean isStored(Class<?> clazz) {
        return DistributedData.class.isAssignableFrom(clazz) || MailboxMessage.class.isAssignableFrom(clazz);
    }
}
