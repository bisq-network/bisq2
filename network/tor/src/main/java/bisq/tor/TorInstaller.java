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

package bisq.tor;

import bisq.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class TorInstaller {
    private final TorInstallationFiles torInstallationFiles;
    private final TorrcConfigInstaller torrcConfigInstaller;

    public TorInstaller(TorInstallationFiles torInstallationFiles, TorrcConfigInstaller torrcConfigInstaller) {
        this.torInstallationFiles = torInstallationFiles;
        this.torrcConfigInstaller = torrcConfigInstaller;
    }

    public void installIfNotUpToDate() throws IOException {
        if (!isTorUpToDate()) {
            install();
        }
    }

    private boolean isTorUpToDate() throws IOException {
        File versionFile = torInstallationFiles.getVersionFile();
        return versionFile.exists() && Tor.VERSION.equals(FileUtils.readFromFile(versionFile));
    }
    
    private void install() throws IOException {
        try {
            File torDir = torInstallationFiles.getTorDir();
            FileUtils.makeDirs(torDir);

            File dotTorDir = torInstallationFiles.getDotTorDir();
            FileUtils.makeDirs(dotTorDir);

            torrcConfigInstaller.install();

            File destDir = torInstallationFiles.getTorDir();
            new TorBinaryZipExtractor(destDir).extractBinary();
            log.info("Tor files installed to {}", destDir.getAbsolutePath());
            // Only if we have successfully extracted all files we write our version file which is used to
            // check if we need to call installFiles.
            File versionFile = torInstallationFiles.getVersionFile();
            FileUtils.writeToFile(Tor.VERSION, versionFile);
        } catch (Throwable e) {
            deleteVersionFile();
            throw e;
        }
    }

    void deleteVersionFile() {
        File versionFile = torInstallationFiles.getVersionFile();
        boolean isSuccess = versionFile.delete();
        if (isSuccess) {
            log.debug("Deleted {}", versionFile.getAbsolutePath());
        }
    }
}
