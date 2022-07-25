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

import bisq.common.encoding.Base64;
import bisq.common.encoding.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class SignatureUtil {
    public static final String SHA256withECDSA = "SHA256withECDSA";
    public static final String SHA256withDSA = "SHA256withDSA";

    static {
        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static byte[] sign(byte[] message, PrivateKey privateKey) throws GeneralSecurityException {
        return sign(message, privateKey, SHA256withECDSA);
    }

    public static byte[] sign(byte[] message, PrivateKey privateKey, String algorithm) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(algorithm, "BC");
        signature.initSign(privateKey);
        signature.update(message);
        return signature.sign();
    }

    public static boolean verify(byte[] message, byte[] signature, PublicKey publicKey) throws GeneralSecurityException {
        return verify(message, signature, publicKey, SHA256withECDSA);
    }

    public static boolean verify(byte[] message, byte[] signature, PublicKey publicKey, String algorithm) throws GeneralSecurityException {
        Signature sig = Signature.getInstance(algorithm, "BC");
        sig.initVerify(publicKey);
        sig.update(message);
        return sig.verify(signature);
    }

    // input: a base-64 bitcoin sig
    // output a DER signature
    public static byte[] bitcoinSigToDer(String bitcoinSig) {
        String sigHex = Hex.encode(Base64.decode(bitcoinSig));
        String r = Integer.parseInt(sigHex.substring(2, 4), 16) > 127 ?
                "00" + sigHex.substring(2, 66) : sigHex.substring(2, 66);
        String s = Integer.parseInt(sigHex.substring(66, 68), 16) > 127 ?
                "00" + sigHex.substring(66) : sigHex.substring(66);
        String result = "02" + String.format("%02X", r.length() / 2) + r +
                "02" + String.format("%02X", s.length() / 2) + s;
        result = "30" + String.format("%02X", result.length() / 2) + result;
        return Hex.decode(result);
    }

    private static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";
    private static final byte[] BITCOIN_SIGNED_MESSAGE_HEADER_BYTES = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes(StandardCharsets.UTF_8);

    public static byte[] formatMessageForSigning(String message) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES.length);
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES);
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            bos.write(messageBytes.length);
            bos.write(messageBytes);
            return DigestUtil.sha256(DigestUtil.sha256(bos.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }
}
