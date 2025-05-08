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

package bisq.desktop.main.content.mu_sig.offerbook;

import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.OfferPriceFormatter;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MuSigOfferListItem {
    @EqualsAndHashCode.Include
    private final MuSigOffer offer;
    private final MarketPriceService marketPriceService;

    private String price;
    private String priceTooltip;
    private final String baseAmountAsString;
    private final String quoteAmountAsString;
    private final String paymentMethod;
    private final String paymentMethodTooltip;
    private final String deposit;
    private final String maker;
    private final String quoteCurrencyCode;
    private final Pin marketPriceByCurrencyMapPin;
    private double priceSpecAsPercent;
    private String formattedPercentagePrice;

    MuSigOfferListItem(MuSigOffer offer, MarketPriceService marketPriceService, UserProfileService userProfileService) {
        this.offer = offer;
        quoteCurrencyCode = offer.getMarket().getQuoteCurrencyCode();

        this.marketPriceService = marketPriceService;
        // ImageUtil.getImageViewById(fiatPaymentMethod.getName());
        paymentMethodTooltip = Joiner.on(", ")
                .join(PaymentMethodSpecUtil.getPaymentMethods(offer.getQuoteSidePaymentMethodSpecs()).stream()
                        .map(PaymentMethod::getDisplayString)
                        .collect(Collectors.toList()));
        paymentMethod = StringUtils.truncate(paymentMethodTooltip, 30);
        deposit = "15%";
        maker = userProfileService.findUserProfile(offer.getMakersUserProfileId())
                .map(UserProfile::getUserName)
                .orElse(Res.get("data.na"));

        AmountSpec amountSpec = offer.getAmountSpec();
        PriceSpec priceSpec = offer.getPriceSpec();
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        Market market = offer.getMarket();
        quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, hasAmountRange, false);
        baseAmountAsString = OfferAmountFormatter.formatBaseAmount(marketPriceService, offer, false);

      //  Monetary quoteSideMinAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, offer).orElseThrow();
       // String formattedRangeQuoteAmount = OfferAmountFormatter.formatQuoteAmount(marketPriceService, offer, false);
       // boolean isFixPrice = offer.getPriceSpec() instanceof FixPriceSpec;

        // authorUserProfileId = offerbookMessage.getAuthorUserProfileId();

        String offerType = offer.getDirection().isBuy()
                ? Res.get("bisqEasy.offerbook.offerList.table.columns.offerType.buy")
                : Res.get("bisqEasy.offerbook.offerList.table.columns.offerType.sell");

        // reputationScore = reputationService.getReputationScore(senderUserProfile);
        // totalScore = reputationScore.getTotalScore();
        long offerAgeInDays = TimeFormatter.getAgeInDays(offer.getDate());
        String formattedOfferAge = TimeFormatter.formatAgeInDays(offer.getDate());
        String offerAgeTooltipText = Res.get("user.profileCard.offers.table.columns.offerAge.tooltip",
                DateFormatter.formatDateTime(offer.getDate()));

        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                UIThread.run(this::updatePriceSpecAsPercent));
        updatePriceSpecAsPercent();
    }

    void dispose() {
        marketPriceByCurrencyMapPin.unbind();
    }

    private void updatePriceSpecAsPercent() {
        priceSpecAsPercent = PriceUtil.findPercentFromMarketPrice(marketPriceService, offer).orElseThrow();
        formattedPercentagePrice = PercentageFormatter.formatToPercentWithSignAndSymbol(priceSpecAsPercent);
        String offerPrice = OfferPriceFormatter.formatQuote(marketPriceService, offer);
        priceTooltip = PriceSpecFormatter.getFormattedPriceSpecWithOfferPrice(offer.getPriceSpec(), offerPrice);

        PriceSpec priceSpec = offer.getPriceSpec();
        price = PriceSpecFormatter.getFormattedPrice(priceSpec, marketPriceService);
    }
}
