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

import net.i2p.I2PException;
import net.i2p.client.I2PClientFactory;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.crypto.SigType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

@Slf4j
public class I2PKeyGeneration {

    public static I2PKeyPair generateKeyPair(Path storageDir, String tag) throws IOException, GeneralSecurityException {

        Path targetDir = storageDir.resolve(tag);
        Path destFilePath = targetDir.resolve("destination_b64");

        if (Files.exists(destFilePath)) {
            byte[] localDestinationKey = null;
            try (FileReader fileReader = new FileReader(destFilePath.toFile())) {
                char[] destKeyBuffer = new char[(int) destFilePath.toFile().length()];
                fileReader.read(destKeyBuffer);
                localDestinationKey = net.i2p.data.Base64.decode(new String(destKeyBuffer));
                Destination dest = rebuildDestinationFromBytes(localDestinationKey);
                return new I2PKeyPair(localDestinationKey);
            } catch (IOException | DataFormatException e) {
                log.warn("Failed to load existing I2P destination from {}: {}.  Will generate fresh.", destFilePath, e.getMessage());
            }
        }

        byte[] freshDest = generateFreshDestination();
        return new I2PKeyPair(freshDest);
    }

    /**
     * Given raw Destination-bytes (the output of Destination.toByteArray()),
     * reconstruct a Destination object.
     *
     * @param destBytes Byte array from I2P’s createDestination.
     * @return a fully-formed Destination
     * @throws IOException If the InputStream fails
     * @throws DataFormatException If the bytes are not a valid Destination
     */
    private static Destination rebuildDestinationFromBytes(byte[] destBytes) throws IOException, DataFormatException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(destBytes)) {
            return Destination.create(in);
        }
    }

    /**
     * Calls I2PClientFactory to build a brand-new Destination (ECDSA_SHA512_P521).
     * We return it to the caller so they can choose to persist or not.
     *
     * @return a fresh Destination
     * @throws GeneralSecurityException if any I2PException or parse error occurs
     */
    private static byte[] generateFreshDestination() throws GeneralSecurityException {
        ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
        try {
            I2PClientFactory.createClient().createDestination(arrayStream, SigType.ECDSA_SHA512_P521);
        } catch (I2PException | IOException e) {
            throw new GeneralSecurityException("Failed to generate new I2P Destination", e);
        }

        return arrayStream.toByteArray();
    }
}
