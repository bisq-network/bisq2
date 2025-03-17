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

package bisq.network.tor.local_network;

import bisq.common.data.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class Main {
    private static final String PASSPHRASE = "my_passphrase";

    public static void main(String[] args) throws IOException, InterruptedException {
        Pair<Path, Boolean> dataDirAndStartArg = parseArgs(args);
        if (!dataDirAndStartArg.getSecond()) {
            System.out.println("--start argument is missing.");
        }

        Path dataDir = dataDirAndStartArg.getFirst();
        new TorNetwork(dataDir)
                .addDirAuth(PASSPHRASE)
                .addDirAuth(PASSPHRASE)
                .addDirAuth(PASSPHRASE)

                .addRelay()
                .addRelay()
                .addRelay()
                .addRelay()
                .addRelay()

                .addClient()
                .addClient()

                .start();
    }

    private static Pair<Path, Boolean> parseArgs(String[] allArgs) {
        Optional<Path> dataDirPath = Optional.empty();
        boolean isStartCommand = false;

        for (int i = 0; i < allArgs.length; i++) {
            switch (allArgs[i]) {
                case "--dataDir":
                    dataDirPath = Optional.of(
                            Path.of(allArgs[i + 1])
                    );
                    break;

                case "--start":
                    isStartCommand = true;
                    break;

            }
        }

        return new Pair<>(dataDirPath.orElseThrow(), isStartCommand);
    }
}
