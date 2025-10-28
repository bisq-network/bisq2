package bisq.http_api.auth;

import bisq.common.encoding.Hex;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public final class AuthUtils {
    private AuthUtils() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    public static final String AUTH_HEADER = "AUTH-TOKEN";
    public static final String AUTH_TIMESTAMP_HEADER = "AUTH-TS";
    private static final String AUTH_HMAC_ALGORITHM = "HmacSHA256";
    // timestamp validity is long to allow Tor requests to succeed
    // but also increases the window of vulnerability for replay attacks
    private static final long AUTH_TIMESTAMP_VALIDITY_MS = 45_000;

    public static SecretKeySpec getSecretKey(String password) {
        return new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), AUTH_HMAC_ALGORITHM);
    }

    public static boolean isValidAuthentication(SecretKey secretKey,
                                                @Nullable String timestamp,
                                                @Nullable String receivedHmac) {
        if (timestamp == null || receivedHmac == null) {
            return false;
        }

        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();

            if (Math.abs(currentTime - requestTime) > AUTH_TIMESTAMP_VALIDITY_MS) {
                log.warn("Request timestamp expired or too far in future");
                return false;
            }

            Mac mac = Mac.getInstance(AUTH_HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] expectedHmac = mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8));

            byte[] receivedHmacBytes = Hex.decode(receivedHmac);

            // Constant-time comparison
            return MessageDigest.isEqual(expectedHmac, receivedHmacBytes);

        } catch (NumberFormatException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.warn("Error validating authentication", e);
            return false;
        } catch (Exception e) {
            log.warn("Error decoding HMAC", e);
            return false;
        }
    }
}
