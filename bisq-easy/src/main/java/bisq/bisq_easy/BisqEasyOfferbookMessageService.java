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
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.map.HashMapObserver;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class BisqEasyOfferbookMessageService implements Service {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BannedUserService bannedUserService;
    private final BisqEasySellersReputationBasedTradeAmountService bisqEasySellersReputationBasedTradeAmountService;
    private final MarketPriceService marketPriceService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    // Advances whenever an input of isValid changes without a chat message event (market prices,
    // ignored or banned profiles, a maker's reputation score), so consumers which cache results
    // of isValid, getOffers or getAllOffers recompute.
    private final Observable<Long> offerValidityRevision = new Observable<>(0L);
    @Nullable
    private Pin marketPriceByCurrencyMapPin, ignoredUserProfileIdsPin, bannedUserProfileDataSetPin,
            scoreByUserProfileIdPin;

    public BisqEasyOfferbookMessageService(ChatService chatService,
                                           UserService userService,
                                           BisqEasySellersReputationBasedTradeAmountService bisqEasySellersReputationBasedTradeAmountService,
                                           MarketPriceService marketPriceService) {
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bannedUserService = userService.getBannedUserService();
        userProfileService = userService.getUserProfileService();
        reputationService = userService.getReputationService();
        this.bisqEasySellersReputationBasedTradeAmountService = bisqEasySellersReputationBasedTradeAmountService;
        this.marketPriceService = marketPriceService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        // BisqEasySellersReputationBasedTradeAmountService is initialized first, so its cache is
        // invalidated before consumers of the revision re-evaluate isValid.
        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap()
                .addObserver(this::incrementOfferValidityRevision);
        ignoredUserProfileIdsPin = userProfileService.getIgnoredUserProfileIds()
                .addObserver(this::incrementOfferValidityRevision);
        bannedUserProfileDataSetPin = bannedUserService.getBannedUserProfileDataSet()
                .addObserver(this::incrementOfferValidityRevision);
        scoreByUserProfileIdPin = reputationService.getScoreByUserProfileId().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String userProfileId, Long score) {
                if (isMakerOfAnyOffer(userProfileId)) {
                    incrementOfferValidityRevision();
                }
            }

            @Override
            public void putAll(Map<? extends String, ? extends Long> map) {
                // Fired at registration with all known scores; one revision instead of one per score.
                incrementOfferValidityRevision();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        Stream.of(marketPriceByCurrencyMapPin, ignoredUserProfileIdsPin, bannedUserProfileDataSetPin, scoreByUserProfileIdPin)
                .filter(Objects::nonNull)
                .forEach(Pin::unbind);
        marketPriceByCurrencyMapPin = null;
        ignoredUserProfileIdsPin = null;
        bannedUserProfileDataSetPin = null;
        scoreByUserProfileIdPin = null;
        return CompletableFuture.completedFuture(true);
    }

    public ReadOnlyObservable<Long> getOfferValidityRevision() {
        return offerValidityRevision;
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

    private boolean isMakerOfAnyOffer(String userProfileId) {
        return bisqEasyOfferbookChannelService.getChannels().stream()
                .flatMap(BisqEasyOfferbookChannel::getBisqEasyOfferbookMessagesWithOffer)
                .flatMap(message -> message.getBisqEasyOffer().stream())
                .anyMatch(offer -> userProfileId.equals(offer.getMakersUserProfileId()));
    }

    private synchronized void incrementOfferValidityRevision() {
        offerValidityRevision.set(offerValidityRevision.get() + 1);
    }
}
