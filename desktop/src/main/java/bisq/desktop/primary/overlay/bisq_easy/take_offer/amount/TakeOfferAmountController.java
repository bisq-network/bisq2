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
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.overlay.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.amount.AmountSpec;
import bisq.offer.amount.AmountUtil;
import bisq.offer.amount.FixQuoteAmountSpec;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
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
        amountComponent.setDirection(bisqEasyOffer.getDirection());
        amountComponent.setMarket(bisqEasyOffer.getMarket());
        amountComponent.setMinMaxRange(AmountUtil.findMinQuoteAmount(marketPriceService, bisqEasyOffer).orElseThrow(),
                AmountUtil.findMaxQuoteAmount(marketPriceService, bisqEasyOffer).orElseThrow());

        String direction = bisqEasyOffer.getTakersDirection().isBuy() ?
                Res.get("buy").toUpperCase() :
                Res.get("sell").toUpperCase();

        amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description",
                bisqEasyOffer.getMarket().getQuoteCurrencyCode(),
                direction,
                OfferAmountFormatter.getMinQuoteAmount(marketPriceService, bisqEasyOffer),
                OfferAmountFormatter.getMaxQuoteAmount(marketPriceService, bisqEasyOffer)));

        PriceUtil.findQuote(marketPriceService, bisqEasyOffer).ifPresent(amountComponent::setQuote);

        takersAmountSpec.ifPresent(amountSpec -> {
            AmountUtil.findAmountOrMaxQuoteAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket())
                    .ifPresent(amountComponent::setQuoteSideAmount);
            AmountUtil.findAmountOrMaxBaseAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket())
                    .ifPresent(amountComponent::setBaseSideAmount);
        });
    }

    public ReadOnlyObjectProperty<AmountSpec> getTakersAmountSpec() {
        return model.getTakersAmountSpec();
    }

    @Override
    public void onActivate() {
        quoteSideAmountPin = EasyBind.subscribe(amountComponent.getQuoteSideAmount(), quoteSideAmount -> {
            model.getTakersAmountSpec().set(new FixQuoteAmountSpec(quoteSideAmount.getValue()));
        });
    }

    @Override
    public void onDeactivate() {
        quoteSideAmountPin.unsubscribe();
    }
}
