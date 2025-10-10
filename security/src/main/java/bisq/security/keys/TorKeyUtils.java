package bisq.security.keys;

import bisq.common.encoding.Hex;
import bisq.common.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base32;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

@Slf4j
public class TorKeyUtils {
    public static byte[] getPublicKeyFromOnionAddress(String onionAddress) {
        onionAddress = onionAddress.substring(0, onionAddress.length() - ".onion".length());
        byte[] decodedOnionAddress = Base32.decode(onionAddress.toUpperCase(Locale.ROOT));
        return Arrays.copyOfRange(decodedOnionAddress, 0, 32);
    }

    public static void writePrivateKey(TorKeyPair torKeyPair, Path storageDir, String tag) {
        Path targetPath = Paths.get(storageDir.toString(), tag);
        try {
            Files.createDirectories(targetPath);
            String dir = targetPath.toAbsolutePath().toString();

            FileUtils.writeToFile(Hex.encode(torKeyPair.getPrivateKey()), Paths.get(dir, "private_key_hex"));
            FileUtils.writeToFile(torKeyPair.getOnionAddress(), Paths.get(dir, "hostname"));

            log.info("We persisted the tor private key in hex encoding for onionAddress {} for tag {} to {}.",
                    torKeyPair.getOnionAddress(), tag, dir);
        } catch (Exception e) {
            log.error("Could not persist torIdentity", e);
        }
    }

    public static TorKeyPair fromPrivateKey(String privateKeyEncoded) {
        return TorKeyGeneration.generateKeyPair(Hex.decode(privateKeyEncoded));
    }
}
