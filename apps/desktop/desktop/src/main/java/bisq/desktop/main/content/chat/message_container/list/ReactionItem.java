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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

@Slf4j
@Getter
public class ReactionItem {
    private final Reaction reaction;
    private final String iconId;
    private long firstAdded;
    private final SimpleIntegerProperty count = new SimpleIntegerProperty(0);
    private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
    private final HashMap<UserProfile, UserWithReactionDate> users = new HashMap<>();
    private final TreeSet<UserWithReactionDate> usersByReactionDate = new TreeSet<>();
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

        UserWithReactionDate userWithReactionDate = new UserWithReactionDate(userProfile, chatMessageReaction.getDate());
        users.put(userProfile, userWithReactionDate);
        usersByReactionDate.add(userWithReactionDate);
        count.set(users.size());
        updateSelected();
        updateFirstAdded();
    }

    void removeUser(UserProfile userProfile) {
        UserWithReactionDate userWithReactionDate = users.get(userProfile);
        if (userWithReactionDate != null) {
            users.remove(userProfile);
            usersByReactionDate.remove(userWithReactionDate);
            count.set(users.size());
            updateSelected();
            updateFirstAdded();
        }
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
        selected.set(getUsers().containsKey(selectedUserProfile));
    }

    private void updateFirstAdded() {
        if (!usersByReactionDate.isEmpty()) {
            firstAdded = usersByReactionDate.first().getDate();
        }
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static final class UserWithReactionDate implements Comparable<UserWithReactionDate> {
        @EqualsAndHashCode.Include
        private final UserProfile userProfile;
        private final long date;

        private UserWithReactionDate(UserProfile userProfile, long date) {
            this.userProfile = userProfile;
            this.date = date;
        }

        @Override
        public int compareTo(UserWithReactionDate other) {
            if (userProfile.equals(other.getUserProfile())) {
                return 0;
            }
            return Long.compare(this.getDate(), other.getDate());
        }
    }
}
