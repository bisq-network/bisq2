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

package bisq.desktop.primary.main.content.social.chat;

import bisq.desktop.components.table.FilterBox;
import bisq.desktop.primary.main.content.social.ChatView;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericChatView extends ChatView {

    public GenericChatView(GenericChatModel model,
                           GenericChatController controller,
                           Pane userProfileSelection,
                           Pane marketChannelSelection,
                           Pane privateChannelSelection,
                           Pane chatMessagesComponent,
                           Pane notificationsSettings,
                           Pane channelInfo,
                           FilterBox filterBox) {
        super(model,
                controller,
                userProfileSelection,
                marketChannelSelection,
                privateChannelSelection,
                chatMessagesComponent,
                notificationsSettings,
                channelInfo,
                filterBox);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
    }
}
