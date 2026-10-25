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
package bisq.network.tor.controller;

import bisq.network.tor.onionservice.OnionServiceKey;
import com.google.common.io.BaseEncoding;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Random keys in the format of real onion service keys. The tests only check that secrets do not reach the logs, so
 * the onion address does not need to belong to the secret.
 */
final class TestKeys {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TestKeys() {
    }

    // Like the secret scalar of a Tor key: 64 bytes, so the Base64 form ends with "=="
    static String randomBase64SecretScalar() {
        return Base64.getEncoder().encodeToString(randomBytes(64));
    }

    static OnionServiceKey randomOnionServiceKey() {
        String serviceId = BaseEncoding.base32().omitPadding().encode(randomBytes(35)).toLowerCase(Locale.ROOT);
        return new OnionServiceKey(serviceId + ".onion", randomBase64SecretScalar());
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
