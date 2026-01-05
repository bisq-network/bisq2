package bisq.security.keys;

import bisq.common.encoding.Hex;
import bisq.common.facades.FacadeProvider;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;

@Slf4j
public class KeyPairUtils {
    static {
        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static void writePrivateKey(KeyPair keyPair, Path storageDirPath, String tag) {
        Path targetPath = storageDirPath.resolve(tag);
        try {
            FacadeProvider.getJdkFacade().createDirectories(targetPath);

            ECPrivateKey ecPrivate = (ECPrivateKey) keyPair.getPrivate();
            byte[] priv32 = toUnsignedFixedLength(ecPrivate.getS(), 32);

            FacadeProvider.getJdkFacade().writeString(Hex.encode(priv32), targetPath.resolve("private_key_hex"));
            log.info("Persisted hex encoded 32-byte secp256k1 private key for tag {} at {}", tag, targetPath);
        } catch (Exception e) {
            log.error("Could not persist private key", e);
        }
    }

    private static byte[] toUnsignedFixedLength(BigInteger d, int length) {
        byte[] b = d.toByteArray(); // big-endian; may include leading 0x00
        if (b.length == length) return b;
        if (b.length == length + 1 && b[0] == 0x00) return java.util.Arrays.copyOfRange(b, 1, b.length);
        if (b.length < length) {
            byte[] out = new byte[length];
            System.arraycopy(b, 0, out, length - b.length, b.length);
            return out;
        }
        throw new IllegalArgumentException("Invalid secp256k1 scalar length");
    }

    public static KeyPair fromPrivateKey(String privateKeyHex) {
        try {
            byte[] privBytes = Hex.decode(privateKeyHex);
            if (privBytes.length != 32) {
                throw new IllegalArgumentException("Private key must be 32 bytes");
            }
            BigInteger privateKeyInt = new BigInteger(1, privBytes);
            ECParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
            if (privateKeyInt.signum() <= 0 || privateKeyInt.compareTo(parameterSpec.getN()) >= 0) {
                throw new IllegalArgumentException("Invalid secp256k1 private key");
            }
            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKeyInt, parameterSpec);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            // Derive public point = G * privateKey
            ECPoint q = parameterSpec.getG().multiply(privateKeyInt).normalize();
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(q, parameterSpec);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            log.error("Could not create keypair from private key", e);
            throw new RuntimeException(e);
        }
    }
}
