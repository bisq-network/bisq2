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

package bisq.desktop.main.content.chat;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.desktop.common.view.NavigationModel;
import bisq.desktop.main.content.chat.sidebar.UserProfileSidebar;
import bisq.i18n.Res;
import javafx.beans.property.*;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Getter
public abstract class BaseChatModel extends NavigationModel {
    protected final ChatChannelDomain chatChannelDomain;
    private final Map<String, StringProperty> chatMessagesByChannelId = new HashMap<>();
    private final StringProperty selectedChatMessages = new SimpleStringProperty("");
    private final StringProperty channelTitle = new SimpleStringProperty("");
    private final StringProperty channelDescription = new SimpleStringProperty("");
    private final StringProperty channelIconId = new SimpleStringProperty("");
    private final ObjectProperty<ChatChannel<? extends ChatMessage>> selectedChannel = new SimpleObjectProperty<>();
    private final ObjectProperty<Pane> chatUserDetailsRoot = new SimpleObjectProperty<>();
    private final BooleanProperty sideBarVisible = new SimpleBooleanProperty();
    private final BooleanProperty sideBarChanged = new SimpleBooleanProperty();
    private final DoubleProperty sideBarWidth = new SimpleDoubleProperty();
    private final BooleanProperty channelSidebarVisible = new SimpleBooleanProperty();
    private final String helpTitle;
    @Setter
    private Optional<UserProfileSidebar> chatUserDetails = Optional.empty();
    private final StringProperty searchText = new SimpleStringProperty();

    public BaseChatModel(ChatChannelDomain chatChannelDomain) {
        this.chatChannelDomain = chatChannelDomain;

        switch (chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK:
            case BISQ_EASY_OPEN_TRADES:
            case BISQ_EASY_PRIVATE_CHAT:
                helpTitle = Res.get("chat.topMenu.tradeGuide.tooltip");
                break;
            case DISCUSSION:
            case EVENTS:
            case SUPPORT:
            default:
                helpTitle = Res.get("chat.topMenu.chatRules.tooltip");
                break;
        }
    }

    @Nullable
    public ChatChannel<? extends ChatMessage> getSelectedChannel() {
        return selectedChannel.get();
    }

    public ObjectProperty<ChatChannel<? extends ChatMessage>> selectedChannelProperty() {
        return selectedChannel;
    }
}
