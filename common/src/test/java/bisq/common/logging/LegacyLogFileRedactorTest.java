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

package bisq.common.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Versions 2.1.3 to 2.1.13 wrote the onion private keys into the log files of the data directory. A node hides them
 * once, before the logger opens the files (see docs/specifications/security/secrets-in-logs.md).
 */
class LegacyLogFileRedactorTest {
    private static final String KEY = "a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8s9T0u1V2w3X4y5Z6a7B8c9D0e1F2g3H4i5J6k7L8m9N0+/==";
    private static final String KEY_LINE = "Oct-01 10:00:00.000 [Tor] INFO  b.n.t.c.TorControlProtocol: " +
            "Send Tor control command: ADD_ONION ED25519-V3:" + KEY + " Port=9999,127.0.0.1:1234\n";
    private static final String HIDDEN_LINE = "Oct-01 10:00:00.000 [Tor] INFO  b.n.t.c.TorControlProtocol: " +
            "Send Tor control command: ADD_ONION ED25519-V3:[hidden] Port=9999,127.0.0.1:1234\n";

    @TempDir
    Path dataDir;

    @Test
    void keysAreHiddenInTheLogFileAndTheRolledLogFiles() throws IOException {
        // A UTF-8 character and a line end of Windows must keep their bytes
        String otherLines = "Line with \u00e9 and \u20ac\r\nNEW:ED25519-V3 without key ADD_ONION NEW:ED25519-V3\n";
        for (String name : List.of("bisq.log", "bisq_1.log", "bisq_20.log")) {
            write(name, otherLines + KEY_LINE + otherLines);
        }

        LegacyLogFileRedactor.Result result = LegacyLogFileRedactor.redact(dataDir, "bisq");

        assertEquals(3, result.redactedFiles().size());
        assertTrue(result.failedFiles().isEmpty());
        for (String name : List.of("bisq.log", "bisq_1.log", "bisq_20.log")) {
            assertArrayEquals((otherLines + HIDDEN_LINE + otherLines).getBytes(StandardCharsets.UTF_8),
                    Files.readAllBytes(dataDir.resolve(name)));
        }
        assertTrue(Files.exists(dataDir.resolve(LegacyLogFileRedactor.MARKER_FILE_NAME)));
    }

    @Test
    void keyAtTheBorderOfTwoChunksIsHidden() throws IOException {
        String filler = "x".repeat(64 * 1024 - 40) + "\n";
        StringWriter writer = new StringWriter();

        assertTrue(LegacyLogFileRedactor.copyRedacted(new StringReader(filler + KEY_LINE), writer));

        assertEquals(filler + HIDDEN_LINE, writer.toString());
    }

    @Test
    void fileWithoutKeyIsNotRewritten() throws IOException {
        write("bisq.log", "no secret here\n");
        FileTime lastModified = FileTime.fromMillis(1_000_000_000_000L);
        Files.setLastModifiedTime(dataDir.resolve("bisq.log"), lastModified);

        LegacyLogFileRedactor.Result result = LegacyLogFileRedactor.redact(dataDir, "bisq");

        assertTrue(result.redactedFiles().isEmpty());
        assertEquals(lastModified, Files.getLastModifiedTime(dataDir.resolve("bisq.log")));
        assertEquals("no secret here\n", Files.readString(dataDir.resolve("bisq.log")));
    }

    @Test
    void otherFilesStayUnchanged() throws IOException {
        for (String name : List.of("bisq.conf", "bi2p.log", "bisq_x.log", "bisq.log.1", "other.log")) {
            write(name, KEY_LINE);
        }

        LegacyLogFileRedactor.redact(dataDir, "bisq");

        for (String name : List.of("bisq.conf", "bi2p.log", "bisq_x.log", "bisq.log.1", "other.log")) {
            assertEquals(KEY_LINE, Files.readString(dataDir.resolve(name)));
        }
    }

    @Test
    void symbolicLinkIsNotFollowed() throws IOException {
        Path target = Files.createDirectory(dataDir.resolve("elsewhere")).resolve("target.log");
        Files.writeString(target, KEY_LINE);
        try {
            Files.createSymbolicLink(dataDir.resolve("bisq.log"), target);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "Symbolic links are not supported here");
        }

        LegacyLogFileRedactor.redact(dataDir, "bisq");

        assertEquals(KEY_LINE, Files.readString(target));
    }

    @Test
    void symbolicLinkAtTheTemporaryPathIsNotFollowed() throws IOException {
        write("bisq.log", KEY_LINE);
        Path target = Files.createDirectory(dataDir.resolve("elsewhere")).resolve("target.log");
        Files.writeString(target, "unchanged\n");
        Path tempFile = dataDir.resolve("bisq.log.redacting");
        try {
            Files.createSymbolicLink(tempFile, target);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "Symbolic links are not supported here");
        }

        LegacyLogFileRedactor.Result result = LegacyLogFileRedactor.redact(dataDir, "bisq");

        assertEquals(List.of(dataDir.resolve("bisq.log")), result.redactedFiles());
        assertEquals("unchanged\n", Files.readString(target));
        assertEquals(HIDDEN_LINE, Files.readString(dataDir.resolve("bisq.log")));
        assertFalse(Files.isSymbolicLink(dataDir.resolve("bisq.log")));
        assertFalse(Files.exists(tempFile, LinkOption.NOFOLLOW_LINKS));
    }

    // A failure does not throw and does not write the marker, so the next start tries again
    @Test
    void failedFileIsTriedAgainAtTheNextStart() throws IOException {
        Path logFile = dataDir.resolve("bisq.log");
        write("bisq.log", KEY_LINE);
        // A directory where the temporary file must be
        Path tempFile = dataDir.resolve("bisq.log.redacting");
        Files.createDirectory(tempFile);
        Files.writeString(tempFile.resolve("content"), "x");

        LegacyLogFileRedactor.Result failed = LegacyLogFileRedactor.redact(dataDir, "bisq");

        assertEquals(List.of(logFile), failed.failedFiles());
        assertEquals(KEY_LINE, Files.readString(logFile));
        assertFalse(Files.exists(dataDir.resolve(LegacyLogFileRedactor.MARKER_FILE_NAME)));

        Files.delete(tempFile.resolve("content"));
        Files.delete(tempFile);
        LegacyLogFileRedactor.Result retried = LegacyLogFileRedactor.redact(dataDir, "bisq");

        assertEquals(List.of(logFile), retried.redactedFiles());
        assertEquals(HIDDEN_LINE, Files.readString(logFile));
        assertTrue(Files.exists(dataDir.resolve(LegacyLogFileRedactor.MARKER_FILE_NAME)));
    }

    @Test
    void afterARunWithoutFailureTheFilesAreNotReadAgain() throws IOException {
        LegacyLogFileRedactor.redact(dataDir, "bisq");
        // A key which an earlier version writes after a downgrade is not hidden anymore
        write("bisq.log", KEY_LINE);

        LegacyLogFileRedactor.Result result = LegacyLogFileRedactor.redact(dataDir, "bisq");

        assertTrue(result.redactedFiles().isEmpty());
        assertEquals(KEY_LINE, Files.readString(dataDir.resolve("bisq.log")));
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(dataDir.resolve(name), content, StandardCharsets.UTF_8);
    }
}
