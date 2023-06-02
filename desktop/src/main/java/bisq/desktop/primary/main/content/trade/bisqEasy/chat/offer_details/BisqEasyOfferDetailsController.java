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
import bisq.common.monetary.Fiat;
import bisq.common.util.MathUtils;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.QuoteFormatter;
import bisq.security.KeyPairService;
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

    private final KeyPairService keyPairService;
    private final MarketPriceService marketPriceService;

    public BisqEasyOfferDetailsController(DefaultApplicationService applicationService) {
        keyPairService = applicationService.getSecurityService().getKeyPairService();
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
        String baseSideAmountAsDisplayString = bisqEasyOffer.getBaseSideAmountAsDisplayString();
        String quoteSideAmountAsDisplayString = bisqEasyOffer.getQuoteSideAmountAsDisplayString();
        bisqEasyOffer.getMinAmountAsPercentage()
                .ifPresentOrElse(minAmountAsPercentage -> {
                            long minBaseSideAmount = MathUtils.roundDoubleToLong(bisqEasyOffer.getBaseSideAmount() * minAmountAsPercentage);
                            String minBaseSideAmountAsDisplayString = AmountFormatter.formatAmountWithCode(Fiat.of(minBaseSideAmount,
                                    market.getBaseCurrencyCode()), true);
                            model.getBaseSideAmount().set(minBaseSideAmountAsDisplayString + " - " + baseSideAmountAsDisplayString);

                            long minQuoteSideAmount = MathUtils.roundDoubleToLong(bisqEasyOffer.getQuoteSideAmount() * minAmountAsPercentage);
                            String minQuoteSideAmountAsDisplayString = AmountFormatter.formatAmountWithCode(Fiat.of(minQuoteSideAmount,
                                    market.getQuoteCurrencyCode()), true);
                            model.getQuoteSideAmount().set(minQuoteSideAmountAsDisplayString + " - " + quoteSideAmountAsDisplayString);
                        },
                        () -> {
                            model.getBaseSideAmount().set(baseSideAmountAsDisplayString);
                            model.getQuoteSideAmount().set(quoteSideAmountAsDisplayString);
                        });
        model.getPrice().set(bisqEasyOffer.getQuote(marketPriceService)
                .map(quote -> {
                    String price = QuoteFormatter.format(quote, true);
                    String percentFromMarketPrice = bisqEasyOffer.findPercentFromMarketPrice(marketPriceService).map(PercentageFormatter::formatToPercentWithSymbol)
                            .orElse("");
                    return Res.get("bisqEasy.offerDetails.priceValue", price, percentFromMarketPrice);
                })
                .orElse(Res.get("na")));
        model.getPriceDescription().set(Res.get("bisqEasy.offerDetails.price", bisqEasyOffer.getMarket().getMarketCodes()));
        model.getPaymentMethods().set(bisqEasyOffer.getSettlementMethodsAsDisplayString());

        model.getId().set(bisqEasyOffer.getId());
        model.getDate().set(DateFormatter.formatDateTime(bisqEasyOffer.getDate()));

        model.getMakersTradeTermsVisible().set(bisqEasyOffer.getMakersTradeTerms().isPresent());
        bisqEasyOffer.getMakersTradeTerms().ifPresent(makersTradeTerms ->
                model.getMakersTradeTerms().set(makersTradeTerms));

        model.getRequiredTotalReputationScoreVisible().set(bisqEasyOffer.getRequiredTotalReputationScore().isPresent());
        bisqEasyOffer.getRequiredTotalReputationScore().ifPresent(requiredTotalReputationScore ->
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

    private boolean isMyOffer(BisqEasyOffer bisqEasyOffer) {
        return keyPairService.findKeyPair(bisqEasyOffer.getMakerNetworkId().getPubKey().getKeyId()).isPresent();
    }
}
