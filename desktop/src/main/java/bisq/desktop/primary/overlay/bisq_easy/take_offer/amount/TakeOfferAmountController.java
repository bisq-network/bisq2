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
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.overlay.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.oracle.service.market_price.MarketPriceService;
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
    private Subscription baseSideAmountPin, quoteSideAmountPin;

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
        amountComponent.setMinMaxRange(OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow(),
                OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow());

        String direction = bisqEasyOffer.getTakersDirection().isBuy() ?
                Res.get("offer.buy").toUpperCase() :
                Res.get("offer.sell").toUpperCase();

        amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description",
                market.getQuoteCurrencyCode(),
                direction,
                OfferAmountFormatter.formatQuoteSideMinAmount(marketPriceService, bisqEasyOffer, false),
                OfferAmountFormatter.formatQuoteSideMaxAmount(marketPriceService, bisqEasyOffer)));

        if (bisqEasyOffer.getTakersDirection().isBuy()) {
            // If taker is buyer we set the sellers price from the offer
            PriceUtil.findQuote(marketPriceService, bisqEasyOffer).ifPresent(amountComponent::setQuote);
        }

        takersAmountSpec.ifPresent(amountSpec -> {
            OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), market)
                    .ifPresent(amountComponent::setQuoteSideAmount);
            OfferAmountUtil.findBaseSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), market)
                    .ifPresent(amountComponent::setBaseSideAmount);
        });
    }

    public void setTradePriceSpec(PriceSpec priceSpec) {
        // Only handle if taker is seller
        if (priceSpec != null && model.getBisqEasyOffer() != null && model.getBisqEasyOffer().getTakersDirection().isSell()) {
            PriceUtil.findQuote(marketPriceService, priceSpec, model.getBisqEasyOffer().getMarket()).ifPresent(amountComponent::setQuote);
        }
    }

    public ReadOnlyObjectProperty<Monetary> getTakersQuoteSideAmount() {
        return model.getTakersQuoteSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getTakersBaseSideAmount() {
        return model.getTakersBaseSideAmount();
    }

    @Override
    public void onActivate() {
        baseSideAmountPin = EasyBind.subscribe(amountComponent.getBaseSideAmount(),
                amount -> model.getTakersBaseSideAmount().set(amount));
        quoteSideAmountPin = EasyBind.subscribe(amountComponent.getQuoteSideAmount(),
                amount -> model.getTakersQuoteSideAmount().set(amount));
    }

    @Override
    public void onDeactivate() {
        baseSideAmountPin.unsubscribe();
        quoteSideAmountPin.unsubscribe();
    }
}
