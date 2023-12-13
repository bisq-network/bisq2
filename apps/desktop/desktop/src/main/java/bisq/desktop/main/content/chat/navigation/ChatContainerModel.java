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

package bisq.desktop.main.content.chat.navigation;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannelDomain;
import bisq.desktop.main.content.ContentTabModel;
import bisq.desktop.main.content.common_chat.Channel;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.Getter;

import java.util.HashMap;

@Getter
public class ChatContainerModel extends ContentTabModel {
    private final ChatChannelDomain chatChannelDomain;
    final ObservableMap<String, Channel> channels = FXCollections.observableMap(new HashMap<>());
    ObjectProperty<Channel> selectedChannel = new SimpleObjectProperty<>();
    private final BooleanProperty hasSelectedChannel = new SimpleBooleanProperty();
    Channel previousSelectedChannel;
    private final StringProperty searchText = new SimpleStringProperty();

    public ChatContainerModel(ChatChannelDomain chatChannelDomain) {
        this.chatChannelDomain = chatChannelDomain;
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return channels.isEmpty()
                ? NavigationTarget.NONE
                : channels.values().stream().findFirst().get().getNavigationTarget();
    }

    public NavigationTarget getPrivateChatsNavigationTarget() {
        if (chatChannelDomain == ChatChannelDomain.DISCUSSION) {
            return NavigationTarget.DISCUSSION_PRIVATECHATS;
        } else if (chatChannelDomain == ChatChannelDomain.EVENTS) {
            return NavigationTarget.EVENTS_PRIVATECHATS;
        }
        return NavigationTarget.SUPPORT_PRIVATECHATS;
    }
}
