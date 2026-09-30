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

package bisq.api.web_socket.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {
    /**
     * Clients released before the node derived the request identity from the WebSocket handshake put
     * their session id into the message payload. The node ignores it, but a raw message must never
     * reach a log file with it still readable, so it is blanked before logging.
     *
     * <p>Only the session id is treated as a secret. The client id is an identifier the node logs
     * elsewhere anyway, and keeping it visible is what makes such a log line useful.
     */
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile(
            "(\"Bisq-Session-Id\"\\s*:\\s*\")[^\"]*(\")", Pattern.CASE_INSENSITIVE);
    private static final String REDACTED = "$1***$2";

    public static String redactCredentials(String json) {
        return SESSION_ID_PATTERN.matcher(json).replaceAll(REDACTED);
    }

    // We use by convention same class name. We get the type field set by the client.
    public static boolean hasExpectedJsonClassName(Class<?> clazz, String json) {
        String regex = "\"type\":\\s*\"([^\"]+)\""; // We use simple name
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String type = matcher.group(1);
            String simpleName = clazz.getSimpleName();
            return simpleName.equals(type);
        } else {
            return false;
        }
    }
}
