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

package bisq.updater;

import bisq.common.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class UpdaterUtils {
    // For testing, we can use bisq1 url
    public static final String RELEASES_URL = "https://github.com/bisq-network/bisq/releases/tag/v";
    public static final String GITHUB_DOWNLOAD_URL = "https://github.com/bisq-network/bisq/releases/download/v";
    // public static final String RELEASES_URL = "https://github.com/bisq-network/bisq2/releases/tag/v";
    // public static final String GITHUB_DOWNLOAD_URL = "https://github.com/bisq-network/bisq2/releases/download/v";
    public static final String PUB_KEYS_URL = "https://bisq.network/pubkey/";
    public static final String FROM_BISQ_WEBPAGE_PREFIX = "from_bisq_webpage_";
    public static final String SIGNING_KEY_FILE = "signingkey.asc";
    public static final String KEY_4A133008 = "4A133008";
    public static final String KEY_E222AA02 = "E222AA02";
    public static final String VERSION_FILE_NAME = "version.txt";
    public static final String DESTINATION_DIR = "jar";
    public static final String FILE_NAME = "desktop.jar";
    public static final String SIGNATURE_FILE_NAME = "desktop.jar.asc";
    public static final String EXTENSION = ".asc";

    public static String getSourceFileName(String version) {
        // for testing, we can use bisq 1 installer file
        return "Bisq-" + version + ".dmg";
        // return "desktop_app-" + version + "-all.jar";
    }

    public static String getSigningKeyId(String directory) throws IOException {
        return FileUtils.readStringFromFile(Path.of(directory, SIGNING_KEY_FILE).toFile());
    }

    public static String getSigningKey(String directory, String signingKeyId) throws IOException {
        return FileUtils.readStringFromFile(Path.of(directory, signingKeyId + EXTENSION).toFile());
    }

    public static Optional<String> readVersionFromVersionFile(String userDataDir) {
        String versionFilePath = userDataDir + File.separator + VERSION_FILE_NAME;
        return FileUtils.readFromFileIfPresent(new File(versionFilePath));
    }

    public static boolean hasKeyInResources(String keyId) {
        return FileUtils.hasResourceFile("keys/" + keyId + EXTENSION);
    }
}