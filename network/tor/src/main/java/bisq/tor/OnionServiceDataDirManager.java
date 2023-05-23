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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
public class OnionServiceDataDirManager {

    public static final String HOST_NAME_FILENAME = "hostname";
    public static final String PRIVATE_KEY_FILENAME = "private_key";

    private final File dataDir;
    private final File hostNameFile;
    private final File privateKeyFile;

    public OnionServiceDataDirManager(File dataDir) {
        this.dataDir = dataDir;
        this.hostNameFile = new File(dataDir, HOST_NAME_FILENAME);
        this.privateKeyFile = new File(dataDir, PRIVATE_KEY_FILENAME);
    }

    public void persist(CreateHiddenServiceResult result) {
        try {
            FileUtils.makeDirs(dataDir);
        } catch (IOException e) {
            log.error("Couldn't create onion service data dir for {}", result.getServiceId(), e);
        }

        if (!hostNameFile.exists()) {
            persistOnionAddress(result.getServiceId());
        }

        if (!privateKeyFile.exists()) {
            persistPrivateKey(result.getPrivateKey());
        }
    }

    public Optional<String> getHostName() {
        return readStringFromFile(hostNameFile);
    }

    public Optional<String> getPrivateKey() {
        return readStringFromFile(privateKeyFile);
    }

    private void persistOnionAddress(String serviceId) {
        try {
            var onionAddress = serviceId + ".onion";
            writeStringToFile(hostNameFile, onionAddress);
        } catch (IOException | NoSuchElementException e) {
            log.error("Couldn't write onion address to disk.", e);
        }
    }

    private void persistPrivateKey(String privateKey) {
        try {
            writeStringToFile(privateKeyFile, privateKey);
        } catch (IOException e) {
            log.error("Couldn't persist private key.", e);
        }
    }

    private Optional<String> readStringFromFile(File file) {
        try {
            String string = FileUtils.readFromFile(file);
            return Optional.of(string);

        } catch (FileNotFoundException e) {
            return Optional.empty();
        }
    }

    private void writeStringToFile(File file, String data) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(data);
        }
    }
}
