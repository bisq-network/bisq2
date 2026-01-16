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

package bisq.security;

import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SignatureException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class PgPUtilsTest {
    @BeforeEach
    void setUp() throws IOException {
        FileMutatorUtils.createDirectories(Paths.get("temp"));
    }

    @AfterEach
    void tearDown() throws IOException {
        FileMutatorUtils.deleteFileOrDirectory(Paths.get("temp"));
    }

    @Test
    public void testReadPgpPublicKeyRing() {
        try {
            PGPPublicKeyRing pgpPublicKeyRing = getPGPPublicKeyRing("387C8307.asc");
            String keyId = Integer.toHexString((int) pgpPublicKeyRing.getPublicKey().getKeyID()).toUpperCase(Locale.ROOT);
            assertEquals("387C8307", keyId);
        } catch (Exception e) {
            log.error(e.toString());
            fail();
        }
    }

    @Test
    public void testReadPgpSignature() {
        // To create a test signature use:
        // gpg --digest-algo SHA256 -u [KEY ID] --output testData.txt.asc --detach-sig --armor testData.txt
        try {
            PGPSignature signature = getPGPSignature("testData.txt.asc");
            String keyId = Integer.toHexString((int) signature.getKeyID()).toUpperCase(Locale.ROOT);
            assertEquals("387C8307", keyId);
        } catch (Exception e) {
            log.error(e.toString());
            fail();
        }
    }

    @Test
    public void testIsSignatureValid() {
        try {
            PGPSignature signature = getPGPSignature("testData.txt.asc");
            PGPPublicKeyRing pgpPublicKeyRing = getPGPPublicKeyRing("387C8307.asc");
            Path dataPath = getDataAsFilePath("testData.txt");
            boolean isValid = PgPUtils.isSignatureValid(signature, pgpPublicKeyRing.getPublicKey(), dataPath);
            assertTrue(isValid);
        } catch (Exception e) {
            log.error(e.toString());
            fail();
        }
    }

    private PGPPublicKeyRing getPGPPublicKeyRing(String fileName) throws IOException, PGPException {
        Path filePath = Paths.get("temp", fileName);
        try {
            try (InputStream resource = FileReaderUtils.getResourceAsStream(fileName);
                 OutputStream out = FileMutatorUtils.newOutputStream(filePath)) {
                FileMutatorUtils.copy(resource, out);
            }
            return PgPUtils.readPgpPublicKeyRing(filePath);
        } finally {
            filePath.toFile().deleteOnExit();
        }
    }

    private PGPSignature getPGPSignature(String fileName) throws IOException, SignatureException {
        Path filePath = Paths.get("temp", fileName);
        try {
            try (InputStream resource = FileReaderUtils.getResourceAsStream(fileName);
                 OutputStream out = FileMutatorUtils.newOutputStream(filePath)) {
                FileMutatorUtils.copy(resource, out);
            }
            return PgPUtils.readPgpSignature(filePath);
        } finally {
            filePath.toFile().deleteOnExit();
        }
    }

    private Path getDataAsFilePath(String fileName) throws IOException {
        Path filePath = Paths.get("temp", fileName);
        try {
            try (InputStream resource = FileReaderUtils.getResourceAsStream(fileName)) {
                OutputStream out = FileMutatorUtils.newOutputStream(filePath);
                FileMutatorUtils.copy(resource, out);
            }
            return filePath;
        } finally {
            filePath.toFile().deleteOnExit();
        }
    }
}
