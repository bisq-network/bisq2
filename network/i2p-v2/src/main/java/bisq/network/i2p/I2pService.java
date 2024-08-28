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

package bisq.network.i2p;

import lombok.extern.slf4j.Slf4j;
import net.i2p.client.I2PSession;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.DataFormatException;
import net.i2p.data.PrivateKeyFile;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class I2pService {
    private final Path privateKeyPath;
    private final Consumer<I2PSocket> clientHandler;
    private Optional<I2pServer> i2pServer = Optional.empty();

    public I2pService(Path privateKeyPath, Consumer<I2PSocket> clientHandler) {
        this.privateKeyPath = privateKeyPath;
        this.clientHandler = clientHandler;
    }

    public void initialize() throws IOException, DataFormatException {
        I2PSocketManager manager = createI2pSocketManager();
        I2PServerSocket serverSocket = manager.getServerSocket();

        I2pServer i2pServer = new I2pServer(serverSocket, clientHandler);
        this.i2pServer = Optional.of(i2pServer);
        i2pServer.start();
    }

    public void shutdown() {
        i2pServer.ifPresent(I2pServer::shutdown);
    }

    private I2PSocketManager createI2pSocketManager() throws IOException, DataFormatException {
        Optional<InputStream> optionalPrivateKeyStream = createOptionalPrivateKeyStream();
        boolean usingExistingKey = optionalPrivateKeyStream.isPresent();

        I2PSocketManager manager;
        if (usingExistingKey) {
            try (InputStream privateKeyStream = optionalPrivateKeyStream.get()) {
                manager = I2PSocketManagerFactory.createManager(privateKeyStream);
            }
        } else {
            manager = I2PSocketManagerFactory.createManager();
            writePrivateKeyToDisk(manager.getSession());
        }

        I2PSession session = manager.getSession();
        String myDestination = getMyDestinationFromSession(session);
        if (usingExistingKey) {
            log.info("Created I2P server with existing key: {}", myDestination);
        } else {
            log.info("Created I2P server with existing key: {}", myDestination);
        }

        return manager;
    }

    private Optional<InputStream> createOptionalPrivateKeyStream() {
        InputStream fileInputStream = null;
        try {
            File privateKeyFile = privateKeyPath.toFile();
            fileInputStream = new FileInputStream(privateKeyFile);
        } catch (FileNotFoundException e) {
            // Nothing to do if file not found.
        }
        return Optional.ofNullable(fileInputStream);
    }

    private void writePrivateKeyToDisk(I2PSession session) throws DataFormatException, IOException {
        File privateKeyFile = privateKeyPath.toFile();
        var i2pPrivateKeyFile = new PrivateKeyFile(privateKeyFile, session);
        i2pPrivateKeyFile.write();
    }

    private String getMyDestinationFromSession(I2PSession session) {
        return session.getMyDestination().toBase64();
    }
}
