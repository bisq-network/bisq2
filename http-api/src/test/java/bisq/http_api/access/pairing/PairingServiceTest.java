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

package bisq.http_api.access.pairing;

import bisq.http_api.access.permissions.Permission;
import bisq.http_api.access.permissions.PermissionMapping;
import bisq.http_api.access.permissions.PermissionService;
import bisq.http_api.access.persistence.ApiAccessStoreService;
import bisq.http_api.access.transport.TlsContext;
import bisq.http_api.access.transport.TorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PairingServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreatePairingCode() {
        ApiAccessStoreService storeService = mock(ApiAccessStoreService.class);
        PermissionService<? extends PermissionMapping> permissionService = mock(PermissionService.class);
        
        PairingService pairingService = new PairingService(
                tempDir,
                false, // don't write to disk in this test
                storeService,
                permissionService
        );

        PairingCode pairingCode = pairingService.createPairingCode(Set.of(Permission.OFFERBOOK));

        assertNotNull(pairingCode);
        assertNotNull(pairingCode.getId());
        assertNotNull(pairingCode.getExpiresAt());
        assertEquals(Set.of(Permission.OFFERBOOK), pairingCode.getGrantedPermissions());
    }

    @Test
    void testCreatePairingQrCode() {
        ApiAccessStoreService storeService = mock(ApiAccessStoreService.class);
        PermissionService<? extends PermissionMapping> permissionService = mock(PermissionService.class);
        
        PairingService pairingService = new PairingService(
                tempDir,
                false,
                storeService,
                permissionService
        );

        PairingCode pairingCode = pairingService.createPairingCode(Set.of(Permission.OFFERBOOK));

        pairingService.createPairingQrCode(
                pairingCode,
                "ws://localhost:8090",
                Optional.empty(),
                Optional.empty()
        );

        // Verify QR code was generated
        String qrCode = pairingService.getPairingQrCode().get();
        assertNotNull(qrCode);
        assertFalse(qrCode.isEmpty());
    }

    @Test
    void testWritePairingQrCodeToDisk() throws Exception {
        ApiAccessStoreService storeService = mock(ApiAccessStoreService.class);
        PermissionService<? extends PermissionMapping> permissionService = mock(PermissionService.class);

        PairingService pairingService = new PairingService(
                tempDir,
                true, // enable writing to disk
                storeService,
                permissionService
        );

        PairingCode pairingCode = pairingService.createPairingCode(Set.of(Permission.OFFERBOOK));
        
        pairingService.createPairingQrCode(
                pairingCode,
                "ws://localhost:8090",
                Optional.empty(),
                Optional.empty()
        );

        // Verify file was created
        Path qrCodeFile = tempDir.resolve("pairing_qr_code.txt");
        assertTrue(Files.exists(qrCodeFile));
        
        String fileContent = Files.readString(qrCodeFile);
        assertFalse(fileContent.isEmpty());
        
        // Verify it matches the observable value
        assertEquals(fileContent, pairingService.getPairingQrCode().get());
    }
}

