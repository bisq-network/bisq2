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

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
        model.setBisqEasyOffer(bisqEasyOffer);

        model.getOfferType().set(bisqEasyOffer.getDirection().isBuy() ?
                Res.get("bisqEasy.offerDetails.buy") :
                Res.get("bisqEasy.offerDetails.sell"));
        Market market = bisqEasyOffer.getMarket();
        model.getQuoteSideAmountDescription().set(Res.get("bisqEasy.offerDetails.quoteSideAmount",
                market.getQuoteCurrencyCode()));


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
            model.getBaseSideAmount().set(formattedMinBaseAmount + " – " + formattedMaxBaseAmount);
            model.getQuoteSideAmount().set(formattedMinQuoteAmount + " – " + formattedMaxQuoteAmount);
        } else {
            Monetary fixBaseSideAmount = OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);
            Monetary fixQuoteSideAmount = OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);
            model.getBaseSideAmount().set(formattedBaseAmount);
            model.getQuoteSideAmount().set(formattedQuoteAmount);
        }

        model.getPrice().set(PriceUtil.findQuote(marketPriceService, bisqEasyOffer)
                .map(quote -> {
                    String price = PriceFormatter.format(quote, true);
                    String percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, bisqEasyOffer)
                            .map(PercentageFormatter::formatToPercentWithSymbol)
                            .orElse("");
                    return Res.get("bisqEasy.offerDetails.priceValue", price, percentFromMarketPrice);
                })
                .orElse(Res.get("data.na")));
        model.getPriceDescription().set(Res.get("bisqEasy.offerDetails.price", bisqEasyOffer.getMarket().getMarketCodes()));
        model.getPaymentMethods().set(PaymentMethodSpecFormatter.fromPaymentMethodSpecs(bisqEasyOffer.getQuoteSidePaymentMethodSpecs()));

        model.getId().set(bisqEasyOffer.getId());
        model.getDate().set(DateFormatter.formatDateTime(bisqEasyOffer.getDate()));

        Optional<String> tradeTerms = OfferOptionUtil.findMakersTradeTerms(bisqEasyOffer);
        model.getMakersTradeTermsVisible().set(tradeTerms.isPresent());
        tradeTerms.ifPresent(makersTradeTerms ->
                model.getMakersTradeTerms().set(makersTradeTerms));
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
