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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.price;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.monetary.Quote;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.overlay.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import bisq.offer.price_spec.FixPriceSpec;
import bisq.offer.price_spec.FloatPriceSpec;
import bisq.offer.price_spec.PriceSpec;
import bisq.offer.utils.OfferUtil;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static bisq.presentation.formatters.PercentageFormatter.formatToPercentWithSymbol;
import static bisq.presentation.parser.PercentageParser.parse;

@Slf4j
public class PriceController implements Controller {
    private final PriceModel model;
    @Getter
    private final PriceView view;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private Subscription priceInputPin;

    public PriceController(DefaultApplicationService applicationService) {
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        priceInput = new PriceInput(applicationService.getOracleService().getMarketPriceService());
        model = new PriceModel();
        view = new PriceView(model, this, priceInput.getRoot());
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        priceInput.setMarket(market);
        model.setMarket(market);
    }

  /*  public ReadOnlyObjectProperty<Quote> getQuote() {
        return priceInput.getQuote();
    }

    public ReadOnlyDoubleProperty getPercentage() {
        return model.getPercentage();
    }*/

    public ReadOnlyObjectProperty<PriceSpec> getPriceSpec() {
        return model.getPriceSpec();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        priceInputPin = EasyBind.subscribe(priceInput.getQuote(), this::onQuoteInput);

        String marketCodes = model.getMarket().getMarketCodes();
        priceInput.setDescription(Res.get("onboarding.price.sellersPrice", marketCodes));

        model.getMarketPriceDescription().set(Res.get("onboarding.price.marketPrice", marketCodes));
        model.getMarketPriceAsString().set(marketPriceService.findMarketPrice(model.getMarket())
                .map(marketPrice -> QuoteFormatter.format(marketPrice.getQuote()))
                .orElse(Res.get("na")));
    }


    @Override
    public void onDeactivate() {
        priceInputPin.unsubscribe();
    }

    void onPercentageFocussed(boolean focussed) {
        if (!focussed) {
            try {
                double percentage = parse(model.getPercentageAsString().get());
                // Need to change the value first otherwise it does not trigger an update
                model.getPercentageAsString().set("");
                model.getPercentageAsString().set(formatToPercentWithSymbol(percentage));

                Quote quote = Quote.fromMarketPriceMarkup(findMarketPriceQuote(), percentage);
                priceInput.setQuote(quote);
                model.getPriceSpec().set(new FloatPriceSpec(percentage));
            } catch (NumberFormatException t) {
                new Popup().warning(Res.get("onboarding.price.warn.invalidPrice")).show();
                onQuoteInput(priceInput.getQuote().get());
            }
        }
    }

    private void onQuoteInput(Quote quote) {
        if (quote == null) {
            model.getPercentage().set(0);
            model.getPercentageAsString().set("");
            return;
        }
        if (isQuoteValid(quote)) {
            model.getPriceAsString().set(QuoteFormatter.format(quote, true));
            applyPercentageFromQuote(quote);
            model.getPriceSpec().set(new FixPriceSpec(quote));
        } else {
            new Popup().warning(Res.get("onboarding.price.warn.invalidPrice")).show();
            Quote marketPrice = findMarketPriceQuote();
            priceInput.setQuote(marketPrice);
            applyPercentageFromQuote(marketPrice);
        }
    }

    private void applyPercentageFromQuote(Quote quote) {
        double percentage = getPercentage(quote);
        model.getPercentage().set(percentage);
        model.getPercentageAsString().set(formatToPercentWithSymbol(percentage));
    }


    //todo add validator and give feedback
    private boolean isQuoteValid(Quote quote) {
        double percentage = getPercentage(quote);
        if (percentage >= -0.1 && percentage <= 0.5) {
            return true;
        }
        return false;
    }

    private double getPercentage(Quote quote) {
        return OfferUtil.findFloatPriceSpec(marketPriceService, quote).orElseThrow().getPercentage();
    }

    private Quote findMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(model.getMarket()).orElseThrow();
    }
}
