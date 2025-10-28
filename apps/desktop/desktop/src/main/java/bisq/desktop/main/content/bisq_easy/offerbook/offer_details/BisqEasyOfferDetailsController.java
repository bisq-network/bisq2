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

package bisq.desktop.main.content.bisq_easy.offerbook.offer_details;

import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecFormatter;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

// TODO (low prio) not used yet, but keep it until we are sure that it will not be used
@Slf4j
public class BisqEasyOfferDetailsController implements InitWithDataController<BisqEasyOfferDetailsController.InitData> {

    @Getter
    @EqualsAndHashCode
    public static final class InitData {
        private final BisqEasyOffer bisqEasyOffer;

        public InitData(BisqEasyOffer bisqEasyOffer) {
            this.bisqEasyOffer = bisqEasyOffer;
        }
    }

    @Getter
    private final BisqEasyOfferDetailsView view;
    private final BisqEasyOfferDetailsModel model;

    private final MarketPriceService marketPriceService;

    public BisqEasyOfferDetailsController(ServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        model = new BisqEasyOfferDetailsModel();
        view = new BisqEasyOfferDetailsView(model, this);
    }

    @Override
    public void initWithData(InitData data) {
        BisqEasyOffer bisqEasyOffer = data.getBisqEasyOffer();
        model.setDirection(bisqEasyOffer.getDirection().isBuy() ?
                Res.get("bisqEasy.offerDetails.direction.buy") :
                Res.get("bisqEasy.offerDetails.direction.sell"));

        Market market = bisqEasyOffer.getMarket();
        String quoteCurrencyCode = market.getQuoteCurrencyCode();
        String baseCurrencyCode = market.getBaseCurrencyCode();

        model.setQuoteSideCurrencyCode(quoteCurrencyCode);
        model.setBaseSideCurrencyCode(baseCurrencyCode);

        boolean isRangeAmount = bisqEasyOffer.hasAmountRange();
        AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
        PriceSpec priceSpec = bisqEasyOffer.getPriceSpec();
        if (isRangeAmount) {
            Monetary minBaseSideAmount = OfferAmountUtil.findBaseSideMinAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            Monetary maxBaseSideAmount = OfferAmountUtil.findBaseSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            Monetary minQuoteSideAmount = OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            Monetary maxQuoteSideAmount = OfferAmountUtil.findQuoteSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            String formattedMinQuoteAmount = AmountFormatter.formatQuoteAmount(minQuoteSideAmount);
            String formattedMinBaseAmount = AmountFormatter.formatBaseAmount(minBaseSideAmount);
            String formattedMaxQuoteAmount = AmountFormatter.formatQuoteAmount(maxQuoteSideAmount);
            String formattedMaxBaseAmount = AmountFormatter.formatBaseAmount(maxBaseSideAmount);

            model.setBaseSideAmount(Res.get("bisqEasy.offerDetails.baseSideRangeAmount", formattedMinBaseAmount, formattedMaxBaseAmount, baseCurrencyCode));
            model.setQuoteSideAmount(Res.get("bisqEasy.offerDetails.quoteSideRangeAmount", formattedMinQuoteAmount, formattedMaxQuoteAmount, quoteCurrencyCode));
        } else {
            Monetary fixBaseSideAmount = OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);
            Monetary fixQuoteSideAmount = OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);

            model.setBaseSideAmount(Res.get("bisqEasy.offerDetails.baseSideAmount", formattedBaseAmount, baseCurrencyCode));
            model.setQuoteSideAmount(Res.get("bisqEasy.offerDetails.quoteSideAmount", formattedQuoteAmount, quoteCurrencyCode));
        }

        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, priceSpec, market);
        String formattedPrice = priceQuote
                .map(PriceFormatter::format)
                .orElse("");
        model.setPrice(Res.get("bisqEasy.offerDetails.price", formattedPrice, market.getMarketCodes()));

        applyPriceDetails(priceSpec, market);
        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = bisqEasyOffer.getQuoteSidePaymentMethodSpecs();
        model.setQuoteSidePaymentMethods(PaymentMethodSpecFormatter.fromPaymentMethodSpecs(quoteSidePaymentMethodSpecs));
        List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs = bisqEasyOffer.getBaseSidePaymentMethodSpecs();
        model.setBaseSidePaymentMethods(PaymentMethodSpecFormatter.fromPaymentMethodSpecs(baseSidePaymentMethodSpecs));
        model.setBaseSidePaymentMethodDescription(
                baseSidePaymentMethodSpecs.size() == 1
                        ? Res.get("bisqEasy.offerDetails.baseSidePaymentMethodDescription")
                        : Res.get("bisqEasy.offerDetails.baseSidePaymentMethodDescriptions")
        );
        model.setQuoteSidePaymentMethodDescription(
                quoteSidePaymentMethodSpecs.size() == 1
                        ? Res.get("bisqEasy.offerDetails.quoteSidePaymentMethodDescription")
                        : Res.get("bisqEasy.offerDetails.quoteSidePaymentMethodDescriptions")
        );

        model.setId(bisqEasyOffer.getId());
        model.setDate(DateFormatter.formatDateTime(bisqEasyOffer.getDate()));

        Optional<String> tradeTerms = OfferOptionUtil.findMakersTradeTerms(bisqEasyOffer.getOfferOptions());
        model.setMakersTradeTermsVisible(tradeTerms.isPresent());
        tradeTerms.ifPresent(model::setMakersTradeTerms);
    }

    private void applyPriceDetails(PriceSpec priceSpec, Market market) {
        String codes = market.getMarketCodes();
        Optional<PriceQuote> marketPriceQuote = marketPriceService.findMarketPrice(market).map(MarketPrice::getPriceQuote);
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::format).orElseGet(() -> Res.get("data.na"));
        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);
        double percent = percentFromMarketPrice.orElse(0d);
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            if (percentFromMarketPrice.isEmpty()) {
                log.warn("No market price available");
                model.setPriceDetails("");
            } else {
                model.setPriceDetails(Res.get("bisqEasy.offerDetails.priceDetails.marketPrice"));
            }
        } else {
            String aboveOrBelow = percent > 0 ? Res.get("offer.price.above") : Res.get("offer.price.below");
            String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElseGet(() -> Res.get("data.na"));
            if (priceSpec instanceof FloatPriceSpec) {
                model.setPriceDetails(Res.get("bisqEasy.offerDetails.priceDetails.float",
                        percentAsString, aboveOrBelow, marketPriceAsString, codes));
            } else {
                if (percent == 0) {
                    if (percentFromMarketPrice.isEmpty()) {
                        log.warn("No market price available");
                        model.setPriceDetails("");
                    } else {
                        model.setPriceDetails(Res.get("bisqEasy.offerDetails.priceDetails.fix.atMarket",
                                marketPriceAsString, codes));
                    }
                } else {
                    model.setPriceDetails(Res.get("bisqEasy.offerDetails.priceDetails.fix",
                            percentAsString, aboveOrBelow, marketPriceAsString, codes));
                }
            }
        }
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onClose() {
        OverlayController.hide();
    }
}
