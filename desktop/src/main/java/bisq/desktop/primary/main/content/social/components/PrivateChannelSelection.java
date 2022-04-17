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

package bisq.desktop.primary.main.content.social.components;

import bisq.desktop.common.observable.FxBindings;
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatService;
import bisq.social.chat.PrivateChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrivateChannelSelection extends ChannelSelection {
    public PrivateChannelSelection(ChatService chatService) {
        super(new Controller(chatService, Res.get("social.privateChannels")) {
            @Override
            public void onActivate() {
                // Private channels have the ability to disappear, when their messages have expired.
                // I dont want them to disappear  in front of the user, therefor its done  before display.
                chatService.removeExpiredPrivateMessages();
                super.onActivate();
                channelsPin = FxBindings.<PrivateChannel, Channel<?>> bind(model.channels)
                        .to(chatService.getPrivateChannels());

                selectedChannelPin = FxBindings.subscribe(chatService.getSelectedChannel(),
                        channel -> {
                            if (channel instanceof PrivateChannel) {
                                model.selectedChannel.set(channel);
                            }
                        });
            }
        });
    }
    
}