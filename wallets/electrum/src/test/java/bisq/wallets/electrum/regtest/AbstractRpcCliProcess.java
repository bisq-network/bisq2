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

package bisq.wallets.electrum.regtest;

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

    protected Process runCliProcess(String... args) throws IOException, InterruptedException {
        List<String> allArgs = createArgsList(args);
        Process process = new ProcessBuilder(allArgs).start();
        process.waitFor();
        return process;
    }

    protected String readProcessOutput(Process process) {
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
        allArgs.add(cliProcessConfig.binaryName());
        allArgs.addAll(cliProcessConfig.defaultArgs());
        allArgs.addAll(Arrays.asList(args));
        return allArgs;
    }
}
