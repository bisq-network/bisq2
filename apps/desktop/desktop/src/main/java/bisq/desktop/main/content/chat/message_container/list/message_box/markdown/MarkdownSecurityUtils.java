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

package bisq.desktop.main.content.chat.message_container.list.message_box.markdown;

import java.net.URI;
import java.util.Locale;

/**
 * The rules for a safe link are specified in {@code docs/specifications/chat/safe-links.md}.
 */
public final class MarkdownSecurityUtils {
    private MarkdownSecurityUtils() {
    }

    public static boolean isSafeOpenUrl(String rawUrl) {
        return isSafeHttpUrl(rawUrl);
    }

    /**
     * A URL is safe when it has no control or bidi control character, is a valid URI, has the scheme http or https,
     * and has an authority of the form {@code host[:port]}: a host, and no user info in front of an {@code @}.
     */
    public static boolean isSafeHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        if (containsDangerousCharacters(url)) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
                return false;
            }
            if (uri.getHost() == null) {
                return false;
            }
            return !hasUserInfo(uri);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * java.net.URI parses {@code https://bisq.network@evil.example/} with the user info {@code bisq.network} and the
     * host {@code evil.example}. The reader takes the text in front of the {@code @} for the host. A link to a web
     * page needs no user info, so a URL with user info, or with any other {@code @} in its authority, is not safe.
     * The authority is not null here, because the URI has a host.
     */
    private static boolean hasUserInfo(URI uri) {
        return uri.getRawUserInfo() != null || uri.getRawAuthority().indexOf('@') >= 0;
    }

    public static boolean containsDangerousCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c)) {
                return true;
            }
            if (isBidiControl(c)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBidiControl(char c) {
        return c == '\u202A'
                || c == '\u202B'
                || c == '\u202C'
                || c == '\u202D'
                || c == '\u202E'
                || c == '\u2066'
                || c == '\u2067'
                || c == '\u2068'
                || c == '\u2069';
    }
}
