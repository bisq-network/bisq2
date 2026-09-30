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

public final class MarkdownSecurityUtils {
    private MarkdownSecurityUtils() {
    }

    public static boolean isSafeOpenUrl(String rawUrl) {
        return isSafeHttpUrl(rawUrl);
    }

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
            return uri.getHost() != null;
        } catch (Exception ignored) {
            return false;
        }
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
