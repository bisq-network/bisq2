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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Main {
    private static final String PASSPHRASE = "my_passphrase";

    public static void main(String[] args) throws IOException, InterruptedException {
        Args parsedArgs = parseArgs(args);
        if (!parsedArgs.isStartCommand()) {
            System.out.println("--start argument is missing.");
        }

        Path dataDirPath = parsedArgs.dataDirPath();
        new TorNetwork(dataDirPath)
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

    private static Args parseArgs(String[] allArgs) {
        Optional<Path> dataDirPath = Optional.empty();
        boolean isStartCommand = false;

        for (int i = 0; i < allArgs.length; i++) {
            switch (allArgs[i]) {
                case "--dataDir":
                    dataDirPath = Optional.of(
                            Paths.get(allArgs[i + 1])
                    );
                    break;

                case "--start":
                    isStartCommand = true;
                    break;

            }
        }

        return new Args(dataDirPath.orElseThrow(), isStartCommand);
    }

    private record Args(Path dataDirPath, boolean isStartCommand) {
    }
}
