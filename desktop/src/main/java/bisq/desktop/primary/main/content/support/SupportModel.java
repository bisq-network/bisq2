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

package bisq.desktop.primary.main.content.support;

import bisq.chat.channel.ChannelDomain;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.BaseChatModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class SupportModel extends BaseChatModel {
    private final ChannelDomain channelDomain;

    public SupportModel(ChannelDomain channelDomain) {
        this.channelDomain = channelDomain;
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.NONE;
    }

}
