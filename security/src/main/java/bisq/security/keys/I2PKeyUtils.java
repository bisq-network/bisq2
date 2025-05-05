package bisq.security.keys;

import bisq.common.encoding.Hex;
import bisq.common.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base32;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class I2PKeyUtils {

    public static byte[] getPublicKeyFromB32Address(String b32Address) {
        b32Address = b32Address.substring(0, b32Address.length() - ".b32.i2p".length());
        return Base32.decode(b32Address.toUpperCase());
    }

    public static void writePrivateKey(I2pKeyPair i2pKeyPair, Path storageDir, String tag) {
        Path targetPath = Paths.get(storageDir.toString(), tag);
        File i2pPrivateKeyDir = targetPath.toFile();
        try {
            log.info("Storing the I2P private key into {}", i2pPrivateKeyDir);
            FileUtils.makeDirs(i2pPrivateKeyDir);
            String dir = i2pPrivateKeyDir.getAbsolutePath();

            FileUtils.writeToFile(Hex.encode(i2pKeyPair.getPrivateKey()),
                    Paths.get(dir, "private_key_hex").toFile());

            String b32Address = Base32.encode(i2pKeyPair.getPublicKey()) + ".b32.i2p";
            FileUtils.writeToFile(b32Address, Paths.get(dir, "hostname").toFile());

            log.info("Persisted I2P private key in hex encoding for b32Address {} for tag {} to {}.",
                    b32Address, tag, dir);
        } catch (Exception e) {
            log.error("Could not persist I2P identity", e);
        }
    }
}
