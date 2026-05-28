/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.evolution.updater;

import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static bisq.evolution.updater.UpdaterUtils.ASC_EXTENSION;
import static bisq.evolution.updater.UpdaterUtils.FROM_BISQ_WEBPAGE_PREFIX;
import static bisq.evolution.updater.UpdaterUtils.SIGNING_KEY_FILE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DownloadedFilesVerificationTest {
    private static final String KEY_ID = "387C8307";
    private static final String OTHER_KEY_ID = "E222AA02";
    private static final String DATA_FILE_NAME = "desktop_app-1.9.10-all.jar";
    private static final String TEST_RELEASE_RESOURCE_DIR = "1.9.10/";

    @Test
    void verifyAcceptsEquivalentKeysWithDifferentArmorMetadata(@TempDir Path tempDirPath) throws IOException {
        writeValidDownloadFiles(tempDirPath);

        String resourceKey = readResourceAsString("keys/" + KEY_ID + ASC_EXTENSION);
        String releaseKey = withArmorComment(readResourceAsString(TEST_RELEASE_RESOURCE_DIR + KEY_ID + ASC_EXTENSION),
                "release key armor metadata differs");
        String webpageKey = withArmorComment(readResourceAsString(TEST_RELEASE_RESOURCE_DIR + KEY_ID + ASC_EXTENSION),
                "webpage key armor metadata differs");
        FileMutatorUtils.writeToPath(releaseKey, tempDirPath.resolve(KEY_ID + ASC_EXTENSION));
        FileMutatorUtils.writeToPath(webpageKey, tempDirPath.resolve(FROM_BISQ_WEBPAGE_PREFIX + KEY_ID + ASC_EXTENSION));

        assertNotEquals(resourceKey, releaseKey);
        assertNotEquals(resourceKey, webpageKey);

        assertDoesNotThrow(() -> DownloadedFilesVerification.verify(tempDirPath, DATA_FILE_NAME, List.of(KEY_ID), false));
    }

    @Test
    void verifyFailsWhenAnyKeySourceCannotVerifyArtifact(@TempDir Path tempDirPath) throws IOException {
        writeValidDownloadFiles(tempDirPath);
        copyResource(TEST_RELEASE_RESOURCE_DIR + KEY_ID + ASC_EXTENSION, tempDirPath.resolve(KEY_ID + ASC_EXTENSION));
        copyResource("keys/" + OTHER_KEY_ID + ASC_EXTENSION,
                tempDirPath.resolve(FROM_BISQ_WEBPAGE_PREFIX + KEY_ID + ASC_EXTENSION));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DownloadedFilesVerification.verify(tempDirPath, DATA_FILE_NAME, List.of(KEY_ID), false));

        assertTrue(exception.getMessage().contains(FROM_BISQ_WEBPAGE_PREFIX + KEY_ID + ASC_EXTENSION));
    }

    private static void writeValidDownloadFiles(Path tempDirPath) throws IOException {
        FileMutatorUtils.writeToPath(KEY_ID, tempDirPath.resolve(SIGNING_KEY_FILE));
        copyResource(TEST_RELEASE_RESOURCE_DIR + DATA_FILE_NAME, tempDirPath.resolve(DATA_FILE_NAME));
        copyResource(TEST_RELEASE_RESOURCE_DIR + DATA_FILE_NAME + ASC_EXTENSION, tempDirPath.resolve(DATA_FILE_NAME + ASC_EXTENSION));
    }

    private static void copyResource(String resourceName, Path destinationPath) throws IOException {
        try (InputStream inputStream = FileReaderUtils.getResourceAsStream(resourceName)) {
            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String readResourceAsString(String resourceName) throws IOException {
        try (InputStream inputStream = FileReaderUtils.getResourceAsStream(resourceName)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String withArmorComment(String publicKey, String comment) {
        return publicKey.replaceFirst("-----BEGIN PGP PUBLIC KEY BLOCK-----\\R",
                "-----BEGIN PGP PUBLIC KEY BLOCK-----\nComment: " + comment + "\n");
    }
}
