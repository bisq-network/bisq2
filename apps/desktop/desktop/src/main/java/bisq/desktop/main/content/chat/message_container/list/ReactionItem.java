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

import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.PrivateChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.user.profile.UserProfile;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReactionItem {
    @EqualsAndHashCode.Include
    private final Reaction reaction;
    private final String iconId;
    private final long firstAdded = 0;
    private final SimpleStringProperty count = new SimpleStringProperty();
    private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
    Set<UserProfile> users = new HashSet<>();

    ReactionItem(Reaction reaction) {
        this.reaction = reaction;
        this.iconId = reaction.toString().replace("_", "").toLowerCase();
    }

    void addUser(ChatMessageReaction chatMessageReaction, UserProfile userProfile) {
        if (hasReactionBeenRemoved(chatMessageReaction)) {
            return;
        }

        users.add(userProfile);
        count.set(getCount());
    }

    void removeUser(UserProfile userProfile) {
        users.remove(userProfile);
        count.set(getCount());
    }

    private String getCount() {
        long count = users.size();
        return count < 100 ? String.valueOf(count) : "+99";
    }

    private boolean hasReactionBeenRemoved(ChatMessageReaction chatMessageReaction) {
        return (chatMessageReaction instanceof PrivateChatMessageReaction
                && ((PrivateChatMessageReaction) chatMessageReaction).isRemoved());
    }
}
