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

import bisq.account.AccountService;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.offer.Direction;
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
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MuSigOfferListItem {
    @EqualsAndHashCode.Include
    private final MuSigOffer offer;
    private final MarketPriceService marketPriceService;

    private final String quoteCurrencyCode, baseAmountAsString, quoteAmountAsString, paymentMethodsAsString,
            maker, takeOfferButtonText;
    private final boolean isMyOffer, noAccountForOfferPaymentMethods, canTakeOffer;
    private final Market market;
    private final Direction direction;
    private final List<FiatPaymentMethod> paymentMethods;
    private final UserProfile makerUserProfile;
    private final ReputationScore reputationScore;
    private final long totalScore;
    private final Map<FiatPaymentMethod, Boolean> accountAvailableByPaymentMethod;

    private Optional<String> cannotTakeOfferReason = Optional.empty();

    private double priceSpecAsPercent = 0;
    private String formattedPercentagePrice = Res.get("data.na"),
            price = Res.get("data.na"),
            priceTooltip = Res.get("data.na");
    private long priceAsLong = 0;


    private final Pin marketPriceByCurrencyMapPin;

    MuSigOfferListItem(MuSigOffer offer,
                       MarketPriceService marketPriceService,
                       UserProfileService userProfileService,
                       IdentityService identityService,
                       ReputationService reputationService,
                       AccountService accountService) {
        this.offer = offer;
        this.marketPriceService = marketPriceService;

        isMyOffer = identityService.findActiveIdentity(offer.getMakerNetworkId()).isPresent();
        quoteCurrencyCode = offer.getMarket().getQuoteCurrencyCode();

        AmountSpec amountSpec = offer.getAmountSpec();
        PriceSpec priceSpec = offer.getPriceSpec();
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        market = offer.getMarket();
        baseAmountAsString = OfferAmountFormatter.formatBaseAmount(marketPriceService, offer, false, false);
        quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, hasAmountRange, false);
        takeOfferButtonText = offer.getDirection().isBuy()
                ? Res.get("muSig.offerbook.table.cell.offer.intent.sell")
                : Res.get("muSig.offerbook.table.cell.offer.intent.buy");
        direction = offer.getDirection();

        // ImageUtil.getImageViewById(fiatPaymentMethod.getName());
        paymentMethodsAsString = Joiner.on("\n")
                .join(PaymentMethodSpecUtil.getPaymentMethods(offer.getQuoteSidePaymentMethodSpecs()).stream()
                        .map(PaymentMethod::getDisplayString)
                        .collect(Collectors.toList()));
        paymentMethods = retrieveAndSortFiatPaymentMethods();

        accountAvailableByPaymentMethod = paymentMethods.stream().collect(Collectors.toMap(paymentMethod -> paymentMethod,
                paymentMethod -> !accountService.getAccounts(paymentMethod).isEmpty()));
        noAccountForOfferPaymentMethods = paymentMethods.stream()
                .allMatch(paymentMethod -> accountService.getAccounts(paymentMethod).isEmpty());
        if (noAccountForOfferPaymentMethods) {
            cannotTakeOfferReason = Optional.of(Res.get("muSig.offerbook..table.cell.takeOffer.cannotTakeOfferReason.noAccountForOfferPaymentMethods"));
        }
        canTakeOffer = !noAccountForOfferPaymentMethods;

        makerUserProfile = userProfileService.findUserProfile(offer.getMakersUserProfileId())
                .orElseThrow(() -> new RuntimeException("No maker user profile found for offer: " + offer.getId()));

        reputationScore = reputationService.getReputationScore(makerUserProfile);
        totalScore = reputationScore.getTotalScore();

        maker = userProfileService.findUserProfile(offer.getMakersUserProfileId())
                .map(UserProfile::getUserName)
                .orElse(Res.get("data.na"));

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
        PriceUtil.findPercentFromMarketPrice(marketPriceService, offer)
                .ifPresent(priceSpecAsPercent -> {
                    this.priceSpecAsPercent = priceSpecAsPercent;
                    formattedPercentagePrice = PercentageFormatter.formatToPercentWithSignAndSymbol(priceSpecAsPercent);
                    String offerPrice = OfferPriceFormatter.formatQuote(marketPriceService, offer);
                    PriceSpec priceSpec = offer.getPriceSpec();
                    priceTooltip = PriceSpecFormatter.getFormattedPriceSpecWithOfferPrice(priceSpec, offerPrice);
                    price = PriceSpecFormatter.getFormattedPrice(priceSpec, marketPriceService, offer.getMarket());

                    priceAsLong = PriceUtil.findQuote(marketPriceService, priceSpec, offer.getMarket()).map(PriceQuote::getValue).orElse(0L);
                });
    }

    private List<FiatPaymentMethod> retrieveAndSortFiatPaymentMethods() {
        List<FiatPaymentMethod> paymentMethods =
                PaymentMethodSpecUtil.getPaymentMethods(offer.getQuoteSidePaymentMethodSpecs());
        paymentMethods.sort(Comparator.comparing(FiatPaymentMethod::isCustomPaymentMethod)
                .thenComparing(FiatPaymentMethod::getDisplayString));
        return paymentMethods;
    }
}
