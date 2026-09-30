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

package bisq.desktop.main.content.chat.message_container.list;

import bisq.chat.reactions.Reaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageListItemReactionCompatibilityTest {
    @Test
    void resolveReactionFromOrdinalReturnsKnownReactions() {
        assertEquals(Reaction.THUMBS_UP,
                ChatMessageListItem.resolveReactionFromOrdinal(Reaction.THUMBS_UP.ordinal()).orElseThrow());
    }

    @Test
    void resolveReactionFromOrdinalIgnoresUnsupportedFutureReactions() {
        assertFalse(ChatMessageListItem.resolveReactionFromOrdinal(Reaction.values().length).isPresent());
        assertTrue(ChatMessageListItem.resolveReactionFromOrdinal(Reaction.HEART.ordinal()).isPresent());
    }
}
