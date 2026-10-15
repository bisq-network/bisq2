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

package bisq.common.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Versions 2.1.3 to 2.1.13 logged the onion private key of each identity at every start (see
 * docs/specifications/security/secrets-in-logs.md).
 */
class LegacyLogSecretsTest {
    // 64 bytes, Base64 encoded, as TorKeyPair.getBase64SecretScalar() returns it
    private static final String PRIVATE_KEY = "a".repeat(41) + "B/+9".repeat(11) + "c==";
    private static final String PREFIX = "Oct-01 10:15:22.123 [Tor control] INFO  b.n.t.c.TorControlProtocol: ";

    @Test
    void privateKeyOfALineOfVersion2113IsHidden() {
        String line = PREFIX + "Send Tor control command: ADD_ONION ED25519-V3:" + PRIVATE_KEY + " Port=9999,127.0.0.1:8000\n";

        assertThat(LegacyLogSecrets.redact(line)).isEqualTo(PREFIX +
                "Send Tor control command: ADD_ONION ED25519-V3:[hidden] Port=9999,127.0.0.1:8000\n");
    }

    // Versions 2.1.3 to 2.1.6 kept the carriage return of the command in the message
    @Test
    void privateKeyOfALineOfVersion213IsHidden() {
        String line = PREFIX + "Send Tor control command: ADD_ONION ED25519-V3:" + PRIVATE_KEY + " Port=9999,127.0.0.1:8000\r\n";

        assertThat(LegacyLogSecrets.redact(line)).doesNotContain(PRIVATE_KEY).contains("ED25519-V3:[hidden] Port=9999");
    }

    @Test
    void privateKeysOfAllIdentitiesAreHidden() {
        String otherKey = "Z".repeat(86) + "==";
        String log = PREFIX + "Send Tor control command: ADD_ONION ED25519-V3:" + PRIVATE_KEY + " Port=1\n" +
                PREFIX + "Other line\n" +
                PREFIX + "Send Tor control command: ADD_ONION ED25519-V3:" + otherKey + " Port=2\n";

        String redacted = LegacyLogSecrets.redact(log);

        assertThat(redacted).doesNotContain(PRIVATE_KEY).doesNotContain(otherKey).contains(PREFIX + "Other line\n");
    }

    @Test
    void linesWithoutPrivateKeyStayUnchanged() {
        String log = PREFIX + "Send Tor control command: ADD_ONION NEW:ED25519-V3 Port=9999,127.0.0.1:8000\n" +
                PREFIX + "Send Tor control command: ADD_ONION ED25519-V3:[hidden] Port=9999,127.0.0.1:8000\n" +
                PREFIX + "Tor control reply: 250-ServiceID=abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuvw\n";

        assertThat(LegacyLogSecrets.redact(log)).isEqualTo(log);
    }
}
