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

package bisq.bisq_easy;

import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.application.Service;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class BisqEasyOfferbookMessageService implements Service {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BannedUserService bannedUserService;
    private final BisqEasySellersReputationBasedTradeAmountService bisqEasySellersReputationBasedTradeAmountService;
    private final UserProfileService userProfileService;

    public BisqEasyOfferbookMessageService(ChatService chatService,
                                           UserService userService,
                                           BisqEasySellersReputationBasedTradeAmountService bisqEasySellersReputationBasedTradeAmountService) {
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bannedUserService = userService.getBannedUserService();
        userProfileService = userService.getUserProfileService();
        this.bisqEasySellersReputationBasedTradeAmountService = bisqEasySellersReputationBasedTradeAmountService;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    public Stream<BisqEasyOffer> getAllOffers() {
        return bisqEasyOfferbookChannelService.getChannels().stream().flatMap(this::getOffers);
    }

    public Stream<BisqEasyOffer> getOffers(BisqEasyOfferbookChannel channel) {
        return getOfferbookMessagesWithOffer(channel)
                .flatMap(message -> message.getBisqEasyOffer().stream());
    }

    public Stream<BisqEasyOfferbookMessage> getAllOfferbookMessagesWithOffer() {
        return bisqEasyOfferbookChannelService.getChannels().stream()
                .flatMap(this::getOfferbookMessagesWithOffer);
    }

    public Stream<BisqEasyOfferbookMessage> getOfferbookMessagesWithOffer(BisqEasyOfferbookChannel channel) {
        return channel.getBisqEasyOfferbookMessagesWithOffer()
                .filter(this::isValid);
    }

    public Stream<BisqEasyOfferbookMessage> getAllOfferbookMessagesWithOffer(String userProfileId) {
        return getAllOfferbookMessagesWithOffer()
                .filter(message -> message.getAuthorUserProfileId().equals(userProfileId));
    }

    public boolean isNotBanned(BisqEasyOfferbookMessage message) {
        return !bannedUserService.isUserProfileBanned(message.getAuthorUserProfileId());
    }

    public boolean isNotIgnored(BisqEasyOfferbookMessage message) {
        return !userProfileService.isChatUserIgnored(message.getAuthorUserProfileId());
    }

    public boolean hasSellerSufficientReputation(BisqEasyOfferbookMessage message) {
        return bisqEasySellersReputationBasedTradeAmountService.hasSellerSufficientReputation(message);
    }

    public static boolean isBuyOffer(BisqEasyOfferbookMessage message) {
        return message.getBisqEasyOffer().map(offer -> offer.getDirection().isBuy()).orElse(false);
    }

    public static boolean isTextMessage(BisqEasyOfferbookMessage message) {
        return message.getBisqEasyOffer().isEmpty();
    }

    public boolean isValid(BisqEasyOfferbookMessage message) {
        return isNotBanned(message) &&
                isNotIgnored(message) &&
                (isTextMessage(message) ||
                        isBuyOffer(message) ||
                        hasSellerSufficientReputation(message));
    }

}
