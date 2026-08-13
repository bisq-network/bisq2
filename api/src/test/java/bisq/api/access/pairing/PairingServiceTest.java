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

package bisq.api.access.pairing;

import bisq.api.ApiConfig;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.persistence.ApiAccessStoreService;
import bisq.common.file.FileReaderUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class PairingServiceTest {
    private PairingService pairingService(Path appDataDir) {
        return new PairingService(mock(ApiConfig.class),
                appDataDir,
                mock(ApiAccessStoreService.class),
                mock(PermissionService.class));
    }

    @Test
    void writeCreatesPairingQrCodeFile(@TempDir Path tempDir) throws IOException {
        pairingService(tempDir).writePairingQrCodeToDataDir("first-payload");

        Path file = tempDir.resolve("pairing_qr_code.txt");
        assertTrue(Files.exists(file));
        assertTrue(FileReaderUtils.readUTF8String(file).startsWith("first-payload"));
    }

    @Test
    void writeReplacesExistingPairingQrCodeFile(@TempDir Path tempDir) throws IOException {
        // The node rotates the code every few minutes, so after the first rotation the
        // target file always exists — replacement must succeed and leave no temp file.
        PairingService service = pairingService(tempDir);
        service.writePairingQrCodeToDataDir("first-payload");

        service.writePairingQrCodeToDataDir("second-payload");

        Path file = tempDir.resolve("pairing_qr_code.txt");
        assertTrue(FileReaderUtils.readUTF8String(file).startsWith("second-payload"));
        assertFalse(Files.exists(tempDir.resolve("pairing_qr_code.txt.tmp")));
    }

    @Test
    void atomicMoveNotSupportedFallsBackToPlainReplace(@TempDir Path tempDir) throws IOException {
        PairingService service = pairingService(tempDir);
        service.writePairingQrCodeToDataDir("first-payload");

        try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.move(any(Path.class), any(Path.class),
                            eq(StandardCopyOption.ATOMIC_MOVE), eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenThrow(new AtomicMoveNotSupportedException("tmp", "target", "not supported"));
            service.writePairingQrCodeToDataDir("second-payload");
        }

        Path file = tempDir.resolve("pairing_qr_code.txt");
        assertTrue(FileReaderUtils.readUTF8String(file).startsWith("second-payload"));
        assertFalse(Files.exists(tempDir.resolve("pairing_qr_code.txt.tmp")));
    }

    @Test
    void atomicMoveIoFailureFallsBackToPlainReplace(@TempDir Path tempDir) throws IOException {
        // ATOMIC_MOVE onto an existing target is implementation specific and can fail with a
        // plain IOException (e.g. a concurrent reader holding the file on Windows).
        PairingService service = pairingService(tempDir);
        service.writePairingQrCodeToDataDir("first-payload");

        try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.move(any(Path.class), any(Path.class),
                            eq(StandardCopyOption.ATOMIC_MOVE), eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenThrow(new IOException("target file is in use"));
            service.writePairingQrCodeToDataDir("second-payload");
        }

        Path file = tempDir.resolve("pairing_qr_code.txt");
        assertTrue(FileReaderUtils.readUTF8String(file).startsWith("second-payload"));
        assertFalse(Files.exists(tempDir.resolve("pairing_qr_code.txt.tmp")));
    }
}
