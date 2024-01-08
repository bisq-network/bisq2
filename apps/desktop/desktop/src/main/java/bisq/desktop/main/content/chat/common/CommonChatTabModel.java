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

package bisq.desktop.main.content.chat.common;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannelDomain;
import bisq.desktop.main.content.ContentTabModel;
import bisq.desktop.main.content.chat.ChatUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;

@Getter
final class CommonChatTabModel extends ContentTabModel {
    private final ChatChannelDomain chatChannelDomain;
    final ObservableMap<String, ChannelTabButtonModel> channelTabButtonModelByChannelId = FXCollections.observableMap(new HashMap<>());
    ObjectProperty<ChannelTabButtonModel> selectedChannelTabButtonModel = new SimpleObjectProperty<>();
    private final BooleanProperty hasSelectedChannel = new SimpleBooleanProperty();
    ChannelTabButtonModel previousSelectedChannelTabButtonModel;

    CommonChatTabModel(ChatChannelDomain chatChannelDomain) {
        this.chatChannelDomain = chatChannelDomain;
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return channelTabButtonModelByChannelId.isEmpty()
                ? NavigationTarget.NONE
                : channelTabButtonModelByChannelId.values().stream().min(getChannelTabButtonComparator()).get().getNavigationTarget();
    }

    NavigationTarget getPrivateChatsNavigationTarget() {
        if (chatChannelDomain == ChatChannelDomain.DISCUSSION) {
            return NavigationTarget.DISCUSSION_PRIVATECHATS;
        } else if (chatChannelDomain == ChatChannelDomain.EVENTS) {
            return NavigationTarget.EVENTS_PRIVATECHATS;
        }
        return NavigationTarget.SUPPORT_PRIVATECHATS;
    }

    Comparator<ChannelTabButtonModel> getChannelTabButtonComparator() {
        return (chatChannelDomain == ChatChannelDomain.SUPPORT)
                ? ChatUtil.SUPPORT_CHANNEL_TAB_BUTTON_COMPARATOR
                : ChatUtil.DEFAULT_CHANNEL_TAB_BUTTON_COMPARATOR;
    }
}
