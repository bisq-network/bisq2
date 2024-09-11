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

package bisq.wallets.regtest;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class FileScanner extends LogScanner {

    private final Future<Path> logFilePath;

    public FileScanner(Set<String> linesToMatch, Future<Path> logFilePath) {
        super(linesToMatch);
        this.logFilePath = logFilePath;
    }

    @Override
    public boolean waitUntilLogContainsLines() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        while (true) {
            Path logFilePath = this.logFilePath.get(1, TimeUnit.MINUTES);
            try (Scanner scanner = new Scanner(logFilePath)) {
                boolean foundAllLines = waitUntilScannerContainsLines(scanner);
                if (foundAllLines) {
                    return true;
                }
            }
        }
    }
}
