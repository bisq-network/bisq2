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

package bisq.api.access.permissions;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionTest {

    @Test
    void everyCurrentPermissionIsStandard() {
        // The 12 current permissions are all standard. This documents that intent; the security
        // guarantee itself comes from the SENSITIVE default (an id-only declaration is not
        // auto-grantable), so a forgotten classification fails closed rather than silently
        // joining grantAll. Whoever adds the first sensitive permission updates this test AND
        // verifies its explicit per-device grant flow.
        assertTrue(Arrays.stream(Permission.values()).allMatch(Permission::isAutoGrantable),
                "a sensitive permission was introduced — update this test and verify its explicit-grant flow");
    }

    @Test
    void autoGrantableEqualsAllValuesWhileNoSensitivePermissionExists() {
        assertEquals(EnumSet.allOf(Permission.class), Permission.autoGrantable());
    }

    @Test
    void autoGrantableSetIsUnmodifiable() {
        // PermissionSet caches this in a static field and returns it directly from
        // getPermissions(); a mutable set would let any caller alter the grantAll expansion for
        // every client (and add a sensitive permission once one exists).
        assertThrows(UnsupportedOperationException.class,
                () -> Permission.autoGrantable().add(Permission.OFFERBOOK));
        assertThrows(UnsupportedOperationException.class,
                () -> Permission.autoGrantable().clear());
    }
}
