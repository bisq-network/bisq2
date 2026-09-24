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

import bisq.chat.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

// The displayed items of a channel by message ID. Two items can share an ID, as the mediator keeps both traders'
// versions of a forwarded message, so content is compared only among the items with that ID. Messages are
// compared at lookup and never hashed, as the hash of a private chat message changes when a reaction is added.
final class ChatMessageListItemsById<T> {
    private final Function<T, ChatMessage> chatMessageOfItem;
    private final Map<String, List<T>> itemsById = new HashMap<>();

    ChatMessageListItemsById(Function<T, ChatMessage> chatMessageOfItem) {
        this.chatMessageOfItem = chatMessageOfItem;
    }

    boolean contains(ChatMessage chatMessage) {
        return itemsById.getOrDefault(chatMessage.getId(), List.of()).stream()
                .anyMatch(item -> chatMessageOfItem.apply(item).equals(chatMessage));
    }

    void add(T item) {
        itemsById.computeIfAbsent(chatMessageOfItem.apply(item).getId(), id -> new ArrayList<>()).add(item);
    }

    // Removes the item of the exact message, or else the first item with its ID.
    Optional<T> remove(ChatMessage chatMessage) {
        List<T> items = itemsById.get(chatMessage.getId());
        if (items == null) {
            return Optional.empty();
        }
        int index = IntStream.range(0, items.size())
                .filter(i -> chatMessageOfItem.apply(items.get(i)).equals(chatMessage))
                .findFirst()
                .orElse(0);
        T item = items.remove(index);
        if (items.isEmpty()) {
            itemsById.remove(chatMessage.getId());
        }
        return Optional.of(item);
    }

    void clear() {
        itemsById.clear();
    }

    boolean isEmpty() {
        return itemsById.isEmpty();
    }
}
