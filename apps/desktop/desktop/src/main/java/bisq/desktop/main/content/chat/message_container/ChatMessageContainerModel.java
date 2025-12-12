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

package bisq.desktop.main.content.chat.message_container;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.user.profile.UserProfile;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class ChatMessageContainerModel implements bisq.desktop.common.view.Model {
    private final ChatChannelDomain chatChannelDomain;
    private final ObjectProperty<ChatChannel<? extends ChatMessage>> selectedChannel = new SimpleObjectProperty<>();
    private final StringProperty textInput = new SimpleStringProperty("");
    private final BooleanProperty shouldShowUserProfileSelection = new SimpleBooleanProperty();
    private final BooleanProperty shouldShowUserProfile = new SimpleBooleanProperty();
    private final ObjectProperty<UserProfile> myUserProfile = new SimpleObjectProperty<>();
    private final ObjectProperty<Boolean> focusInputTextField = new SimpleObjectProperty<>();
    private final ObservableList<UserProfile> mentionableUsers = FXCollections.observableArrayList();
    private final BooleanProperty chatDialogEnabled = new SimpleBooleanProperty(true);
    private final IntegerProperty caretPosition = new SimpleIntegerProperty();

    public ChatMessageContainerModel(ChatChannelDomain chatChannelDomain) {
        this.chatChannelDomain = chatChannelDomain;
    }
}
