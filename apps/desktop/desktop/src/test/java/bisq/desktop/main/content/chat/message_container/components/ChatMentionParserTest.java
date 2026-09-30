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

import bisq.desktop.main.content.chat.message_container.components.ChatMentionParser.MentionMatch;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatMentionParserTest {
    @Test
    void mentionAtTheEndOfTheText() {
        assertEquals(Optional.of(new MentionMatch("jo", 0, 3)),
                ChatMentionParser.findMentionAtCaret("@jo", 3));
    }

    @Test
    void mentionBeforeExistingTextIsFound() {
        // The reported bug: the caret sits right after "@jo" while more text follows.
        assertEquals(Optional.of(new MentionMatch("jo", 0, 3)),
                ChatMentionParser.findMentionAtCaret("@jo hello", 3));
    }

    @Test
    void mentionInTheMiddleOfASentence() {
        assertEquals(Optional.of(new MentionMatch("jo", 6, 9)),
                ChatMentionParser.findMentionAtCaret("hello @jo world", 9));
    }

    @Test
    void matchNeverExtendsPastTheCaret() {
        // "@al" typed directly in front of an existing word: the word must not become part of
        // the replaced range, or completion would silently delete it.
        assertEquals(Optional.of(new MentionMatch("al", 0, 3)),
                ChatMentionParser.findMentionAtCaret("@alworld", 3));
        // Caret inside a token: only the part before the caret is replaced.
        assertEquals(Optional.of(new MentionMatch("j", 0, 2)),
                ChatMentionParser.findMentionAtCaret("@joe", 2));
    }

    @Test
    void indicatorAfterNewLine() {
        assertEquals(Optional.of(new MentionMatch("jo", 3, 6)),
                ChatMentionParser.findMentionAtCaret("hi\n@jo", 6));
    }

    @Test
    void bareIndicatorMatchesWithEmptyQuery() {
        assertEquals(Optional.of(new MentionMatch("", 0, 1)),
                ChatMentionParser.findMentionAtCaret("@", 1));
    }

    @Test
    void indicatorNotPrecededByWhitespaceDoesNotMatch() {
        assertTrue(ChatMentionParser.findMentionAtCaret("x@jo", 4).isEmpty());
        assertTrue(ChatMentionParser.findMentionAtCaret("a@jo", 4).isEmpty());
        assertTrue(ChatMentionParser.findMentionAtCaret("@a@b", 4).isEmpty());
    }

    @Test
    void caretBeforeTheIndicatorDoesNotMatch() {
        assertTrue(ChatMentionParser.findMentionAtCaret("@jo", 0).isEmpty());
    }

    @Test
    void nonWordCharacterBetweenIndicatorAndCaretDoesNotMatch() {
        assertTrue(ChatMentionParser.findMentionAtCaret("@j-o", 4).isEmpty());
    }

    @Test
    void transientCaretPositionsAreTolerated() {
        // JavaFX can notify the text before the caret is clamped to the new length.
        assertTrue(ChatMentionParser.findMentionAtCaret("@jo", 5).isEmpty());
        assertTrue(ChatMentionParser.findMentionAtCaret("@jo", -1).isEmpty());
        assertTrue(ChatMentionParser.findMentionAtCaret("", 0).isEmpty());
        assertTrue(ChatMentionParser.findMentionAtCaret(null, 0).isEmpty());
    }
}
