package bisq.bisq_easy;

import bisq.chat.ChatChannelDomain;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ChatChannelDomainNavigationTargetMapper {

    public static Optional<NavigationTarget> fromChatChannelDomain(ChatChannelDomain chatChannelDomain) {
        switch (chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK:
            case BISQ_EASY_OPEN_TRADES:
                return Optional.of(NavigationTarget.BISQ_EASY);
            case DISCUSSION:
                return Optional.of(NavigationTarget.CHAT);
            case SUPPORT:
                return Optional.of(NavigationTarget.SUPPORT);
            default:
                return Optional.empty();
        }

    }

    public static Set<ChatChannelDomain> fromNavigationTarget(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case BISQ_EASY:
                return Set.of(ChatChannelDomain.BISQ_EASY_OFFERBOOK,
                        ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
            case BISQ_EASY_OFFERBOOK:
                return Set.of(ChatChannelDomain.BISQ_EASY_OFFERBOOK);
            case BISQ_EASY_OPEN_TRADES:
                return Set.of(ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
            case CHAT:
                return Set.of(ChatChannelDomain.DISCUSSION);
            case SUPPORT:
                return Set.of(ChatChannelDomain.SUPPORT);
            default:
                return new HashSet<>();
        }
    }
}
