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

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.user.identity.UserIdentityService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@Getter
public class ChatMessagesListModel implements bisq.desktop.common.view.Model {
    private final UserIdentityService userIdentityService;
    private final ObjectProperty<ChatChannel<?>> selectedChannel = new SimpleObjectProperty<>();
    private final ObservableList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> chatMessages = FXCollections.observableArrayList();
    private final FilteredList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> filteredChatMessages = new FilteredList<>(chatMessages);
    private final SortedList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> sortedChatMessages = new SortedList<>(filteredChatMessages);
    private final Set<String> chatMessageIds = new HashSet<>();
    private final BooleanProperty layoutChildrenDone = new SimpleBooleanProperty();
    private final BooleanProperty hasBisqEasyOfferMessages = new SimpleBooleanProperty(false);

    private final BooleanProperty isPublicChannel = new SimpleBooleanProperty();
    private final ChatChannelDomain chatChannelDomain;
    @Setter
    private Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> searchPredicate = e -> true;
    @Setter
    private boolean autoScrollToBottom;
    @Setter
    private int numReadMessages;
    @Setter
    private boolean hasExpiredMessagesIndicator;
    private final BooleanProperty hasUnreadMessages = new SimpleBooleanProperty();
    private final StringProperty numUnReadMessages = new SimpleStringProperty();
    private final BooleanProperty showScrolledDownButton = new SimpleBooleanProperty();
    private final BooleanProperty scrollBarVisible = new SimpleBooleanProperty();
    private final DoubleProperty scrollValue = new SimpleDoubleProperty();
    private final StringProperty placeholderTitle = new SimpleStringProperty();
    private final StringProperty placeholderDescription = new SimpleStringProperty();

    public ChatMessagesListModel(UserIdentityService userIdentityService,
                                 ChatChannelDomain chatChannelDomain) {
        this.userIdentityService = userIdentityService;
        this.chatChannelDomain = chatChannelDomain;
    }

    boolean isMyMessage(ChatMessage chatMessage) {
        return chatMessage.isMyMessage(userIdentityService);
    }
}
