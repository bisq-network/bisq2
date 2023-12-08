package bisq.security;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Base32;

import java.util.Arrays;

public class TorSignatureUtil {
    public static byte[] sign(byte[] privateKey, byte[] message) throws CryptoException {
        Signer signer = new Ed25519Signer();
        signer.init(true, new Ed25519PrivateKeyParameters(privateKey));
        signer.update(message, 0, message.length);
        return signer.generateSignature();
    }

    public static boolean verify(String onionAddress, byte[] message, byte[] signature) {
        onionAddress = onionAddress.substring(0, onionAddress.length() - ".onion".length());
        byte[] decodedOnionAddress = Base32.decode(onionAddress.toUpperCase());
        byte[] publicKey = Arrays.copyOfRange(decodedOnionAddress, 0, 32);

        Signer verifier = new Ed25519Signer();
        verifier.init(false, new Ed25519PublicKeyParameters(publicKey));
        verifier.update(message, 0, message.length);
        return verifier.verifySignature(signature);
    }
}
