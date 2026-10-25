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

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates the forms of Tor control commands and replies which may be written to the log.
 * <p>
 * Commands and replies can carry secrets: the private key of an onion service ({@code ADD_ONION}), the control
 * password or the auth cookie ({@code AUTHENTICATE}), and keys which Tor generates for a reply. Users share their log
 * files, for example in bug reports. So the log form keeps only parts which are known to be public. The arguments of a
 * command which is not listed here are hidden. See {@code docs/specifications/security/secrets-in-logs.md}.
 */
final class TorControlLogFormat {
    static final String HIDDEN = "[hidden]";

    // The arguments of these commands are info keys, onion service IDs, option names and event names.
    private static final Set<String> COMMANDS_WITH_PUBLIC_ARGUMENTS = Set.of("GETINFO", "HSFETCH", "RESETCONF",
            "SETEVENTS", "TAKEOWNERSHIP");
    private static final Set<String> ADD_ONION_KEY_TYPES = Set.of("NEW", "RSA1024", "ED25519-V3");
    // Tor returns generated keys as reply data, for example "250-PrivateKey=...". Only these keys have public values.
    private static final Set<String> REPLY_DATA_KEYS_WITH_PUBLIC_VALUE = Set.of("ServiceID", "net/listeners/socks");
    // Status code, then '-' (mid reply line), '+' (data reply line) or ' ' (end reply line), then the reply text
    private static final Pattern REPLY_LINE_PATTERN = Pattern.compile("^(\\d{3}[-+ ])(.*)$", Pattern.DOTALL);
    // Reply data of the form Key=Value. The value starts after the first '=' and can contain '='.
    private static final Pattern REPLY_DATA_PATTERN = Pattern.compile("^([^ =]+)=.*$", Pattern.DOTALL);

    private TorControlLogFormat() {
    }

    static String toLoggableCommand(String command) {
        String[] tokens = command.strip().split("\\s+");
        String keyword = tokens[0];
        String[] arguments = Arrays.copyOfRange(tokens, 1, tokens.length);
        if (arguments.length == 0 || COMMANDS_WITH_PUBLIC_ARGUMENTS.contains(keyword)) {
            return String.join(" ", tokens);
        }
        if (keyword.equals("ADD_ONION")) {
            return keyword + " " + toLoggableAddOnionArguments(arguments);
        }
        // For example AUTHENTICATE (password or auth cookie) and SETCONF (an option value can be a password)
        return keyword + " " + HIDDEN;
    }

    static String toLoggableReply(String replyLine) {
        Matcher replyLineMatcher = REPLY_LINE_PATTERN.matcher(replyLine);
        if (!replyLineMatcher.matches()) {
            // A data line of a multi-line reply, for example a line of a configuration
            return HIDDEN;
        }
        String statusCodeAndSeparator = replyLineMatcher.group(1);
        String text = replyLineMatcher.group(2);
        Matcher replyDataMatcher = REPLY_DATA_PATTERN.matcher(text);
        if (!replyDataMatcher.matches()) {
            // A status text such as "OK" or an error message of Tor, or an event such as "HS_DESC ..."
            return replyLine;
        }
        String key = replyDataMatcher.group(1);
        return REPLY_DATA_KEYS_WITH_PUBLIC_VALUE.contains(key)
                ? replyLine
                : statusCodeAndSeparator + key + "=" + HIDDEN;
    }

    // ADD_ONION KeyType:KeyBlob [Flags=...] [MaxStreams=...] Port=... [ClientAuth=Name:Blob]
    // The key blob is the private key of the onion service. The log keeps the key type and the port mappings.
    private static String toLoggableAddOnionArguments(String[] arguments) {
        String keyArgument = arguments[0];
        int keyTypeEnd = keyArgument.indexOf(':');
        String keyType = keyTypeEnd < 0 ? "" : keyArgument.substring(0, keyTypeEnd);
        String loggableKeyArgument = ADD_ONION_KEY_TYPES.contains(keyType) ? keyType + ":" + HIDDEN : HIDDEN;
        Stream<String> loggableOtherArguments = Arrays.stream(arguments)
                .skip(1)
                .map(argument -> argument.startsWith("Port=") ? argument : HIDDEN);
        return Stream.concat(Stream.of(loggableKeyArgument), loggableOtherArguments)
                .collect(Collectors.joining(" "));
    }
}
