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

package bisq.api.access.identity;

import bisq.security.DigestUtil;
import bisq.security.HmacUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Names a paired client in the API without disclosing its credentials.
 * <p>
 * A {@code clientId} is part of what a client authenticates with, so it is not something to hand to
 * other clients. Anything that lists clients therefore names them with a handle that carries no
 * credential value.
 * <p>
 * Derived rather than stored, so no migration and no second identity to keep in sync: an HMAC over
 * the client id, keyed by a hash of that client's own secret. That makes it stable for the life of
 * the pairing, unique per client, unable to be turned back into the id, and not computable by anyone
 * who does not already hold the secret.
 */
@Slf4j
public final class ClientManagementId {
    private static final String DOMAIN_SEPARATOR = "bisq-api-client-management-id:";
    /** 128 bits, far past collision or guessing concerns for a handful of paired clients. */
    private static final int LENGTH_IN_BYTES = 16;

    private ClientManagementId() {
    }

    public static String of(ClientProfile clientProfile) {
        byte[] message = (DOMAIN_SEPARATOR + clientProfile.getClientId()).getBytes(StandardCharsets.UTF_8);
        // Hashed into the key rather than used raw: HMAC keys have a minimum length here, and a
        // profile with a shorter secret must not make the whole listing fail.
        byte[] key = DigestUtil.sha256(clientProfile.getClientSecret().getBytes(StandardCharsets.UTF_8));
        try {
            byte[] hmac = HmacUtil.createHmac(message, HmacUtil.createHmacKeySpec(key));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(Arrays.copyOf(hmac, LENGTH_IN_BYTES));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not derive the management ID of a client", e);
        }
    }

    /** Compared in constant time, as the value selects which client a revocation applies to. */
    public static boolean matches(ClientProfile clientProfile, String managementId) {
        return MessageDigest.isEqual(of(clientProfile).getBytes(StandardCharsets.UTF_8),
                managementId.getBytes(StandardCharsets.UTF_8));
    }
}
