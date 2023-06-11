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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.amount;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.overlay.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.amount.AmountUtil;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.FixQuoteAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class TakeOfferAmountController implements Controller {
    private final TakeOfferAmountModel model;
    @Getter
    private final TakeOfferAmountView view;
    private final AmountComponent amountComponent;
    private final MarketPriceService marketPriceService;
    private Subscription quoteSideAmountPin;

    public TakeOfferAmountController(DefaultApplicationService applicationService) {
        model = new TakeOfferAmountModel();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        amountComponent = new AmountComponent(applicationService, true);
        view = new TakeOfferAmountView(model, this, amountComponent.getView().getRoot());
    }

    public void init(BisqEasyOffer bisqEasyOffer, Optional<AmountSpec> takersAmountSpec) {
        model.setBisqEasyOffer(bisqEasyOffer);

        amountComponent.setDirection(bisqEasyOffer.getDirection());
        Market market = bisqEasyOffer.getMarket();
        amountComponent.setMarket(market);
        amountComponent.setMinMaxRange(AmountUtil.findMinOrFixQuoteAmount(marketPriceService, bisqEasyOffer).orElseThrow(),
                AmountUtil.findMaxOrFixQuoteAmount(marketPriceService, bisqEasyOffer).orElseThrow());

        String direction = bisqEasyOffer.getTakersDirection().isBuy() ?
                Res.get("buy").toUpperCase() :
                Res.get("sell").toUpperCase();

        amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description",
                market.getQuoteCurrencyCode(),
                direction,
                OfferAmountFormatter.formatMinQuoteAmount(marketPriceService, bisqEasyOffer, false),
                OfferAmountFormatter.formatMaxQuoteAmount(marketPriceService, bisqEasyOffer)));

        if (bisqEasyOffer.getTakersDirection().isBuy()) {
            // If taker is buyer we set the sellers price from the offer
            PriceUtil.findQuote(marketPriceService, bisqEasyOffer).ifPresent(amountComponent::setQuote);
        }

        takersAmountSpec.ifPresent(amountSpec -> {
            AmountUtil.findFixOrMaxQuoteAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), market)
                    .ifPresent(amountComponent::setQuoteSideAmount);
            AmountUtil.findFixOrMaxBaseAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), market)
                    .ifPresent(amountComponent::setBaseSideAmount);
        });
    }

    public void setTradePriceSpec(PriceSpec priceSpec) {
        // Only handle if taker is seller
        if (priceSpec != null && model.getBisqEasyOffer() != null && model.getBisqEasyOffer().getTakersDirection().isSell()) {
            PriceUtil.findQuote(marketPriceService, priceSpec, model.getBisqEasyOffer().getMarket()).ifPresent(amountComponent::setQuote);
        }
    }

    public ReadOnlyObjectProperty<AmountSpec> getTakersAmountSpec() {
        return model.getTakersAmountSpec();
    }

    @Override
    public void onActivate() {
        quoteSideAmountPin = EasyBind.subscribe(amountComponent.getQuoteSideAmount(),
                quoteSideAmount -> model.getTakersAmountSpec().set(new FixQuoteAmountSpec(quoteSideAmount.getValue())));
    }

    @Override
    public void onDeactivate() {
        quoteSideAmountPin.unsubscribe();
    }
}
