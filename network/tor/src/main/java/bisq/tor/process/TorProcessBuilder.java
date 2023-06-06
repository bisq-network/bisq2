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

package bisq.tor.process;

import bisq.tor.OsType;
import bisq.tor.TorInstallationFiles;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class TorProcessBuilder {
    private static final String ARG_TORRC = "-f";
    private static final String ARG_OWNER_PID = "__OwningControllerProcess";

    private final TorProcessConfig torProcessConfig;
    private final TorInstallationFiles torInstallationFiles;
    private final OsType osType;

    public TorProcessBuilder(TorProcessConfig torProcessConfig, TorInstallationFiles torInstallationFiles, OsType osType) {
        this.torProcessConfig = torProcessConfig;
        this.torInstallationFiles = torInstallationFiles;
        this.osType = osType;
    }

    public Process createAndStartProcess() throws IOException {
        File torDir = torInstallationFiles.getTorDir();
        String torBinaryPath = new File(torDir, osType.getBinaryName()).getAbsolutePath();

        log.debug("command for process builder: {} {} {} {} {}",
                torBinaryPath, ARG_TORRC, torProcessConfig.getTorrcPath(), ARG_OWNER_PID, torProcessConfig.getOwnerPid());

        ProcessBuilder processBuilder = new ProcessBuilder(
                torBinaryPath,
                ARG_TORRC, torProcessConfig.getTorrcPath(),
                ARG_OWNER_PID, torProcessConfig.getOwnerPid()
        );

        processBuilder.directory(torDir);

        Map<String, String> environment = processBuilder.environment();
        environment.put("HOME", torDir.getAbsolutePath());

        if (osType == OsType.LINUX_32 || osType == OsType.LINUX_64) {
            forceLinkerToLoadLibrariesFromTorBundle(environment);
        }

        Process process = processBuilder.start();
        log.debug("Process started. pid={} info={}", process.pid(), process.info());
        return process;
    }

    private void forceLinkerToLoadLibrariesFromTorBundle(Map<String, String> environment) {
        File torDir = torInstallationFiles.getTorDir();
        environment.put("LD_LIBRARY_PATH", torDir.getAbsolutePath());
    }
}
