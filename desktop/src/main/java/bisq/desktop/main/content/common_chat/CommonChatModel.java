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

package bisq.desktop.main.content.common_chat;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.chat.ChatModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CommonChatModel extends ChatModel {
    @Setter
    private TwoPartyPrivateChatChannel selectPrivateTwoPartyChannel;
    private final StringProperty searchText = new SimpleStringProperty();
    private final BooleanProperty isTwoPartyPrivateChatChannelSelectionVisible = new SimpleBooleanProperty();

    public CommonChatModel(ChatChannelDomain chatChannelDomain) {
        super(chatChannelDomain);
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.NONE;
    }
}
