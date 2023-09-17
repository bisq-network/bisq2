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

import lombok.Getter;
import lombok.ToString;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.math.ec.rfc8032.Ed25519;

import java.security.SecureRandom;

@Getter
@ToString
public class TorIdentity {

    @ToString.Exclude
    private final byte[] privateKey;
    private final int port;

    public TorIdentity(byte[] privateKey, int port) {
        this.privateKey = privateKey;
        this.port = port;
    }

    public static TorIdentity generate(int port) {
        byte[] privateKey = new byte[32];
        Ed25519.generatePrivateKey(new SecureRandom(), privateKey);
        return new TorIdentity(privateKey, port);
    }

    public String getTorOnionKey() {
        // Key Format definition:
        // https://gitlab.torproject.org/tpo/core/torspec/-/blob/main/control-spec.txt

        byte[] secretScalar = generateSecretScalar(privateKey);
        String base64EncodedSecretScalar = java.util.Base64.getEncoder()
                .encodeToString(secretScalar);

        return  "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
                base64EncodedSecretScalar + "\n" +
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
}
