package bisq.security.keys;

import bisq.common.encoding.Hex;
import bisq.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base32;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class TorKeyUtils {
    public static byte[] getPublicKeyFromOnionAddress(String onionAddress) {
        onionAddress = onionAddress.substring(0, onionAddress.length() - ".onion".length());
        byte[] decodedOnionAddress = Base32.decode(onionAddress.toUpperCase());
        return Arrays.copyOfRange(decodedOnionAddress, 0, 32);
    }

    public static void writePrivateKey(TorKeyPair torKeyPair, String baseDir, String identityTag) {
        File hiddenServiceDir = getHiddenServiceDirectory(baseDir, identityTag);
        // Only try to write if tor directory exists already (if we run with TOR)
        if (hiddenServiceDir.exists()) {
            try {
                String path = hiddenServiceDir.getAbsolutePath();
                FileUtils.makeDirs(hiddenServiceDir);
                File privateKeyFile = Path.of(path, "private_key").toFile();
                if (!privateKeyFile.exists()) {
                    byte[] privateKey = torKeyPair.getPrivateKey();
                    String onionAddress = torKeyPair.getOnionAddress();
                    String privateKeyInOpenSshFormat = TorKeyGeneration.getPrivateKeyInOpenSshFormat(privateKey);
                    String privateKeyAsHex = Hex.encode(privateKey);
                    FileUtils.writeToFile(privateKeyAsHex, Path.of(path, "private_key_hex").toFile());
                    FileUtils.writeToFile(privateKeyInOpenSshFormat, privateKeyFile);
                    FileUtils.writeToFile(onionAddress, Path.of(path, "hostname").toFile());
                    log.info("We persisted the tor private key for onionAddress {} for identityTag {} to {}.",
                            onionAddress, identityTag, path);
                }
            } catch (IOException e) {
                log.error("Could not persist torIdentity", e);
            }
        }
    }

    public static Optional<byte[]> findPrivateKey(String baseDir, String identityTag) {
        File hiddenServiceDir = getHiddenServiceDirectory(baseDir, identityTag);
        if (!hiddenServiceDir.exists()) {
            return Optional.empty();
        }
        try {
            String path = hiddenServiceDir.getAbsolutePath();
            String privateKeyAsHex = FileUtils.readStringFromFile(Path.of(path, "private_key_hex").toFile());
            byte[] privateKey = Hex.decode(privateKeyAsHex);
            log.info("We found an existing tor private key at {} and use that for identityTag {}.", path, identityTag);
            return Optional.of(privateKey);
        } catch (IOException e) {
            log.warn("Could not read private_key_hex or port", e);
            return Optional.empty();
        }
    }

    public static File getHiddenServiceDirectory(String baseDir, String identityTag) {
        return new File(Path.of(baseDir, "tor", "hiddenservice", identityTag).toString());
    }
}
