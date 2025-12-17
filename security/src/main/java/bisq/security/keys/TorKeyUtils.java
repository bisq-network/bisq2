package bisq.security.keys;

import bisq.common.encoding.Hex;
import bisq.common.facades.FacadeProvider;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base32;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

@Slf4j
public class TorKeyUtils {
    public static byte[] getPublicKeyFromOnionAddress(String onionAddress) {
        onionAddress = onionAddress.substring(0, onionAddress.length() - ".onion".length());
        byte[] decodedOnionAddress = Base32.decode(onionAddress.toUpperCase(Locale.ROOT));
        return Arrays.copyOfRange(decodedOnionAddress, 0, 32);
    }

    public static void writePrivateKey(TorKeyPair torKeyPair, Path storageDirPath, String tag) {
        Path targetPath = storageDirPath.resolve(tag);
        try {
            FacadeProvider.getJdkFacade().createDirectories(targetPath);

            FacadeProvider.getJdkFacade().writeString(Hex.encode(torKeyPair.getPrivateKey()), targetPath.resolve("private_key_hex"));
            FacadeProvider.getJdkFacade().writeString(torKeyPair.getOnionAddress(), targetPath.resolve("hostname"));

            log.info("We persisted the tor private key in hex encoding for onionAddress {} for tag {} to {}.",
                    torKeyPair.getOnionAddress(), tag, targetPath);
        } catch (Exception e) {
            log.error("Could not persist torIdentity", e);
        }
    }

    public static TorKeyPair fromPrivateKey(String privateKeyEncoded) {
        return TorKeyGeneration.generateKeyPair(Hex.decode(privateKeyEncoded));
    }
}
