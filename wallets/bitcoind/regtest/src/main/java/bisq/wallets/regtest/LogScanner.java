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

import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class LogScanner {

    private final Set<String> linesToMatch;
    private final Set<String> matchedLines = new HashSet<>();

    public LogScanner(Set<String> linesToMatch) {
        this.linesToMatch = linesToMatch;
    }

    public abstract boolean waitUntilLogContainsLines() throws IOException, ExecutionException, InterruptedException, TimeoutException;

    protected boolean waitUntilScannerContainsLines(Scanner scanner) {
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

        return false;
    }
}
