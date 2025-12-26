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

package bisq.network.tor.installer;

import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class TorInstaller {
    private static final String VERSION = "0.1.0";
    private final Path torDirPath;
    private final Path versionFilePath;

    public TorInstaller(Path torDirPath) {
        this.torDirPath = torDirPath;
        this.versionFilePath = this.torDirPath.resolve("version");
    }

    public void installIfNotUpToDate() {
        try {
            if (!isTorUpToDate()) {
                install();
            }
        } catch (IOException e) {
            throw new CannotInstallBundledTor(e);
        }
    }

    public void deleteVersionFile() {
        try {
            Files.deleteIfExists(versionFilePath);
            log.debug("Deleted {}", versionFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Couldn't delete version file {}", versionFilePath.toAbsolutePath(), e);
        }
    }

    private boolean isTorUpToDate() throws IOException {
        if (!Files.exists(versionFilePath)) {
            return false;
        }
        String torVersion = FacadeProvider.getJdkFacade().readString(versionFilePath);
        return VERSION.equals(torVersion);
    }

    private void install() throws IOException {
        try {
            new TorBinaryZipExtractor(torDirPath).extractBinary();
            log.info("Tor files installed to {}", torDirPath.toAbsolutePath());
            // Only if we have successfully extracted all files we write our version file which is used to
            // check if we need to call installFiles.
            FacadeProvider.getJdkFacade().writeString(VERSION, versionFilePath);
        } catch (IOException e) {
            deleteVersionFile();
            throw e;
        }
    }
}
