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
