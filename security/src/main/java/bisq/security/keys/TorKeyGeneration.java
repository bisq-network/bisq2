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
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.util.encoders.Base32;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

@Slf4j
public class TorKeyGeneration {
    public static byte[] generatePrivateKey() {
        byte[] privateKey = new byte[32];
        Ed25519.generatePrivateKey(new SecureRandom(), privateKey);
        return privateKey;
    }

    public static TorKeyPair generateKeyPair() {
        byte[] privateKey = generatePrivateKey();
        return new TorKeyPair(privateKey, getPublicKey(privateKey));
    }

    private static byte[] getPublicKey(byte[] privateKey) {
        byte[] publicKey = new byte[32];
        Ed25519.generatePublicKey(privateKey, 0, publicKey, 0);
        return publicKey;
    }

  /*  public static String getOnionAddressFromPrivateKey(byte[] privateKey) {
        return getOnionAddressFromPublicKey(getPublicKey(privateKey));
    }*/

    public static String getOnionAddressFromPublicKey(byte[] publicKey) {
        byte[] checksumForAddress = computeOnionAddressChecksum(publicKey);

        ByteBuffer byteBuffer = ByteBuffer.allocate(32 + 2 + 1);
        byteBuffer.put(publicKey); // 32 bytes
        byteBuffer.put(checksumForAddress); // 2 bytes
        byteBuffer.put((byte) 3); // 1 byte

        byte[] byteArray = byteBuffer.array();
        String base32String = Base32.toBase32String(byteArray);

        return base32String.toLowerCase() + ".onion";
    }


    public static byte[] getPublicKeyFromOnionAddress(String onionAddress) {
        onionAddress = onionAddress.substring(0, onionAddress.length() - ".onion".length());
        byte[] decodedOnionAddress = Base32.decode(onionAddress.toUpperCase());
        return Arrays.copyOfRange(decodedOnionAddress, 0, 32);
    }


    private static byte[] computeOnionAddressChecksum(byte[] publicKey) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(15 + 32 + 1);
        byteBuffer.put(".onion checksum".getBytes()); // 15 bytes
        byteBuffer.put(publicKey); // 32 bytes
        byteBuffer.put((byte) 3); // 1 byte

        byte[] byteArray = byteBuffer.array();

        SHA3Digest sha3_256Digest = new SHA3Digest();
        sha3_256Digest.update(byteArray, 0, byteArray.length);

        byte[] hashedByteArray = new byte[64];
        sha3_256Digest.doFinal(hashedByteArray, 0);

        return new byte[]{
                hashedByteArray[0],
                hashedByteArray[1]
        };
    }

    /**
     * The format how the private key is stored in the tor directory
     */
    public static String getPrivateKeyInOpenSshFormat(byte[] privateKey) {
        // Key Format definition:
        // https://gitlab.torproject.org/tpo/core/torspec/-/blob/main/control-spec.txt
        byte[] secretScalar = generateSecretScalar(privateKey);
        String encoded = java.util.Base64.getEncoder().encodeToString(secretScalar);
        return "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
                encoded + "\n" +
                "-----END OPENSSH PRIVATE KEY-----\n";
    }

    private static byte[] generateSecretScalar(byte[] privateKey) {
        // https://www.rfc-editor.org/rfc/rfc8032#section-5.1

        SHA512Digest sha512Digest = new SHA512Digest();
        sha512Digest.update(privateKey, 0, privateKey.length);

        byte[] secretScalar = new byte[64];
        sha512Digest.doFinal(secretScalar, 0);

        secretScalar[0] &= (byte) 248;
        secretScalar[31] &= 127;
        secretScalar[31] |= 64;

        return secretScalar;
    }


    //todo support loading tor private key from tor dir
/*    private TorIdentity findOrCreateTorIdentity(String identityTag) {
        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        boolean isTorSupported = supportedTransportTypes.contains(TransportType.TOR);
        if (isTorSupported) {
            // If we find a persisted tor private_key in the tor hiddenservice directory for the given identityTag
            // we use that, otherwise we create a new one.
            Optional<TorIdentity> persistedTorIdentity = findPersistedTorIdentityFromTorDir(identityTag);
            if (persistedTorIdentity.isPresent()) {
                return persistedTorIdentity.get();
            }
        }

        Map<TransportType, Integer> defaultPorts = networkService.getDefaultNodePortByTransportType();
        int torPort = isTorSupported && identityTag.equals(DEFAULT_IDENTITY_TAG) ?
                defaultPorts.getOrDefault(TransportType.TOR, NetworkUtils.selectRandomPort()) :
                NetworkUtils.selectRandomPort();
        byte[] privateKey = keyBundleService.findKeyBundle(keyBundleService.getKeyIdFromTag(identityTag)).orElseThrow().getTorKeyPair().getPrivateKey();
        return TorIdentity.from(privateKey, torPort);
    }*/
}

