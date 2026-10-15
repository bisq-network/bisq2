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

package bisq.desktop.main.content.chat.message_container.components;

import java.util.Optional;

/**
 * Finds the mention token the caret is currently inside. The single parsed match is shared by
 * the popup filter and the completion so both always agree on the replaced range.
 */
public final class ChatMentionParser {
    private static final char INDICATOR = '@';

    public record MentionMatch(String query, int indicatorIndex, int caretPosition) {
    }

    public static Optional<MentionMatch> findMentionAtCaret(String text, int caretPosition) {
        if (text == null || caretPosition < 1 || caretPosition > text.length()) {
            return Optional.empty();
        }
        int indicatorIndex = -1;
        for (int i = caretPosition - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == INDICATOR) {
                indicatorIndex = i;
                break;
            }
            if (!isTokenCharacter(c)) {
                return Optional.empty();
            }
        }
        if (indicatorIndex < 0) {
            return Optional.empty();
        }
        if (indicatorIndex > 0 && !Character.isWhitespace(text.charAt(indicatorIndex - 1))) {
            return Optional.empty();
        }
        // The completion never replaces past the caret: text right after the caret can be a
        // pre-existing word the user typed the mention in front of, and deleting it silently
        // would be worse than leaving a token remainder behind (the editor-standard behavior).
        return Optional.of(new MentionMatch(text.substring(indicatorIndex + 1, caretPosition), indicatorIndex, caretPosition));
    }

    private static boolean isTokenCharacter(char c) {
        return Character.isLetterOrDigit(c) && c < 128;
    }

    private ChatMentionParser() {
    }
}
