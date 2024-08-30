package bisq.security.keys;

import bisq.common.encoding.Hex;
import bisq.common.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base32;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
public class TorKeyUtils {
    public static byte[] getPublicKeyFromOnionAddress(String onionAddress) {
        onionAddress = onionAddress.substring(0, onionAddress.length() - ".onion".length());
        byte[] decodedOnionAddress = Base32.decode(onionAddress.toUpperCase());
        return Arrays.copyOfRange(decodedOnionAddress, 0, 32);
    }

    public static void writePrivateKey(TorKeyPair torKeyPair, Path storageDir, String tag) {
        Path targetPath = Path.of(storageDir.toString(), tag);
        File torPrivateKeyDir = targetPath.toFile();
        try {
            log.info("Store the torPrivateKey into {}", torPrivateKeyDir);
            FileUtils.makeDirs(torPrivateKeyDir);
            String dir = torPrivateKeyDir.getAbsolutePath();

            FileUtils.writeToFile(Hex.encode(torKeyPair.getPrivateKey()), Path.of(dir, "private_key_hex").toFile());
            FileUtils.writeToFile(torKeyPair.getOnionAddress(), Path.of(dir, "hostname").toFile());

            log.info("We persisted the tor private key in hex encoding for onionAddress {} for tag {} to {}.",
                    torKeyPair.getOnionAddress(), tag, dir);
        } catch (Exception e) {
            log.error("Could not persist torIdentity", e);
        }
    }
}
