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
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class TorBootstrap {
    private final Path torDirPath;
    private final TorInstallationFileManager torInstallationFileManager;
    private final TorrcConfigInstaller torrcConfigInstaller;
    private final OsType osType;

    private volatile boolean isStopped;

    TorBootstrap(Path torDirPath) {
        this.torDirPath = torDirPath;
        this.torInstallationFileManager = new TorInstallationFileManager(torDirPath);
        this.torrcConfigInstaller = new TorrcConfigInstaller(torInstallationFileManager);
        this.osType = OsType.getOsType();
    }

    int start() throws IOException, InterruptedException {
        maybeCleanupCookieFile();

        if (!torInstallationFileManager.isTorUpToDate()) {
            installFiles();
        }

        Process torProcess = startTorProcess();
        log.info("Tor process started");

        int controlPort = waitForControlPort(torProcess);
        terminateProcessBuilder(torProcess);

        waitForCookieInitialized();
        log.info("Cookie initialized");
        return controlPort;
    }

    File getCookieFile() {
        return torInstallationFileManager.getCookieFile();
    }

    void deleteVersionFile() {
        File versionFile = torInstallationFileManager.getVersionFile();
        versionFile.delete();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void maybeCleanupCookieFile() throws IOException {
        File cookieFile = torInstallationFileManager.getCookieFile();
        if (cookieFile.exists() && !cookieFile.delete()) {
            throw new IOException("Cannot delete old cookie file.");
        }
    }

    private void installFiles() throws IOException {
        try {
            File torDir = torInstallationFileManager.getTorDir();
            FileUtils.makeDirs(torDir);

            File dotTorDir = torInstallationFileManager.getDotTorDir();
            FileUtils.makeDirs(dotTorDir);

            torrcConfigInstaller.install();

            File destDir = torDirPath.toFile();
            new TorBinaryZipExtractor(destDir).extractBinary();
            log.info("Tor files installed to {}", torDirPath);
            // Only if we have successfully extracted all files we write our version file which is used to
            // check if we need to call installFiles.
            File versionFile = torInstallationFileManager.getVersionFile();
            FileUtils.writeToFile(Tor.VERSION, versionFile);
        } catch (Throwable e) {
            deleteVersionFile();
            throw e;
        }
    }

    private Process startTorProcess() throws IOException {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        String ownerPid = processName.split("@")[0];
        log.debug("Owner pid {}", ownerPid);
        File pidFile = torInstallationFileManager.getPidFile();
        FileUtils.writeToFile(ownerPid, pidFile);

        File torDir = torInstallationFileManager.getTorDir();
        String path = new File(torDir, osType.getBinaryName()).getAbsolutePath();
        File torrcFile = torInstallationFileManager.getTorrcFile();
        String[] command = {path, "-f", torrcFile.getAbsolutePath(), Constants.CONTROL_RESET_CONF, ownerPid};
        log.debug("command for process builder: {} {} {} {} {}",
                path, "-f", torrcFile.getAbsolutePath(), Constants.CONTROL_RESET_CONF, ownerPid);
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.directory(torDir);
        Map<String, String> environment = processBuilder.environment();
        environment.put("HOME", torDir.getAbsolutePath());
        if (osType == OsType.LINUX_32 || osType == OsType.LINUX_64) {
            // TODO Taken from netlayer, but not sure if needed. Not used in Briar.
            // Not recommended to be used here: https://www.hpc.dtu.dk/?page_id=1180
            environment.put("LD_LIBRARY_PATH", torDir.getAbsolutePath());
        }

        Process process = processBuilder.start();
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

    private void waitForCookieInitialized() throws InterruptedException, IOException {
        long start = System.currentTimeMillis();
        File cookieFile = torInstallationFileManager.getCookieFile();
        while (!isStopped && cookieFile.length() < 32 && !Thread.currentThread().isInterrupted()) {
            if (System.currentTimeMillis() - start > 5000) {
                throw new IOException("Auth cookie not created");
            }
            Thread.sleep(50);
        }
    }

    void shutdown() {
        isStopped = true;
    }
}
