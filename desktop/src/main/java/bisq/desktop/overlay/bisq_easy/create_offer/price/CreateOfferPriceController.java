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

package bisq.desktop.overlay.bisq_easy.create_offer.price;

import bisq.common.currency.Market;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.*;
import bisq.oracle.service.market_price.MarketPriceService;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static bisq.presentation.formatters.PercentageFormatter.formatToPercentWithSymbol;
import static bisq.presentation.parser.PercentageParser.parse;

@Slf4j
public class CreateOfferPriceController implements Controller {
    private final CreateOfferPriceModel model;
    @Getter
    private final CreateOfferPriceView view;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private final SettingsService settingsService;
    private Subscription priceInputPin;

    public CreateOfferPriceController(ServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getOracleService().getMarketPriceService();
        settingsService = serviceProvider.getSettingsService();
        priceInput = new PriceInput(serviceProvider.getOracleService().getMarketPriceService());
        model = new CreateOfferPriceModel();
        view = new CreateOfferPriceView(model, this, priceInput);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        priceInput.setMarket(market);
        model.setMarket(market);
    }

    public ReadOnlyObjectProperty<PriceSpec> getPriceSpec() {
        return model.getPriceSpec();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        settingsService.getCookie().asBoolean(CookieKey.CREATE_OFFER_USE_FIX_PRICE, getCookieSubKey())
                .ifPresent(useFixPrice -> model.getUseFixPrice().set(useFixPrice));

        priceInputPin = EasyBind.subscribe(priceInput.getQuote(), this::onQuoteInput);

        String marketCodes = model.getMarket().getMarketCodes();
        priceInput.setDescription(Res.get("bisqEasy.price.sellersPrice", marketCodes));

        applyPriceSpec();
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
                PriceQuote priceQuote = PriceUtil.fromMarketPriceMarkup(findMarketPriceQuote(), percentage);
                priceInput.setQuote(priceQuote);
                applyPriceSpec();
            } catch (NumberFormatException t) {
                new Popup().warning(Res.get("bisqEasy.price.warn.invalidPrice")).show();
                onQuoteInput(priceInput.getQuote().get());
            }
        }
    }

    void onToggleUseFixPrice() {
        boolean useFixPrice = !model.getUseFixPrice().get();
        model.getUseFixPrice().set(useFixPrice);
        settingsService.setCookie(CookieKey.CREATE_OFFER_USE_FIX_PRICE, getCookieSubKey(), useFixPrice);
        applyPriceSpec();
    }

    private void applyPriceSpec() {
        if (model.getUseFixPrice().get()) {
            model.getPriceSpec().set(new FixPriceSpec(priceInput.getQuote().get()));
        } else {
            double percentage = model.getPercentage().get();
            if (percentage == 0) {
                model.getPriceSpec().set(new MarketPriceSpec());
            } else {
                model.getPriceSpec().set(new FloatPriceSpec(percentage));
            }
        }
    }

    private void onQuoteInput(PriceQuote priceQuote) {
        if (priceQuote == null) {
            model.getPercentage().set(0);
            model.getPercentageAsString().set("");
            return;
        }
        if (isQuoteValid(priceQuote)) {
            model.getPriceAsString().set(PriceFormatter.format(priceQuote, true));
            applyPercentageFromQuote(priceQuote);
            applyPriceSpec();
        } else {
            new Popup().warning(Res.get("bisqEasy.price.warn.invalidPrice")).show();
            PriceQuote marketPrice = findMarketPriceQuote();
            priceInput.setQuote(marketPrice);
            applyPercentageFromQuote(marketPrice);
            applyPriceSpec();
        }
    }

    private void applyPercentageFromQuote(PriceQuote priceQuote) {
        double percentage = getPercentage(priceQuote);
        model.getPercentage().set(percentage);
        model.getPercentageAsString().set(formatToPercentWithSymbol(percentage));
    }


    //todo add validator and give feedback
    private boolean isQuoteValid(PriceQuote priceQuote) {
        double percentage = getPercentage(priceQuote);
        if (percentage >= -0.1 && percentage <= 0.5) {
            return true;
        }
        return false;
    }

    private double getPercentage(PriceQuote priceQuote) {
        return PriceSpecUtil.createFloatPriceSpec(marketPriceService, priceQuote).orElseThrow().getPercentage();
    }

    private PriceQuote findMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(model.getMarket()).orElseThrow();
    }

    private String getCookieSubKey() {
        return model.getMarket().getMarketCodes();
    }
}
