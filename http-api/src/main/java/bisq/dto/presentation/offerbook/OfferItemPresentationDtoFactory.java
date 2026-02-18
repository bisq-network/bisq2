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

package bisq.dto.presentation.offerbook;


import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.dto.DtoMappings;
import bisq.dto.offer.bisq_easy.BisqEasyOfferDto;
import bisq.dto.user.profile.UserProfileDto;
import bisq.dto.user.reputation.ReputationScoreDto;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class OfferItemPresentationDtoFactory {
    /**
     * Attempts to create an {@link OfferItemPresentationDto} for the given offerbook message.
     *
     * <p>Returns {@link Optional#empty()} when required data is transiently unavailable — most
     * commonly when the author's {@link UserProfile} has not yet been received via the P2P gossip
     * layer.  Callers must not treat an empty result as an error; they should simply skip the
     * offer for the current snapshot.  Note: {@code OffersWebSocketService} only observes
     * {@code channel.getChatMessages()} — there is no listener on {@code UserProfileService}
     * that re-emits skipped offers when profiles arrive, so the offer will only appear on the
     * next full subscription (REPLACE) or when the client re-subscribes.</p>
     *
     * @param userProfileService     service used to resolve the offer author's user profile
     * @param userIdentityService    service used to determine whether the offer is the local user's own offer
     * @param reputationService      service used to look up the offer author's reputation score
     * @param marketPriceService     service used to format price-related fields
     * @param bisqEasyOfferbookMessage the offerbook message carrying the offer
     * @return an {@link Optional} containing the presentation DTO, or empty if a required
     *         dependency (e.g. the author's user profile) is not yet available
     */
    public static Optional<OfferItemPresentationDto> create(UserProfileService userProfileService,
                                                            UserIdentityService userIdentityService,
                                                            ReputationService reputationService,
                                                            MarketPriceService marketPriceService,
                                                            BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
        BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().orElseThrow();
        String authorUserProfileId = bisqEasyOfferbookMessage.getAuthorUserProfileId();

        // The author's UserProfile may not yet be available if the P2P gossip network has not
        // delivered the profile data.  Returning empty rather than throwing allows callers to
        // skip this offer gracefully and serve the remaining valid offers to subscribers.
        Optional<UserProfile> userProfileOpt = userProfileService.findUserProfile(authorUserProfileId);
        if (userProfileOpt.isEmpty()) {
            log.debug("User profile not found for authorUserProfileId={}, skipping offer creation", authorUserProfileId);
            return Optional.empty();
        }

        boolean isMyOffer = bisqEasyOfferbookMessage.isMyMessage(userIdentityService);
        BisqEasyOfferDto bisqEasyOfferDto = DtoMappings.BisqEasyOfferMapping.fromBisq2Model(bisqEasyOffer);

        // For now, we send also the formatted values as we have not the complex formatters in mobile impl. yet.
        // We might need to replicate the formatters anyway later and then those fields could be removed
        long date = bisqEasyOfferbookMessage.getDate();
        String formattedDate = DateFormatter.formatDateTime(new Date(date), DateFormat.MEDIUM, DateFormat.SHORT,
                true, " " + Res.get("temporal.at") + " ");
        AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
        PriceSpec priceSpec = bisqEasyOffer.getPriceSpec();
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        Market market = bisqEasyOffer.getMarket();
        String formattedQuoteAmount = OfferAmountFormatter.formatQuoteAmount(
                marketPriceService,
                amountSpec,
                priceSpec,
                market,
                hasAmountRange,
                true
        );
        String formattedBaseAmount = OfferAmountFormatter.formatBaseAmount(
                marketPriceService,
                amountSpec,
                priceSpec,
                market,
                hasAmountRange,
                true,
                false
        );
        String formattedPrice = PriceUtil.findQuote(marketPriceService, bisqEasyOffer)
                .map(PriceFormatter::format)
                .orElse("");
        String formattedPriceSpec = PriceSpecFormatter.getFormattedPriceSpec(priceSpec, true);
        List<String> quoteSidePaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getQuoteSidePaymentMethodSpecs())
                .stream()
                .map(PaymentMethod::getPaymentRailName)
                .collect(Collectors.toList());
        List<String> baseSidePaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getBaseSidePaymentMethodSpecs())
                .stream()
                .map(PaymentMethod::getPaymentRailName)
                .collect(Collectors.toList());

        UserProfileDto userProfileDto = DtoMappings.UserProfileMapping.fromBisq2Model(userProfileOpt.get());
        ReputationScore reputationScore = reputationService.getReputationScore(authorUserProfileId);
        ReputationScoreDto reputationScoreDto = DtoMappings.ReputationScoreMapping.fromBisq2Model(reputationScore);
        return Optional.of(new OfferItemPresentationDto(bisqEasyOfferDto,
                isMyOffer,
                userProfileDto,
                formattedDate,
                formattedQuoteAmount,
                formattedBaseAmount,
                formattedPrice,
                formattedPriceSpec,
                quoteSidePaymentMethods,
                baseSidePaymentMethods,
                reputationScoreDto));
    }
}