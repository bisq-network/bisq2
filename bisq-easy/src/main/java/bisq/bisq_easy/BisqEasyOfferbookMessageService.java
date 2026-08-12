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

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.offer.amount.OfferAmountUtil;
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
    private final MarketPriceService marketPriceService;
    private final UserProfileService userProfileService;

    public BisqEasyOfferbookMessageService(ChatService chatService,
                                           UserService userService,
                                           BisqEasySellersReputationBasedTradeAmountService bisqEasySellersReputationBasedTradeAmountService,
                                           MarketPriceService marketPriceService) {
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bannedUserService = userService.getBannedUserService();
        userProfileService = userService.getUserProfileService();
        this.bisqEasySellersReputationBasedTradeAmountService = bisqEasySellersReputationBasedTradeAmountService;
        this.marketPriceService = marketPriceService;
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
                        (hasResolvableAmounts(message) &&
                                (isBuyOffer(message) || hasSellerSufficientReputation(message))));
    }

    // An offer whose amounts cannot be resolved on both sides - because a conversion overflows
    // at its price, or because the required market price is missing and the conversions come
    // back empty - cannot be rendered or taken: the rendering paths call orElseThrow on these
    // values. Such an offer is invalid regardless of direction, so no list item is ever
    // constructed from it. Probing both sides covers every conversion the rendering paths
    // perform.
    private boolean hasResolvableAmounts(BisqEasyOfferbookMessage message) {
        return message.getBisqEasyOffer().map(offer -> {
            try {
                return OfferAmountUtil.findBaseSideMinOrFixedAmount(marketPriceService, offer.getAmountSpec(),
                        offer.getPriceSpec(), offer.getMarket()).isPresent()
                        && OfferAmountUtil.findBaseSideMaxOrFixedAmount(marketPriceService, offer.getAmountSpec(),
                        offer.getPriceSpec(), offer.getMarket()).isPresent()
                        && OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, offer.getAmountSpec(),
                        offer.getPriceSpec(), offer.getMarket()).isPresent()
                        && OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, offer.getAmountSpec(),
                        offer.getPriceSpec(), offer.getMarket()).isPresent();
            } catch (ArithmeticException e) {
                log.debug("Offer {} has amounts which cannot be converted at its price", offer.getId());
                return false;
            }
        }).orElse(true);
    }

}
