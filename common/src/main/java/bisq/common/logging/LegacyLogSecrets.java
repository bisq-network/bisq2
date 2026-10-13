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

import java.util.List;
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

    // The rest of a line ends before the line break, so a line end of Windows keeps its carriage return
    private static final String REST_OF_LINE = "[^\\r\\n]*";

    // Versions 2.1.3 to 2.1.13 logged each Tor control command, for example
    // "Send Tor control command: ADD_ONION ED25519-V3:<base64 private key> Port=...". The key type stays readable.
    // A key blob has at least 20 characters, so "ADD_ONION NEW:ED25519-V3", which carries no key, stays unchanged.
    private static final Pattern ADD_ONION_PRIVATE_KEY = Pattern.compile("(ADD_ONION\\s+[A-Za-z0-9-]+:)[A-Za-z0-9+/=]{20,}");

    // The API of versions 2.1.3 to 2.1.13 (module http-api, from 2.1.9 api) logged whole WebSocket messages and the
    // bodies of REST requests and responses. They carry the EC private keys of user profiles: the key material of a new
    // profile (GET /user-identities/key-material, then POST /user-identities), and the key bundle of the identity in
    // each trade and trade chat, which the topic of trades sends to each subscriber. The key bundle also holds the
    // onion and the I2P private key. These lines also carry trade, chat and payment account data, so the rest of the
    // line after the start of the message is hidden. The class name of the logger precedes the message, so a line of
    // another class with the same text stays unchanged. "httpResponse (GET <url>) <status>" of versions 2.1.3 to 2.1.8
    // logged a response without its body and stays unchanged.
    private static final Pattern API_PAYLOAD = Pattern.compile("((?:" + String.join("|",
            "WebSocketConnectionHandler: Received message ",
            "WebSocketConnectionHandler: No service found for handling message: ",
            "WebSocketRestApiRequest: Json deserialization failed\\. Message=",
            "SubscriptionRequest: Json deserialization failed\\. Message=",
            "WebSocketRestApiService: REST API call failed for request: ",
            "WebSocketRestApiService: httpResponse WebSocketRestApiResponse\\(",
            "WebSocketRestApiService: Error at sending a '[^'\\r\\n]*' request to '[^'\\r\\n]*' with body: ",
            "BaseWebSocketService: Sending json with modificationType [^\\r\\n]*?\\. json=",
            "Subscriber: Send json: ",
            "SubscriptionService: Send SubscriptionResponse json: ") + "))" + REST_OF_LINE);

    // "Message was not sent to websocket.\nJson=<request>" of WebSocketRestApiService continues in the next line, which
    // starts with "Json=". No other message of these versions starts a line with it. Only \n starts a line
    // (UNIX_LINES): read as ISO-8859-1, a byte of a UTF-8 sequence can be U+0085, which Java treats as a line end
    // otherwise.
    private static final Pattern API_PAYLOAD_IN_NEXT_LINE = Pattern.compile("(?md)^(Json=)" + REST_OF_LINE);

    // For a line which the patterns above do not know: the value of a private key in the JSON of the API, also when
    // the JSON is escaped within other JSON. "privateKey":{"encoded":...} is the key of a key pair, "privateKeyEncoded"
    // the onion key and "identityBytes" the I2P key. A Base64 value contains no quote and no backslash.
    private static final String QUOTE = "\\\\*\"";
    private static final Pattern API_PRIVATE_KEY_VALUE = Pattern.compile("(" +
            QUOTE + "(?:privateKeyEncoded|identityBytes)" + QUOTE + "\\s*:\\s*" + QUOTE + "|" +
            QUOTE + "privateKey" + QUOTE + "\\s*:\\s*\\{\\s*" + QUOTE + "encoded" + QUOTE + "\\s*:\\s*" + QUOTE +
            ")[^\"\\\\\\r\\n]*");

    private static final List<Pattern> PATTERNS = List.of(ADD_ONION_PRIVATE_KEY,
            API_PAYLOAD,
            API_PAYLOAD_IN_NEXT_LINE,
            API_PRIVATE_KEY_VALUE);

    private LegacyLogSecrets() {
    }

    // Each pattern keeps its first group and replaces the secret after it
    public static String redact(String logContent) {
        String redacted = logContent;
        for (Pattern pattern : PATTERNS) {
            redacted = pattern.matcher(redacted).replaceAll("$1" + Matcher.quoteReplacement(HIDDEN));
        }
        return redacted;
    }
}
