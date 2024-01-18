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

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.main.content.chat.common.ChannelTabButtonModel;

import java.util.Comparator;
import java.util.Map;

public class ChatUtil {
    public static final Comparator<ChannelTabButtonModel> DEFAULT_CHANNEL_TAB_BUTTON_COMPARATOR =
            (lhs, rhs) -> lhs.getChannelTitle().compareToIgnoreCase(rhs.getChannelTitle());

    public static final Comparator<ChannelTabButtonModel> SUPPORT_CHANNEL_TAB_BUTTON_COMPARATOR = new Comparator<>() {
        private final Map<NavigationTarget, Integer> orderMap = Map.of(
                NavigationTarget.SUPPORT_SUPPORT, 0,
                NavigationTarget.SUPPORT_QUESTIONS, 1,
                NavigationTarget.SUPPORT_REPORTS, 2
        );

        @Override
        public int compare(ChannelTabButtonModel lhs, ChannelTabButtonModel rhs) {
            Integer lhsOrder = orderMap.getOrDefault(lhs.getNavigationTarget(), Integer.MAX_VALUE);
            Integer rhsOrder = orderMap.getOrDefault(rhs.getNavigationTarget(), Integer.MAX_VALUE);
            return lhsOrder.compareTo(rhsOrder);
        }
    };

    public static String getChannelIconId(String channelId) {
        return "channels-" + channelId.replace(".", "-");
    }
}
