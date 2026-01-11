/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.api.access.filter.authn;

import bisq.api.access.identity.DeviceProfile;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import bisq.api.access.session.SessionToken;
import bisq.common.encoding.Base64;
import bisq.security.SignatureUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public final class SessionAuthenticationService {

    private static final long MAX_CLOCK_SKEW_MS = TimeUnit.MINUTES.toMillis(10);
    private static final long NONCE_TTL_MS = SessionToken.TTL+ TimeUnit.MINUTES.toMillis(5);

    private final PairingService pairingService;
    private final SessionService sessionService;

    private final Map<String, Long> timestampBySessionNonceKeys = new ConcurrentHashMap<>();

    public SessionAuthenticationService(PairingService pairingService, SessionService sessionService) {
        this.pairingService = pairingService;
        this.sessionService = sessionService;
    }

    public AuthenticatedSession authenticate(
            String sessionId,
            String method,
            URI requestUri,
            String nonce,
            String timestamp,
            String signatureBase64,
            Optional<String> bodyHashBase64
    ) throws AuthenticationException {

        checkNotNull(sessionId, "Missing sessionId");
        checkNotNull(nonce, "Missing nonce");
        checkNotNull(timestamp, "Missing timestamp");
        checkNotNull(signatureBase64, "Missing signature");

        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new AuthenticationException("Invalid timestamp format");
        }

        long now = System.currentTimeMillis();
        long skew = Math.abs(now - requestTime);
        if (skew > MAX_CLOCK_SKEW_MS) {
            throw new AuthenticationException("Request timestamp outside allowed skew");
        }

        SessionToken session = sessionService.find(sessionId)
                .orElseThrow(() -> new AuthenticationException("Invalid session"));

        if (session.isExpired()) {
            throw new AuthenticationException("Session expired");
        }

        DeviceProfile deviceProfile = pairingService.findDeviceProfile(session.getDeviceId())
                .orElseThrow(() -> new AuthenticationException("Unknown device"));

        String signedMessage = buildCanonicalRequest(
                nonce,
                timestamp,
                method,
                requestUri,
                bodyHashBase64
        );

        verifySignature(
                signedMessage,
                signatureBase64,
                deviceProfile.getPublicKey()
        );

        enforceNonceUniqueness(sessionId, nonce);

        return new AuthenticatedSession(
                session.getSessionId(),
                session.getDeviceId()
        );
    }

    private static String buildCanonicalRequest(String nonce,
                                                String timestamp,
                                                String method,
                                                URI uri,
                                                Optional<String> bodyHash) {

        return String.join("\n",
                "v1",
                nonce,
                timestamp,
                method.toUpperCase(Locale.ROOT),
                AuthUtils.normalizePathAndQuery(uri),
                bodyHash.orElse("")
        );
    }

    private static void verifySignature(String message,
                                        String signatureBase64,
                                        PublicKey publicKey)
            throws AuthenticationException {

        byte[] signature;
        try {
            signature = Base64.decode(signatureBase64);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Invalid base64 signature");
        }
        try {
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            boolean isValidSignature = SignatureUtil.verify(bytes, signature, publicKey);
            if (!isValidSignature) {
                throw new AuthenticationException("Invalid request signature");
            }
        } catch (GeneralSecurityException e) {
            throw new AuthenticationException("Invalid request signature");
        }
    }

    private void enforceNonceUniqueness(String sessionId, String nonce) throws AuthenticationException {
        long now = System.currentTimeMillis();
        timestampBySessionNonceKeys.entrySet().removeIf(entry -> now - entry.getValue() > NONCE_TTL_MS);

        String key = sessionId + ":" + nonce;
        if (timestampBySessionNonceKeys.putIfAbsent(key, now) != null) {
            throw new AuthenticationException("Nonce already used");
        }
    }
}
