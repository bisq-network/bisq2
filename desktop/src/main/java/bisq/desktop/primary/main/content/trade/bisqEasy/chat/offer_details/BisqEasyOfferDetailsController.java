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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.offer_details;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.QuoteFormatter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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

    public BisqEasyOfferDetailsController(DefaultApplicationService applicationService) {
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        model = new BisqEasyOfferDetailsModel();
        view = new BisqEasyOfferDetailsView(model, this);
    }

    @Override
    public void initWithData(InitData data) {
        BisqEasyOffer bisqEasyOffer = data.getBisqEasyOffer();
        model.setBisqEasyOffer(bisqEasyOffer);

        model.getOfferType().set(bisqEasyOffer.getDirection().isBuy() ?
                Res.get("bisqEasy.offerDetails.makerAsBuyer") :
                Res.get("bisqEasy.offerDetails.makerAsSeller"));
        Market market = bisqEasyOffer.getMarket();
        model.getQuoteSideAmountDescription().set(Res.get("bisqEasy.offerDetails.quoteSideAmount",
                market.getQuoteCurrencyCode()));

        if (bisqEasyOffer.getBaseSideMaxAmount().getValue() == bisqEasyOffer.getBaseSideMinAmount().getValue()) {
            model.getBaseSideAmount().set(bisqEasyOffer.getBaseSideAmountAsDisplayString());
        } else {
            model.getBaseSideAmount().set(bisqEasyOffer.getBaseSideMinAmountAsDisplayString() + " - " +
                    bisqEasyOffer.getBaseSideMaxAmountAsDisplayString());
        }

        if (bisqEasyOffer.getQuoteSideMaxAmount().getValue() == bisqEasyOffer.getQuoteSideMinAmount().getValue()) {
            model.getQuoteSideAmount().set(bisqEasyOffer.getQuoteSideMaxAmountAsDisplayString());
        } else {
            model.getQuoteSideAmount().set(bisqEasyOffer.getQuoteSideMinAmountAsDisplayString() + " - " +
                    bisqEasyOffer.getQuoteSideMaxAmountAsDisplayString());
        }


        model.getPrice().set(bisqEasyOffer.findQuote(marketPriceService)
                .map(quote -> {
                    String price = QuoteFormatter.format(quote, true);
                    String percentFromMarketPrice = bisqEasyOffer.findPercentFromMarketPrice(marketPriceService).map(PercentageFormatter::formatToPercentWithSymbol)
                            .orElse("");
                    return Res.get("bisqEasy.offerDetails.priceValue", price, percentFromMarketPrice);
                })
                .orElse(Res.get("na")));
        model.getPriceDescription().set(Res.get("bisqEasy.offerDetails.price", bisqEasyOffer.getMarket().getMarketCodes()));
        model.getPaymentMethods().set(bisqEasyOffer.getQuoteSideSettlementMethodsAsDisplayString());

        model.getId().set(bisqEasyOffer.getId());
        model.getDate().set(DateFormatter.formatDateTime(bisqEasyOffer.getDate()));

        model.getMakersTradeTermsVisible().set(bisqEasyOffer.findMakersTradeTerms().isPresent());
        bisqEasyOffer.findMakersTradeTerms().ifPresent(makersTradeTerms ->
                model.getMakersTradeTerms().set(makersTradeTerms));

        model.getRequiredTotalReputationScoreVisible().set(bisqEasyOffer.findRequiredTotalReputationScore().isPresent());
        bisqEasyOffer.findRequiredTotalReputationScore().ifPresent(requiredTotalReputationScore ->
                model.getRequiredTotalReputationScore().set("" + requiredTotalReputationScore)); //todo
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
