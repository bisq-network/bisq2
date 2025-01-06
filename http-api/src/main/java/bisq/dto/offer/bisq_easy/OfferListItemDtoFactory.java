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

package bisq.dto.offer.bisq_easy;


import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.currency.Market;
import bisq.dto.DtoMappings;
import bisq.dto.user.profile.UserProfileItemDto;
import bisq.dto.user.reputation.ReputationScoreDto;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
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

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OfferListItemDtoFactory {
    public static OfferListItemDto create(UserProfileService userProfileService,
                                          UserIdentityService userIdentityService,
                                          ReputationService reputationService,
                                          MarketPriceService marketPriceService,
                                          BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
        BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().orElseThrow();
        boolean isMyOffer = bisqEasyOfferbookMessage.isMyMessage(userIdentityService);
        Direction direction = bisqEasyOffer.getDirection();
        String messageId = bisqEasyOfferbookMessage.getId();
        String offerId = bisqEasyOffer.getId();
        BisqEasyOfferDto bisqEasyOfferDto = DtoMappings.BisqEasyOfferMapping.fromBisq2Model(bisqEasyOffer);
        String authorUserProfileId = bisqEasyOfferbookMessage.getAuthorUserProfileId();
        Optional<UserProfile> authorUserProfile = userProfileService.findUserProfile(authorUserProfileId);
        String nym = authorUserProfile.map(UserProfile::getNym).orElse("");
        String userName = authorUserProfile.map(UserProfile::getUserName).orElse("");
        ReputationScoreDto reputationScore = authorUserProfile.flatMap(reputationService::findReputationScore)
                .map(DtoMappings.ReputationScoreMapping::fromBisq2Model)
                .orElse(DtoMappings.ReputationScoreMapping.fromBisq2Model(ReputationScore.NONE));

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
                .map(PaymentMethod::getName)
                .collect(Collectors.toList());
        List<String> baseSidePaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getBaseSidePaymentMethodSpecs())
                .stream()
                .map(PaymentMethod::getName)
                .collect(Collectors.toList());

        //todo use UserProfile and handle reputation separately
        UserProfileItemDto userProfileItem = new UserProfileItemDto(userName, nym, reputationScore);
        return new OfferListItemDto(bisqEasyOfferDto,
                isMyOffer,
                userProfileItem,
                formattedDate,
                formattedQuoteAmount,
                formattedBaseAmount,
                formattedPrice,
                formattedPriceSpec,
                quoteSidePaymentMethods,
                baseSidePaymentMethods);
    }
}