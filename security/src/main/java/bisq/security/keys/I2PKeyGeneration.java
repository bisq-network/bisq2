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

package bisq.security.keys;

import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PException;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.crypto.SigType;
import net.i2p.data.Destination;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class I2PKeyGeneration {
    public static Path getDestinationFilePath(Path storageDir, String tag, String suffix) {
        Path targetDir = storageDir.resolve(tag);
        return targetDir.resolve(suffix);
    }

    public static I2PKeyPair generateKeyPair() {
        try (ByteArrayOutputStream identityBytesStream = new ByteArrayOutputStream()) {
            I2PClient client = I2PClientFactory.createClient();
            Destination destination = client.createDestination(identityBytesStream, SigType.EdDSA_SHA512_Ed25519);
            return new I2PKeyPair(identityBytesStream.toByteArray(), destination);
        } catch (I2PException | IOException e) {
            throw new RuntimeException("Failed to generate new I2P Destination", e);
        }
    }
}
