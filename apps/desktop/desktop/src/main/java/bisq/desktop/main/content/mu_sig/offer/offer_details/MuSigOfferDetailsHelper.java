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

package bisq.desktop.main.content.mu_sig.offer.offer_details;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.asset.Asset;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.Amount;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FixAtMarketPriceDetails;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FixPriceDetails;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FixedAmount;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FixedOfferAmounts;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FloatPriceDetails;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FormattedFixedOfferAmounts;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FormattedRangeOfferAmounts;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.MarketPriceDetails;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.OfferAmounts;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.PriceDetails;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.RangeAmount;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.RangeOfferAmounts;
import bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.SecurityDepositInfo;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.presentation.formatters.AmountFormatter.formatAmount;
import static bisq.presentation.formatters.AmountFormatter.formatAmountWithCode;
import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
class MuSigOfferDetailsHelper {

    //
    // SecurityDeposit
    //

    static Optional<SecurityDepositInfo> createSecurityDepositInfo(MuSigOffer offer, Optional<OfferAmounts> offerAmounts) {
        double securityDepositAsPercent = getSecurityDepositPercent(offer);
        String formattedPercent = PercentageFormatter.formatToPercentWithSymbol(securityDepositAsPercent, 0);
        Optional<Amount> securityDepositAsBTC = offerAmounts.map(amounts ->
                calculateSecurityDepositAsBTC(offer.getMarket(), amounts, securityDepositAsPercent));
        return Optional.of(new SecurityDepositInfo(securityDepositAsPercent, securityDepositAsBTC, formattedPercent));
    }

