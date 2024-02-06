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

package bisq.security;

import bisq.security.keys.TorKeyGeneration;
import bisq.security.keys.TorKeyPair;
import org.bouncycastle.crypto.CryptoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class TorSignatureUtilTests {
    private final TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();

    @Test
    void signEmptyMessage() throws CryptoException {
        byte[] message = new byte[0];
        byte[] signature = TorSignatureUtil.sign(torKeyPair.getPrivateKey(), message);
        boolean isValid = TorSignatureUtil.verify(torKeyPair.getPublicKey(), message, signature);
        assertThat(isValid).isTrue();
    }

    @Test
    void signMessage() throws CryptoException {
        byte[] message = "Hello World!".getBytes(StandardCharsets.UTF_8);
        byte[] signature = TorSignatureUtil.sign(torKeyPair.getPrivateKey(), message);
        boolean isValid = TorSignatureUtil.verify(torKeyPair.getPublicKey(), message, signature);
        assertThat(isValid).isTrue();
    }

    @Test
    void verifyInvalidMessage() throws CryptoException {
        byte[] message = "Hello World!".getBytes(StandardCharsets.UTF_8);
        byte[] signature = TorSignatureUtil.sign(torKeyPair.getPrivateKey(), message);
        boolean isValid = TorSignatureUtil.verify(torKeyPair.getPublicKey(), "Hi".getBytes(), signature);
        assertThat(isValid).isFalse();
    }

    @Test
    void verifyInvalidSignature() throws CryptoException {
        byte[] message = "Hello World!".getBytes(StandardCharsets.UTF_8);
        byte[] invalidSignature = TorSignatureUtil.sign(torKeyPair.getPrivateKey(), "Hi".getBytes());
        boolean isValid = TorSignatureUtil.verify(torKeyPair.getPublicKey(), message, invalidSignature);
        assertThat(isValid).isFalse();
    }
}
