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

import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestPermissionMappingTest {
    private final RestPermissionMapping mapping = new RestPermissionMapping();

    @Test
    void clientEndpointsRequireClientManagement() {
        // These were unreachable before the rule existed: unmapped paths fail closed, so the
        // revoke endpoint answered 403 for every client.
        assertEquals(Permission.CLIENT_MANAGEMENT, mapping.getRequiredPermission("/api/v1/access/clients", "GET"));
        assertEquals(Permission.CLIENT_MANAGEMENT,
                mapping.getRequiredPermission("/api/v1/access/clients/client-1", "DELETE"));
    }

    @Test
    void unmappedPathsFailClosed() {
        assertThrows(ForbiddenException.class, () -> mapping.getRequiredPermission("/api/v1/unmapped", "GET"));
    }
}
