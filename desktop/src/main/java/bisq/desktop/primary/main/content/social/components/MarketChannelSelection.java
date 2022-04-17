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
import bisq.social.chat.MarketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketChannelSelection extends ChannelSelection {
    public MarketChannelSelection(ChatService chatService) {
        super(new Controller(chatService, Res.get("social.marketChannels")) {
            @Override
            public void onActivate() {
                super.onActivate();
                channelsPin = FxBindings.<MarketChannel, Channel<?>>bind(model.channels)
                        .to(chatService.getMarketChannels());
            }
        });
    }
}