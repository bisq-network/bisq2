package bisq.bisq_easy;

import bisq.chat.ChatChannelDomain;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ChatChannelDomainNavigationTargetMapper {

    public static Optional<NavigationTarget> fromChatChannelDomain(ChatChannelDomain chatChannelDomain) {
        return switch (chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK, BISQ_EASY_OPEN_TRADES -> Optional.of(NavigationTarget.BISQ_EASY);
            case DISCUSSION -> Optional.of(NavigationTarget.CHAT);
            case SUPPORT -> Optional.of(NavigationTarget.SUPPORT);
            default -> Optional.empty();
        };

    }

    public static Set<ChatChannelDomain> fromNavigationTarget(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case BISQ_EASY -> Set.of(ChatChannelDomain.BISQ_EASY_OFFERBOOK,
                    ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
            case BISQ_EASY_OFFERBOOK -> Set.of(ChatChannelDomain.BISQ_EASY_OFFERBOOK);
            case BISQ_EASY_OPEN_TRADES -> Set.of(ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
            case CHAT -> Set.of(ChatChannelDomain.DISCUSSION);
            case SUPPORT -> Set.of(ChatChannelDomain.SUPPORT);
            default -> new HashSet<>();
        };
    }
}
