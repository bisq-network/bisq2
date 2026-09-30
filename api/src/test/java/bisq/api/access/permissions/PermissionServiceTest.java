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

import bisq.api.access.persistence.ApiAccessStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionServiceTest {
    private ApiAccessStoreService apiAccessStoreService;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        apiAccessStoreService = mock(ApiAccessStoreService.class);
        permissionService = new PermissionService(apiAccessStoreService);
    }

    @Test
    void grantCoveringAllPermissionsIsStoredAsGrantAll() {
        permissionService.putPermissions("client-1", Permission.autoGrantable());

        ArgumentCaptor<PermissionSet> captor = ArgumentCaptor.forClass(PermissionSet.class);
        verify(apiAccessStoreService).putPermissions(eq("client-1"), captor.capture());
        assertTrue(captor.getValue().isGrantAll());
    }

    @Test
    void partialGrantIsStoredExplicitly() {
        Set<Permission> subset = Set.of(Permission.OFFERBOOK, Permission.MARKET_PRICE);

        permissionService.putPermissions("client-1", subset);

        ArgumentCaptor<PermissionSet> captor = ArgumentCaptor.forClass(PermissionSet.class);
        verify(apiAccessStoreService).putPermissions(eq("client-1"), captor.capture());
        assertFalse(captor.getValue().isGrantAll());
        assertEquals(subset, captor.getValue().getPermissions());
    }

    @Test
    void findPermissionsExpandsGrantAllToAllPermissions() {
        when(apiAccessStoreService.getPermissionsByClientId())
                .thenReturn(Map.of("client-1", PermissionSet.grantAll()));

        Optional<Set<Permission>> found = permissionService.findPermissions("client-1");

        assertTrue(found.isPresent());
        assertEquals(Permission.autoGrantable(), found.get());
    }

    @Test
    void findPermissionsReturnsStoredExplicitSubsetAsIs() {
        Set<Permission> subset = Set.of(Permission.MARKET_PRICE);
        when(apiAccessStoreService.getPermissionsByClientId())
                .thenReturn(Map.of("client-1", new PermissionSet(subset)));

        Optional<Set<Permission>> found = permissionService.findPermissions("client-1");

        assertTrue(found.isPresent());
        assertEquals(subset, found.get());
        assertFalse(permissionService.hasPermission(found.get(), Permission.TRADES));
    }

    @Test
    void findPermissionsIsEmptyForUnknownClient() {
        when(apiAccessStoreService.getPermissionsByClientId()).thenReturn(Map.of());

        assertTrue(permissionService.findPermissions("unknown").isEmpty());
    }
}
