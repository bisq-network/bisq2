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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Secrets which earlier versions wrote into log files. Log files are appended across versions, so a log file of this
 * version can still contain them (see docs/specifications/security/secrets-in-logs.md).
 */
public final class LegacyLogSecrets {
    public static final String HIDDEN = "[hidden]";
    /**
     * The version of the patterns. LegacyLogFileRedactor checks the log files once per version, so the version has to
     * be increased with each pattern which a released version did not have.
     */
    public static final int VERSION = 1;

    // Versions 2.1.3 to 2.1.13 logged each Tor control command, for example
    // "Send Tor control command: ADD_ONION ED25519-V3:<base64 private key> Port=...". The key type stays readable.
    // A key blob has at least 20 characters, so "ADD_ONION NEW:ED25519-V3", which carries no key, stays unchanged.
    private static final Pattern ADD_ONION_PRIVATE_KEY = Pattern.compile("(ADD_ONION\\s+[A-Za-z0-9-]+:)[A-Za-z0-9+/=]{20,}");

    private LegacyLogSecrets() {
    }

    public static String redact(String logContent) {
        return ADD_ONION_PRIVATE_KEY.matcher(logContent).replaceAll("$1" + Matcher.quoteReplacement(HIDDEN));
    }
}
