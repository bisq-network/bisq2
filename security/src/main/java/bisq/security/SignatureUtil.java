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
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECKey;

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

    /**
     * Supports only {@link #SHA256withECDSA} and {@link #SHA256withDSA}, given by exactly these names. Other names
     * are rejected, including aliases which the provider would accept (other letter case, OIDs). An ECDSA signature
     * must be converted to the low-S form, and this conversion depends on the signature encoding. For an arbitrary
     * algorithm name this method cannot know the encoding, and an ECDSA signature could skip the conversion.
     */
    public static byte[] sign(byte[] message, PrivateKey privateKey, String algorithm) throws GeneralSecurityException {
        return switch (algorithm) {
            case SHA256withECDSA -> {
                if (!(privateKey instanceof ECKey ecKey)) {
                    throw new InvalidKeyException(SHA256withECDSA + " requires an EC private key");
                }
                // For each ECDSA signature (r, s), the signature (r, n - s) is valid as well. Only the low-S form is
                // created, so that verifiers can reject the other form (see verifyLowS). All verifiers accept it.
                yield toLowS(signWithProvider(message, privateKey, algorithm), ecKey.getParams().getOrder());
            }
            case SHA256withDSA -> signWithProvider(message, privateKey, algorithm);
            default -> throw new NoSuchAlgorithmException("Unsupported signature algorithm: " + algorithm +
                    ". Supported are " + SHA256withECDSA + " and " + SHA256withDSA);
        };
    }

    private static byte[] signWithProvider(byte[] message, PrivateKey privateKey, String algorithm) throws GeneralSecurityException {
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

    /**
     * Like {@link #verify(byte[], byte[], PublicKey)}, but accepts only the low-S form of an ECDSA signature
     * (s <= n / 2, where n is the order of the curve).
     * <p>
     * A third party can turn a valid signature (r, s) into another valid signature (r, n - s) without the private key.
     * Requiring the low-S form leaves one valid encoding per signature. BouncyCastle already rejects encodings which
     * are not strict DER.
     * <p>
     * Use this method only for signature formats which were introduced together with low-S signing in
     * {@link #sign(byte[], PrivateKey, String)}. Nodes of earlier versions create high-S signatures in about half of all
     * cases, so this method rejects their signatures.
     * <p>
     * As {@link #verify(byte[], byte[], PublicKey)}, it returns false or throws a {@link SignatureException} if the
     * signature is invalid.
     */
    public static boolean verifyLowS(byte[] message, byte[] signature, PublicKey publicKey) throws GeneralSecurityException {
        if (!(publicKey instanceof ECKey ecKey)) {
            throw new InvalidKeyException("Low-S verification requires an EC public key");
        }
        if (!isLowS(signature, ecKey.getParams().getOrder())) {
            return false;
        }
        return verify(message, signature, publicKey, SHA256withECDSA);
    }

    private static byte[] toLowS(byte[] derSignature, BigInteger curveOrder) throws SignatureException {
        BigInteger[] rs = decodeDerSignature(derSignature);
        BigInteger s = rs[1];
        if (s.compareTo(curveOrder.shiftRight(1)) <= 0) {
            return derSignature;
        }
        return encodeDerSignature(rs[0], curveOrder.subtract(s));
    }

    private static boolean isLowS(byte[] derSignature, BigInteger curveOrder) throws SignatureException {
        BigInteger s = decodeDerSignature(derSignature)[1];
        return s.compareTo(curveOrder.shiftRight(1)) <= 0;
    }

    private static BigInteger[] decodeDerSignature(byte[] derSignature) throws SignatureException {
        try {
            ASN1Primitive primitive = ASN1Primitive.fromByteArray(derSignature);
            if (primitive == null) {
                throw new SignatureException("ECDSA signature is empty");
            }
            ASN1Sequence sequence = ASN1Sequence.getInstance(primitive);
            if (sequence.size() != 2) {
                throw new SignatureException("ECDSA signature must contain 2 integers");
            }
            return new BigInteger[]{
                    ASN1Integer.getInstance(sequence.getObjectAt(0)).getValue(),
                    ASN1Integer.getInstance(sequence.getObjectAt(1)).getValue()
            };
        } catch (IOException | IllegalArgumentException e) {
            throw new SignatureException("Invalid ECDSA signature encoding", e);
        }
    }

    private static byte[] encodeDerSignature(BigInteger r, BigInteger s) throws SignatureException {
        try {
            return new DERSequence(new ASN1Integer[]{new ASN1Integer(r), new ASN1Integer(s)}).getEncoded();
        } catch (IOException e) {
            throw new SignatureException("Encoding ECDSA signature failed", e);
        }
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
