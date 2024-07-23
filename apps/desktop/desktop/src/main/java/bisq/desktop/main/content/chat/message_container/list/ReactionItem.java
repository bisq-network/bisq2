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
import javafx.beans.property.SimpleIntegerProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.LinkedHashSet;

@Slf4j
@Getter
public class ReactionItem {
    private final Reaction reaction;
    private final String iconId;
    private long firstAdded;
    private final SimpleIntegerProperty count = new SimpleIntegerProperty(0);
    private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
    private final LinkedHashSet<UserProfile> users = new LinkedHashSet<>();
    private UserProfile selectedUserProfile;

    ReactionItem(Reaction reaction, UserProfile selectedUserProfile) {
        this.reaction = reaction;
        this.selectedUserProfile = selectedUserProfile;
        this.iconId = reaction.toString().replace("_", "").toLowerCase();
    }

    public boolean hasActiveReactions() {
        return !users.isEmpty();
    }

    public String getCountAsString() {
        long count = users.size();
        if (count < 2) {
            return "";
        }
        return count < 100 ? String.valueOf(count) : "+99";
    }

    public static Comparator<ReactionItem> firstAddedComparator() {
        return Comparator.comparingLong(ReactionItem::getFirstAdded);
    }

    void addUser(ChatMessageReaction chatMessageReaction, UserProfile userProfile) {
        if (hasReactionBeenRemoved(chatMessageReaction)) {
            return;
        }

        if (firstAdded == 0) {
            firstAdded = chatMessageReaction.getDate();
        }

        users.add(userProfile);
        count.set(users.size());
        updateSelected();
    }

    void removeUser(UserProfile userProfile) {
        users.remove(userProfile);
        count.set(users.size());
        updateSelected();
    }

    void setSelectedUserProfile(UserProfile selectedUserProfile) {
        this.selectedUserProfile = selectedUserProfile;
        updateSelected();
    }

    private boolean hasReactionBeenRemoved(ChatMessageReaction chatMessageReaction) {
        return (chatMessageReaction instanceof PrivateChatMessageReaction
                && ((PrivateChatMessageReaction) chatMessageReaction).isRemoved());
    }

    private void updateSelected() {
        selected.set(getUsers().contains(selectedUserProfile));
    }
}