    static Amount calculateSecurityDepositAsBTC(Market market,
                                                OfferAmounts offerAmounts,
                                                double securityDepositAsPercent) {
        return
                switch (offerAmounts) {
                    case RangeOfferAmounts rangeAmounts -> {
                        Monetary minSecurityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market,
                                rangeAmounts.baseSide().minAmount(),
                                rangeAmounts.quoteSide().minAmount(),
                                securityDepositAsPercent);
                        Monetary maxSecurityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market,
                                rangeAmounts.baseSide().maxAmount(),
                                rangeAmounts.quoteSide().maxAmount(),
                                securityDepositAsPercent);
                        yield new RangeAmount(minSecurityDeposit, maxSecurityDeposit);
                    }
                    case FixedOfferAmounts fixedAmounts -> {
                        Monetary securityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market,
                                fixedAmounts.baseSide().amount(),
                                fixedAmounts.quoteSide().amount(),
                                securityDepositAsPercent);
                        yield new FixedAmount(securityDeposit);
                    }
                    default -> throw new IllegalStateException("Unexpected amounts type: " + offerAmounts);
                };
    }

    // From bisq.desktop.main.content.mu_sig.offer.take_offer.review.MuSigTakeOfferReviewController.init
    static double getSecurityDepositPercent(MuSigOffer muSigOffer) {
        Optional<CollateralOption> optionalCollateralOption = OfferOptionUtil.findCollateralOption(muSigOffer.getOfferOptions());
        checkArgument(optionalCollateralOption.isPresent(), "CollateralOption must be present");
        CollateralOption collateralOption = optionalCollateralOption.get();
        checkArgument(Math.abs(collateralOption.getSellerSecurityDeposit() - collateralOption.getBuyerSecurityDeposit()) < 1e-9,
                "SellerSecurityDeposit and BuyerSecurityDeposit are expected to be equal");
        return collateralOption.getBuyerSecurityDeposit();
    }

    // From bisq.offer.amount.OfferAmountFormatter#formatDepositAmountAsBTC(Monetary)
    static String formatDepositAmountAsBTC(Monetary monetary, boolean withCode) {
        checkArgument(Asset.isBtc(monetary.getCode()));
        return withCode ?
                formatAmountWithCode(Coin.asBtcFromValue(monetary.getValue()), false) :
                formatAmount(Coin.asBtcFromValue(monetary.getValue()), false);
    }

    //
    // Price details
    //

    static Optional<PriceDetails> getPriceDetails(MarketPriceService marketPriceService,
                                                  PriceSpec priceSpec,
                                                  Market market) {

        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);

        // Check if market price is available
        double percent;
        if (percentFromMarketPrice.isEmpty()) {
            log.warn("No market price available");
            return Optional.empty();
        } else {
            percent = percentFromMarketPrice.get();
        }

        // Prepare percent information for display
        String aboveOrBelow = percent > 0 ? Res.get("offer.price.above") : Res.get("offer.price.below");
        String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                .orElseGet(() -> Res.get("data.na"));

        // Get market price information
        String codes = market.getMarketCodes();
        Optional<PriceQuote> marketPriceQuote = marketPriceService.findMarketPrice(market).map(MarketPrice::getPriceQuote);
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::format).orElseGet(() -> Res.get("data.na"));

        // Handle market price or float price at market rate
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            return Optional.of(new MarketPriceDetails());
        }

        // Handle float price (not at market rate)
        if (priceSpec instanceof FloatPriceSpec) {
            return Optional.of(new FloatPriceDetails(marketPriceAsString, codes, percentAsString, aboveOrBelow));
        }

        // Handle fixed price spec
        if (priceSpec instanceof FixPriceSpec) {
            return percent == 0 ?
                    Optional.of(new FixAtMarketPriceDetails(marketPriceAsString, codes)) :
                    Optional.of(new FixPriceDetails(marketPriceAsString, codes, percentAsString, aboveOrBelow));
        }

        throw new IllegalStateException("Unexpected priceSpec type: " + priceSpec);
    }

    //
    // Offer Amounts
    //

    static Optional<OfferAmounts> getOfferAmounts(MarketPriceService marketPriceService, MuSigOffer offer) {
        return getOfferAmounts(marketPriceService, offer.getMarket(), offer.getAmountSpec(), offer.getPriceSpec());
    }

    static Optional<OfferAmounts> getOfferAmounts(MarketPriceService marketPriceService,
                                                  Market market,
                                                  AmountSpec amountSpec,
                                                  PriceSpec priceSpec) {
        return
                switch (amountSpec) {
                    case RangeAmountSpec a ->
                            getRangeOfferAmounts(marketPriceService, a, priceSpec, market).map(OfferAmounts.class::cast);
                    case FixedAmountSpec a ->
                            getFixedOfferAmounts(marketPriceService, a, priceSpec, market).map(OfferAmounts.class::cast);
                    default -> {
                        log.warn("Unexpected amountSpec type: {}", amountSpec);
                        yield Optional.empty();
                    }
                };
    }

    static Optional<RangeOfferAmounts> getRangeOfferAmounts(MarketPriceService marketPriceService,
                                                            RangeAmountSpec amountSpec,
                                                            PriceSpec priceSpec,
                                                            Market market) {
        Optional<Monetary> minBaseSideAmount = OfferAmountUtil.findBaseSideMinAmount(marketPriceService, amountSpec, priceSpec, market);
        Optional<Monetary> maxBaseSideAmount = OfferAmountUtil.findBaseSideMaxAmount(marketPriceService, amountSpec, priceSpec, market);
        Optional<Monetary> minQuoteSideAmount = OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, amountSpec, priceSpec, market);
        Optional<Monetary> maxQuoteSideAmount = OfferAmountUtil.findQuoteSideMaxAmount(marketPriceService, amountSpec, priceSpec, market);

        // Return empty if any required amount is missing
        if (minBaseSideAmount.isEmpty() || maxBaseSideAmount.isEmpty() ||
                minQuoteSideAmount.isEmpty() || maxQuoteSideAmount.isEmpty()) {
            log.warn("Unable to calculate one or more range amounts for market: {}", market);
            return Optional.empty();
        }

        return Optional.of(new RangeOfferAmounts(
                new RangeAmount(minBaseSideAmount.get(), maxBaseSideAmount.get()),
                new RangeAmount(minQuoteSideAmount.get(), maxQuoteSideAmount.get())));
    }

    static Optional<FixedOfferAmounts> getFixedOfferAmounts(MarketPriceService marketPriceService,
                                                            FixedAmountSpec amountSpec,
                                                            PriceSpec priceSpec,
                                                            Market market) {
        Optional<Monetary> fixBaseSideAmount = OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, amountSpec, priceSpec, market);
        Optional<Monetary> fixQuoteSideAmount = OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, amountSpec, priceSpec, market);

        // Return empty if any required amount is missing
        if (fixBaseSideAmount.isEmpty() || fixQuoteSideAmount.isEmpty()) {
            log.warn("Unable to calculate one or more fixed amounts for market: {}", market);
            return Optional.empty();
        }

        return Optional.of(new FixedOfferAmounts(
                new FixedAmount(fixBaseSideAmount.get()),
                new FixedAmount(fixQuoteSideAmount.get())));
    }

    static FormattedRangeOfferAmounts formatRangeOfferAmounts(RangeOfferAmounts rangeOfferAmounts) {
        return new FormattedRangeOfferAmounts(
                AmountFormatter.formatBaseAmount(rangeOfferAmounts.baseSide().minAmount()),
                AmountFormatter.formatBaseAmount(rangeOfferAmounts.baseSide().maxAmount()),
                AmountFormatter.formatQuoteAmount(rangeOfferAmounts.quoteSide().minAmount()),
                AmountFormatter.formatQuoteAmount(rangeOfferAmounts.quoteSide().maxAmount())
        );
    }

    static FormattedFixedOfferAmounts formatFixedOfferAmounts(FixedOfferAmounts fixedOfferAmounts) {
        return new FormattedFixedOfferAmounts(
                AmountFormatter.formatBaseAmount(fixedOfferAmounts.baseSide().amount()),
                AmountFormatter.formatQuoteAmount(fixedOfferAmounts.quoteSide().amount())
        );
    }
}
