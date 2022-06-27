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

package bisq.wallets.regtest.process;

import bisq.common.util.FileUtils;
import bisq.wallets.core.exceptions.WalletShutdownFailedException;
import bisq.wallets.core.exceptions.WalletStartupFailedException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public abstract class DaemonProcess implements BisqProcess {

    @Getter
    protected final Path dataDir;
    private Process process;

    public DaemonProcess(Path dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    public void start() throws IOException, InterruptedException {
        try {
            FileUtils.makeDirs(dataDir.toFile());
            process = createAndStartProcess();
            waitUntilReady();

        } catch (IOException e) {
            throw new WalletStartupFailedException("Cannot start wallet process.", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            invokeStopRpcCall();
            process.waitFor();
        } catch (InterruptedException e) {
            String processName = process.info().command().orElse("<unknown process>");
            throw new WalletShutdownFailedException("Cannot shutdown " + processName + ".", e);
        }
    }

    private void waitUntilReady() {
        Set<String> linesToMatch = getIsSuccessfulStartUpLogLines();
        boolean isSuccess = waitUntilLogFileContainsLines(linesToMatch);

        if (!isSuccess) {
            String processName = process.info().command().orElse("<unknown process>");
            throw new WalletStartupFailedException("Cannot start wallet process." + processName, null);
        }
    }

    protected abstract Set<String> getIsSuccessfulStartUpLogLines();

    public abstract void invokeStopRpcCall();

    private Process createAndStartProcess() throws IOException {
        ProcessConfig processConfig = createProcessConfig();
        List<String> args = processConfig.toCommandList();

        var processBuilder = new ProcessBuilder(args);
        processBuilder.redirectErrorStream(true);

        Map<String, String> environment = processBuilder.environment();
        environment.putAll(processConfig.getEnvironmentVars());

        return processBuilder.start();
    }

    public abstract ProcessConfig createProcessConfig();

    protected boolean waitUntilLogFileContainsLines(Set<String> linesToMatch) {
        Set<String> matchedLines = new HashSet<>();

        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                for (String lineToMatch : linesToMatch) {
                    if (line.contains(lineToMatch)) {
                        matchedLines.add(lineToMatch);

                        if (matchedLines.size() == linesToMatch.size()) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            String processName = process.info().command().orElse("<unknown process>");
            log.error(processName + " didn't start correctly.", e);
            throw e;
        }
        return false;
    }
}
