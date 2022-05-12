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

package bisq.desktop.primary.main.content.discussion;

import bisq.desktop.components.table.FilterBox;
import bisq.desktop.primary.main.content.ChatView;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiscussionsView extends ChatView {
    public DiscussionsView(DiscussionsModel model,
                           DiscussionsController controller,
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
}
