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

import bisq.tor.installer.TorInstallationFiles;
import bisq.tor.installer.TorInstaller;
import bisq.tor.installer.TorrcConfigInstaller;
import bisq.tor.process.TorProcessBuilder;
import bisq.tor.process.TorProcessConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class TorBootstrap {
    private final TorInstallationFiles torInstallationFiles;
    private final TorInstaller torInstaller;
    private final OsType osType;
    private final AtomicBoolean isRunning = new AtomicBoolean();

    TorBootstrap(Path torDirPath) {
        OsType osType = OsType.getOsType();
        this.torInstallationFiles = new TorInstallationFiles(torDirPath, osType);

        TorrcConfigInstaller torrcConfigInstaller = new TorrcConfigInstaller(torInstallationFiles);
        this.torInstaller = new TorInstaller(torInstallationFiles);

        this.osType = osType;
    }

    int start() throws IOException, InterruptedException {
        torInstaller.installIfNotUpToDate();

        Process torProcess = startTorProcess();
        isRunning.set(true);
        log.info("Tor process started");

        int controlPort = waitForControlPort(torProcess);
        terminateProcessBuilder(torProcess);

        log.info("Cookie initialized");
        return controlPort;
    }

    void deleteVersionFile() {
        torInstaller.deleteVersionFile();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Process startTorProcess() throws IOException {
        String ownerPid = Pid.getMyPid();
        log.debug("Owner pid {}", ownerPid);
        torInstallationFiles.writePidToDisk(ownerPid);

        TorProcessConfig torProcessConfig = TorProcessConfig.builder()
                .torrcFile(torInstallationFiles.getTorrcFile())
                .ownerPid(ownerPid)
                .build();

        var torProcessBuilder = new TorProcessBuilder(torProcessConfig, torInstallationFiles, osType);
        Process process = torProcessBuilder.createAndStartProcess();

        log.debug("Process started. pid={} info={}", process.pid(), process.info());
        return process;
    }

    private int waitForControlPort(Process torProcess) {
        AtomicInteger controlPort = new AtomicInteger();
        AtomicInteger scannedLines = new AtomicInteger(0);
        try (Scanner info = new Scanner(torProcess.getInputStream());
             Scanner error = new Scanner(torProcess.getErrorStream())) {
            // We get a few lines (7) of logs and filter for "Control listener listening on port" to figure  
            // out the control port and exit the while loop (there would be one more line).
            while (info.hasNextLine() || error.hasNextLine()) {
                if (info.hasNextLine()) {
                    String line = info.nextLine();
                    log.debug("Logs from control connection: >> {}", line);
                    if (line.contains(Constants.CONTROL_PORT_LOG_SUB_STRING)) {
                        String[] split = line.split(Constants.CONTROL_PORT_LOG_SUB_STRING);
                        String portString = split[1].replace(".", "");
                        controlPort.set(Integer.parseInt(portString));
                        log.info("Control connection port: {}", controlPort);
                        break;
                    }
                }
                if (error.hasNextLine()) {
                    // In case we get an error we log it and exit.
                    throw new RuntimeException("We got an error log from the tor process: " + error.nextLine());
                }
                if (scannedLines.incrementAndGet() >= 10) {
                    throw new RuntimeException("We scanned already 10 lines but did not find the control port.");
                }
            }
        }
        return controlPort.get();
    }

    private void terminateProcessBuilder(Process torProcess) throws InterruptedException, IOException {
        // TODO investigate how to handle windows case?
        if (osType != OsType.WIN) {
            int result = torProcess.waitFor();
            if (result != 0) {
                throw new IOException("Terminate processBuilder exited with an error. result=" + result);
            }
        }
        log.debug("Process builder terminated");
    }

    void shutdown() {
        isRunning.set(false);
    }
}
