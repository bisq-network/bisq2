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

package bisq.tor.installer;

import bisq.common.util.FileUtils;
import bisq.tor.Constants;
import bisq.tor.OsType;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;

@Slf4j
public class TorrcConfigInstaller {
    private final TorInstallationFiles torInstallationFiles;
    private final File torrcFile;
    private final OsType osType;

    public TorrcConfigInstaller(TorInstallationFiles torInstallationFiles) {
        this.torInstallationFiles = torInstallationFiles;
        this.torrcFile = torInstallationFiles.getTorrcFile();
        this.osType = OsType.getOsType();
    }

    public void install() throws IOException {
        FileUtils.resourceToFile(torrcFile);
        extendTorrcFile();
    }

    public void addBridgesToTorrcFile(List<String> bridgeConfig) throws IOException {
        // We overwrite old file as it might contain diff. bridges
        install();

        try (FileWriter fileWriter = new FileWriter(torrcFile, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
             PrintWriter printWriter = new PrintWriter(bufferedWriter)) {
            if (!bridgeConfig.isEmpty()) {
                printWriter.println("");
                printWriter.println("UseBridges 1");
            }
            bridgeConfig.forEach(entry -> printWriter.println("Bridge " + entry));
        }
        log.info("Added bridges to torrc");
    }

    private void extendTorrcFile() throws IOException {
        try (FileWriter fileWriter = new FileWriter(torrcFile, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
             PrintWriter printWriter = new PrintWriter(bufferedWriter)) {

            // Defaults are from resources
            printWriter.println("");
            FileUtils.appendFromResource(printWriter, FileUtils.FILE_SEP + Constants.TORRC_DEFAULTS);
            printWriter.println("");
            FileUtils.appendFromResource(printWriter, osType.getTorrcNative());

            // Update with our newly created files
            printWriter.println("");
            printWriter.println(Constants.TORRC_KEY_DATA_DIRECTORY + " " +
                    torInstallationFiles.getTorDir().getCanonicalPath());
            printWriter.println(Constants.TORRC_KEY_GEOIP + " " +
                    torInstallationFiles.getGeoIPFile().getCanonicalPath());
            printWriter.println(Constants.TORRC_KEY_GEOIP6 + " " +
                    torInstallationFiles.getGeoIPv6File().getCanonicalPath());
            printWriter.println(Constants.TORRC_KEY_PID + " " +
                    torInstallationFiles.getPidFile().getCanonicalPath());
            printWriter.println("");
        }
    }
}
