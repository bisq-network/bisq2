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

package bisq.network.identity;

import bisq.common.proto.Proto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.util.encoders.Base32;

import java.nio.ByteBuffer;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class TorIdentity implements Proto {
    public static TorIdentity from(byte[] privateKey, int port) {
        return new TorIdentity(privateKey, port, computeOnionAddressFromPrivateKey(privateKey));
    }

    @ToString.Exclude
    private final byte[] privateKey;

    @EqualsAndHashCode.Include
    @Getter
    private final int port;

    @EqualsAndHashCode.Include
    @Getter
    private final String onionAddress;

    private TorIdentity(byte[] privateKey, int port, String onionAddress) {
        this.privateKey = privateKey;
        this.port = port;
        this.onionAddress = onionAddress;
    }

    @Override
    public bisq.network.identity.protobuf.TorIdentity toProto() {
        return bisq.network.identity.protobuf.TorIdentity.newBuilder()
                .setPrivateKey(ByteString.copyFrom(privateKey))
                .setPort(port)
                .build();
    }

    public static TorIdentity fromProto(bisq.network.identity.protobuf.TorIdentity proto) {
        byte[] privateKey = proto.getPrivateKey().toByteArray();
        return new TorIdentity(
                privateKey,
                proto.getPort(),
                computeOnionAddressFromPrivateKey(privateKey)
        );
    }

    public String getTorOnionKey() {
        // Key Format definition:
        // https://gitlab.torproject.org/tpo/core/torspec/-/blob/main/control-spec.txt

        byte[] secretScalar = generateSecretScalar(privateKey);
        String base64EncodedSecretScalar = java.util.Base64.getEncoder()
                .encodeToString(secretScalar);

        return "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
                base64EncodedSecretScalar + "\n" +
                "-----END OPENSSH PRIVATE KEY-----\n";
    }

    private static String computeOnionAddressFromPrivateKey(byte[] privateKey) {
        byte[] publicKey = new byte[32];
        Ed25519.generatePublicKey(privateKey, 0, publicKey, 0);

        byte[] checksumForAddress = computeOnionAddressChecksum(publicKey);

        ByteBuffer byteBuffer = ByteBuffer.allocate(32 + 2 + 1);
        byteBuffer.put(publicKey); // 32 bytes
        byteBuffer.put(checksumForAddress); // 2 bytes
        byteBuffer.put((byte) 3); // 1 byte

        byte[] byteArray = byteBuffer.array();
        String base32String = Base32.toBase32String(byteArray);

        return base32String.toLowerCase() + ".onion";
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
}

