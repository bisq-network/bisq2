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

package bisq.wallets.process.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public abstract class AbstractRpcCliProcess {
    private final CliProcessConfig cliProcessConfig;

    public AbstractRpcCliProcess(CliProcessConfig cliProcessConfig) {
        this.cliProcessConfig = cliProcessConfig;
    }

    protected String runAndGetOutput(String... args) {
        Process process = runCliProcess(args);
        return readProcessOutput(process);
    }

    private Process runCliProcess(String... args) {
        try {
            List<String> allArgs = createArgsList(args);
            Process process = new ProcessBuilder(allArgs).start();
            process.waitFor();

            if (process.exitValue() != 0) {
                throw new IOException();
            }

            return process;
        } catch (IOException | InterruptedException e) {
            String errorMessage = String.format(
                    "`%s %s %s` did not succeed.",
                    cliProcessConfig.getBinaryName(),
                    cliProcessConfig.getDefaultArgs(),
                    String.join(" ", args)
            );
            throw new CliCommandFailedException(errorMessage, e);
        }
    }

    private String readProcessOutput(Process process) {
        StringBuilder stringBuilder = new StringBuilder();

        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }

    private List<String> createArgsList(String... args) {
        List<String> allArgs = new ArrayList<>();
        allArgs.add(cliProcessConfig.getBinaryName());
        allArgs.addAll(cliProcessConfig.getDefaultArgs());
        allArgs.addAll(Arrays.asList(args));
        return allArgs;
    }
}
