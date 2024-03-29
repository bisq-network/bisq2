package bisq.security.keys;

import bisq.common.encoding.Hex;
import bisq.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base32;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
public class TorKeyUtils {
    public static byte[] getPublicKeyFromOnionAddress(String onionAddress) {
        onionAddress = onionAddress.substring(0, onionAddress.length() - ".onion".length());
        byte[] decodedOnionAddress = Base32.decode(onionAddress.toUpperCase());
        return Arrays.copyOfRange(decodedOnionAddress, 0, 32);
    }

    public static void writePrivateKey(TorKeyPair torKeyPair, String baseDir, String tag) {
        File hiddenServiceDir = getHiddenServiceDirectory(baseDir, tag);
        // Only try to write if tor directory exists already (if we run with TOR)
        if (hiddenServiceDir.exists()) {
            try {
                String path = hiddenServiceDir.getAbsolutePath();
                FileUtils.makeDirs(hiddenServiceDir);

                FileUtils.writeToFile(Hex.encode(torKeyPair.getPrivateKey()), Path.of(path, "private_key_hex").toFile());
                FileUtils.writeToFile(torKeyPair.getOnionAddress(), Path.of(path, "hostname").toFile());

                // We do not write the private key in open SSH format to not give the impression that the key was
                // generated by Tor or is used. We only write the private key in hex encoding for usage as config
                // parameter.

                log.info("We persisted the tor private key in hex encoding for onionAddress {} for tag {} to {}.",
                        torKeyPair.getOnionAddress(), tag, path);
            } catch (IOException e) {
                log.error("Could not persist torIdentity", e);
            }
        }
    }

    public static File getHiddenServiceDirectory(String baseDir, String tag) {
        return new File(Path.of(baseDir, "tor", "hiddenservice", tag).toString());
    }
}
