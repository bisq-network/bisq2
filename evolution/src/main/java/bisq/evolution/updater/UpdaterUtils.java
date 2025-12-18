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

package bisq.evolution.updater;

import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileReaderUtils;
import bisq.common.platform.Platform;
import bisq.common.platform.PlatformUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class UpdaterUtils {
    public static final String RELEASES_URL = "https://github.com/bisq-network/bisq2/releases/tag/v";
    public static final String GITHUB_DOWNLOAD_URL = "https://github.com/bisq-network/bisq2/releases/download/v";
    public static final String PUB_KEYS_URL = "https://bisq.network/pubkey/";
    public static final String FROM_BISQ_WEBPAGE_PREFIX = "from_bisq_webpage_";
    public static final String FROM_RESOURCES_PREFIX = "from_resources_";
    public static final String SIGNING_KEY_FILE = "signingkey.asc";
    public static final String VERSION_FILE_NAME = "version.txt";
    public static final String UPDATES_DIR = "updates";
    public static final String ASC_EXTENSION = ".asc";

    public static String getSigningKeyId(Path dirPath) throws IOException {
        return FacadeProvider.getJdkFacade().readString(dirPath.resolve(SIGNING_KEY_FILE));
    }

    public static String getSigningKey(Path dirPath, String signingKeyId) throws IOException {
        return FacadeProvider.getJdkFacade().readString(dirPath.resolve(signingKeyId + ASC_EXTENSION));
    }

    public static String getDownloadFileName(String version, boolean isLauncherUpdate) {
        return isLauncherUpdate ? getInstallerFileName(version) : getJarFileName(version);
    }

    public static String getInstallerFileName(String version) {
        return "Bisq-" + version + PlatformUtils.getInstallerExtension();
    }

    public static String getJarFileName(String version) {
        String platformName = Platform.getPlatform().getPlatformName();
        return "desktop-app-" + version + "-" + platformName + "-all.jar";
    }

    public static Optional<String> readVersionFromVersionFile(Path userDataDirPath) {
        Path versionFilePath = userDataDirPath.resolve(VERSION_FILE_NAME);
        return FileReaderUtils.readFromFileIfPresent(versionFilePath);
    }

    public static boolean isDownloadedFile(String fileName) {
        return fileName.endsWith("-all.jar") || fileName.endsWith(PlatformUtils.getInstallerExtension());
    }
}