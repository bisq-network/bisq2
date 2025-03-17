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
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.desktop.main.content.chat.ChatUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class ChannelTabButtonModel {
    @EqualsAndHashCode.Include
    private final ChatChannel<?> chatChannel;
    @EqualsAndHashCode.Include
    private final NavigationTarget navigationTarget;

    private final String channelId;
    private final ChatChannelDomain chatChannelDomain;
    private final String channelTitle;
    private final String iconId;
    @Setter
    private boolean isSelected;

    ChannelTabButtonModel(CommonPublicChatChannel chatChannel,
                          NavigationTarget navigationTarget,
                          CommonPublicChatChannelService chatChannelService) {
        this.chatChannel = chatChannel;
        this.navigationTarget = navigationTarget;

        channelId = chatChannel.getId();
        chatChannelDomain = chatChannel.getChatChannelDomain();
        channelTitle = chatChannelService.getChannelTitle(chatChannel);
        iconId = ChatUtil.getChannelIconId(channelId);
    }
}
