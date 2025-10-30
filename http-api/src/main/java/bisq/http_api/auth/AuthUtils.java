package bisq.http_api.auth;

import bisq.common.encoding.Hex;
import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class AuthUtils {
    private AuthUtils() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    public static final String AUTH_HEADER = "AUTH-TOKEN";
    public static final String AUTH_TIMESTAMP_HEADER = "AUTH-TS";
    public static final String AUTH_NONCE_HEADER = "AUTH-NONCE";
    private static final String AUTH_HMAC_ALGORITHM = "HmacSHA256";
    // timestamp validity is long to allow Tor requests to succeed
    // but also increases the window of vulnerability for replay attacks
    private static final long AUTH_TIMESTAMP_VALIDITY_MS = 3_600_000; // 1 hour, to account for out of sync clocks
    // Map to hold used nonces for the duration of AUTH_TIMESTAMP_VALIDITY_MS
    private static final ConcurrentHashMap<String, Long> usedNonces = new ConcurrentHashMap<>();

    /**
     * Checks if a nonce is unused and marks it as used for the duration of AUTH_TIMESTAMP_VALIDITY_MS.
     * Returns true if the nonce was not used before, false otherwise.
     */
    private static boolean canUseNonce(String nonce) {
        long now = System.currentTimeMillis();
        usedNonces.entrySet().removeIf(e -> now - e.getValue() > AUTH_TIMESTAMP_VALIDITY_MS);
        Long previous = usedNonces.putIfAbsent(nonce, now);
        return previous == null;
    }

    public static SecretKeySpec getSecretKey(String password) {
        return new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), AUTH_HMAC_ALGORITHM);
    }

    public static String normalizePathAndQuery(URI requestUri) {
        // we strip trailing slash and add query if not empty
        String rawPath = requestUri.getRawPath();
        if (StringUtils.isEmpty(rawPath)) {
            rawPath = "/";
        } else if (rawPath.length() > 1 && rawPath.endsWith("/")) {
            rawPath = rawPath.substring(0, rawPath.length() - 1);
        }
        String rawQuery = requestUri.getRawQuery();
        String result = rawPath;
        if (StringUtils.isNotEmpty(rawQuery)) {
            result += "?" + rawQuery;
        }
        return result;
    }

    public static boolean isValidAuthentication(SecretKey secretKey,
                                                String method,
                                                String normalizedPathAndQuery,
                                                @Nullable String nonce,
                                                @Nullable String timestamp,
                                                @Nullable String receivedHmac) {
        return isValidAuthentication(secretKey, method, normalizedPathAndQuery, nonce, timestamp, receivedHmac, null);
    }

    public static boolean isValidAuthentication(SecretKey secretKey,
                                                String method,
                                                String normalizedPathAndQuery,
                                                @Nullable String nonce,
                                                @Nullable String timestamp,
                                                @Nullable String receivedHmac,
                                                @Nullable String bodySha256Hex) {
        if (timestamp == null || receivedHmac == null || nonce == null || !canUseNonce(nonce)) {
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
            String canonical = nonce
                    + "\n" + timestamp
                    + "\n" + method.toUpperCase(java.util.Locale.ROOT)
                    + "\n" + normalizedPathAndQuery
                    + "\n" + (bodySha256Hex == null ? "" : bodySha256Hex);
            byte[] expectedHmac = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));

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
