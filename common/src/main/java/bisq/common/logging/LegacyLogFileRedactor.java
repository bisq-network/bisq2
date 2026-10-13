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

import bisq.common.file.FileMutatorUtils;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Hides the secrets of {@link LegacyLogSecrets} in the log files which earlier versions wrote into the data directory:
 * the log file and the rolled log files. The node runs it once per data directory, before the logger opens the files
 * (see docs/specifications/security/secrets-in-logs.md).
 */
public final class LegacyLogFileRedactor {
    // Written after a run in which no file failed, so a failed file is tried again at the next start
    static final String MARKER_FILE_NAME = "legacy_log_secrets_hidden";
    private static final int CHUNK_SIZE = 64 * 1024;

    public record Result(List<Path> redactedFiles, List<Path> failedFiles) {
    }

    private LegacyLogFileRedactor() {
    }

    /**
     * Never throws: a failure must not stop the start of the node. It is reported in the result.
     *
     * @param baseName The base name of the log files, for example "bisq" for bisq.log and bisq_1.log
     */
    public static Result redact(Path dataDir, String baseName) {
        List<Path> redactedFiles = new ArrayList<>();
        List<Path> failedFiles = new ArrayList<>();
        try {
            Path markerFile = dataDir.resolve(MARKER_FILE_NAME);
            if (Files.exists(markerFile) || !Files.isDirectory(dataDir)) {
                return new Result(List.of(), List.of());
            }
            Pattern logFileName = Pattern.compile(Pattern.quote(baseName) + "(_\\d+)?\\.log");
            List<Path> logFiles;
            try (Stream<Path> files = Files.list(dataDir)) {
                logFiles = files
                        .filter(path -> logFileName.matcher(path.getFileName().toString()).matches())
                        .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .sorted()
                        .toList();
            }
            for (Path logFile : logFiles) {
                try {
                    if (redactFile(logFile)) {
                        redactedFiles.add(logFile);
                    }
                } catch (IOException | RuntimeException e) {
                    failedFiles.add(logFile);
                }
            }
            if (failedFiles.isEmpty()) {
                FileMutatorUtils.writeToPath("The onion private keys which earlier versions logged are hidden " +
                        "in the log files of this directory.", markerFile);
            }
        } catch (IOException | RuntimeException e) {
            failedFiles.add(dataDir);
        }
        return new Result(redactedFiles, failedFiles);
    }

    // The node writes a new file and replaces the old one with it, so a failure leaves the old file complete. A file
    // without secrets stays unchanged. Returns whether the file contained a secret.
    private static boolean redactFile(Path logFile) throws IOException {
        Path tempFile = logFile.resolveSibling(logFile.getFileName() + ".redacting");
        try {
            // A file or a symbolic link which is left at the temporary path is removed first. So the writer creates a
            // new file and does not follow a link, as the log files themselves are not followed.
            Files.deleteIfExists(tempFile);
            boolean changed;
            try (Reader reader = Files.newBufferedReader(logFile, StandardCharsets.ISO_8859_1);
                 Writer writer = new OutputStreamWriter(FileMutatorUtils.newOutputStream(tempFile),
                         StandardCharsets.ISO_8859_1)) {
                changed = copyRedacted(reader, writer);
            }
            if (changed) {
                FileMutatorUtils.replaceFile(tempFile, logFile);
            }
            return changed;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // ISO-8859-1 maps each byte to one character and back, so all other bytes stay unchanged, also UTF-8 sequences. A
    // chunk ends at its last line break, and a key never contains a line break, so no key spans two chunks.
    @VisibleForTesting
    static boolean copyRedacted(Reader reader, Writer writer) throws IOException {
        boolean changed = false;
        StringBuilder pending = new StringBuilder();
        char[] buffer = new char[CHUNK_SIZE];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            pending.append(buffer, 0, read);
            int end = pending.lastIndexOf("\n") + 1;
            if (end > 0) {
                changed |= writeRedacted(pending.substring(0, end), writer);
                pending.delete(0, end);
            }
        }
        changed |= writeRedacted(pending.toString(), writer);
        return changed;
    }

    private static boolean writeRedacted(String text, Writer writer) throws IOException {
        String redacted = LegacyLogSecrets.redact(text);
        writer.write(redacted);
        return !redacted.equals(text);
    }
}
