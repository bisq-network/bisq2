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
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.desktop.common.view.NavigationModel;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
@Getter
public abstract class BaseChatModel extends NavigationModel {
    protected final ChatChannelDomain chatChannelDomain;
    private final StringProperty selectedChatMessages = new SimpleStringProperty("");
    private final StringProperty channelTitle = new SimpleStringProperty("");
    private final StringProperty channelDescription = new SimpleStringProperty("");
    private final StringProperty channelIconId = new SimpleStringProperty("");
    private final ObjectProperty<ChatChannel<? extends ChatMessage>> selectedChannel = new SimpleObjectProperty<>();
    private final BooleanProperty sideBarVisible = new SimpleBooleanProperty();
    private final BooleanProperty sideBarChanged = new SimpleBooleanProperty();
    private final DoubleProperty sideBarWidth = new SimpleDoubleProperty();
    private final BooleanProperty channelSidebarVisible = new SimpleBooleanProperty();
    private final String helpTitle;
    private final StringProperty searchText = new SimpleStringProperty();
    private final ObjectProperty<ChatChannelNotificationType> selectedNotificationSetting = new SimpleObjectProperty<>(ChatChannelNotificationType.GLOBAL_DEFAULT);

    public BaseChatModel(ChatChannelDomain chatChannelDomain) {
        this.chatChannelDomain = chatChannelDomain;

        switch (chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK:
            case BISQ_EASY_OPEN_TRADES:
                helpTitle = Res.get("chat.topMenu.tradeGuide.tooltip");
                break;
            case DISCUSSION:
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
