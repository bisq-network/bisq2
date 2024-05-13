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

package bisq.desktop.main.content.bisq_easy.trade_wizard.price;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.*;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

import static bisq.presentation.formatters.PercentageFormatter.formatToPercentWithSymbol;
import static bisq.presentation.parser.PercentageParser.parse;

@Slf4j
public class TradeWizardPriceController implements Controller {
    private final TradeWizardPriceModel model;
    @Getter
    private final TradeWizardPriceView view;
    private final PriceInput priceInput;
    private final Region owner;
    private final MarketPriceService marketPriceService;
    private final SettingsService settingsService;
    private Subscription priceInputPin, isPriceInvalidPin, priceSpecPin, percentageInputPin;

    public TradeWizardPriceController(ServiceProvider serviceProvider, Region owner) {
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        settingsService = serviceProvider.getSettingsService();
        priceInput = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());
        this.owner = owner;
        model = new TradeWizardPriceModel();
        view = new TradeWizardPriceView(model, this, priceInput);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        priceInput.setMarket(market);
        model.setMarket(market);
    }

    public void setDirection(Direction direction) {
        if (direction != null) {
            model.setDirection(direction);
        }
    }

    public ReadOnlyObjectProperty<PriceSpec> getPriceSpec() {
        return model.getPriceSpec();
    }

    public void reset() {
        priceInput.reset();
        model.reset();
    }

    public boolean validate() {
        if (model.getErrorMessage().get() == null) {
            return true;
        } else {
            new Popup().invalid(model.getErrorMessage().get())
                    .owner(owner)
                    .show();
            return false;
        }
    }

    @Override
    public void onActivate() {
        settingsService.getCookie().asBoolean(CookieKey.CREATE_OFFER_USE_FIX_PRICE, getCookieSubKey())
                .ifPresent(useFixPrice -> model.getUseFixPrice().set(useFixPrice));
        if (model.getPriceSpec().get() == null) {
            model.getPriceSpec().set(new MarketPriceSpec());
        }

        priceInputPin = EasyBind.subscribe(priceInput.getQuote(), this::onQuoteInput);
        isPriceInvalidPin = EasyBind.subscribe(priceInput.getValidationResult(), validationResult -> {
            if (validationResult != null && !validationResult.isValid) {
                model.getErrorMessage().set(validationResult.errorMessage);
                model.setLastValidPriceQuote(null);
            }
        });
        priceSpecPin = EasyBind.subscribe(model.getPriceSpec(), this::updateFeedback);

        percentageInputPin = EasyBind.subscribe(model.getPercentageInput(), percentageInput -> {
            if (percentageInput != null) {
                onPercentageInput(percentageInput);
            }
        });

        String marketCodes = model.getMarket().getMarketCodes();
        priceInput.setDescription(Res.get("bisqEasy.price.tradePrice.inputBoxText", marketCodes));

        model.getShouldShowFeedback().set(model.getDirection().isBuy());

        applyPriceSpec();
    }

    @Override
    public void onDeactivate() {
        model.getShouldShowLearnWhyOverlay().set(false);

        priceInputPin.unsubscribe();
        isPriceInvalidPin.unsubscribe();
        priceSpecPin.unsubscribe();
        percentageInputPin.unsubscribe();
    }

    void onPercentageFocussed(boolean focussed) {
        model.setFocused(focussed);
        if (!focussed) {
            try {
                double percentage = parse(model.getPercentageInput().get());
                String percentageAsString = formatToPercentWithSymbol(percentage);
                // Need to change the value first otherwise it does not trigger an update
                model.getPercentageInput().set("");
                model.getPercentageInput().set(percentageAsString);
                onPercentageInput(percentageAsString);
            } catch (Exception e) {
                model.getErrorMessage().set(Res.get("bisqEasy.price.warn.invalidPrice.numberFormatException"));
            }
        }
    }

    private void onPercentageInput(String percentageAsString) {
        if (model.isFocused() || model.getMarket() == null) {
            return;
        }
        if (percentageAsString == null || percentageAsString.trim().isEmpty()) {
            return;
        }

        model.getErrorMessage().set(null);
        try {
            double percentage = parse(percentageAsString);
            if (!validatePercentage(percentage)) {
                return;
            }
            Optional<PriceQuote> marketPriceQuote = findMarketPriceQuote();
            if (marketPriceQuote.isPresent()) {
                PriceQuote priceQuote = PriceUtil.fromMarketPriceMarkup(marketPriceQuote.get(), percentage);
                if (validateQuote(priceQuote)) {
                    priceInput.setQuote(priceQuote);
                } else {
                    return;
                }
            } else {
                log.error("marketPriceQuote is not present");
            }
            applyPriceSpec();
        } catch (NumberFormatException e) {
            model.getErrorMessage().set(Res.get("bisqEasy.price.warn.invalidPrice.numberFormatException"));
        } catch (Exception e) {
            model.getErrorMessage().set(Res.get("bisqEasy.price.warn.invalidPrice.exception", e.getMessage()));
        }
    }

    void onToggleUseFixPrice() {
        boolean useFixPrice = !model.getUseFixPrice().get();
        model.getUseFixPrice().set(useFixPrice);
        settingsService.setCookie(CookieKey.CREATE_OFFER_USE_FIX_PRICE, getCookieSubKey(), useFixPrice);
        applyPriceSpec();
    }

    void useFixedPrice() {
        if (!model.getUseFixPrice().get()) {
            onToggleUseFixPrice();
        }
    }

    void usePercentagePrice() {
        if (model.getUseFixPrice().get()) {
            onToggleUseFixPrice();
        }
    }

    void showLearnWhySection() {
        model.getShouldShowLearnWhyOverlay().set(true);
    }

    void closeLearnWhySection() {
        model.getShouldShowLearnWhyOverlay().set(false);
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
            model.getPercentageInput().set("");
            return;
        }
        if (validateQuote(priceQuote)) {
            model.getPriceAsString().set(PriceFormatter.format(priceQuote, true));
            applyPercentageFromQuote(priceQuote);
            applyPriceSpec();
            model.setLastValidPriceQuote(priceQuote);
        }
    }

    private void applyPercentageFromQuote(PriceQuote priceQuote) {
        double percentage = getPercentage(priceQuote);
        model.getPercentage().set(percentage);
        model.getPercentageInput().set(formatToPercentWithSymbol(percentage));
    }

    private boolean validateQuote(PriceQuote priceQuote) {
        return validatePercentage(getPercentage(priceQuote));
    }

    private boolean validatePercentage(double percentage) {
        if (percentage >= -0.1 && percentage <= 0.5) {
            model.getErrorMessage().set(null);
            return true;
        } else {
            model.getErrorMessage().set(Res.get("bisqEasy.price.warn.invalidPrice.outOfRange"));
            return false;
        }
    }

    private double getPercentage(PriceQuote priceQuote) {
        try {
            Optional<Double> optionalPercentage = PriceSpecUtil.createFloatPriceAsPercentage(marketPriceService, priceQuote);
            if (optionalPercentage.isEmpty()) {
                log.error("optionalPercentage not present");
            }
            return optionalPercentage.orElse(0d);
        } catch (Exception e) {
            model.getErrorMessage().set(Res.get("bisqEasy.price.warn.invalidPrice.outOfRange"));
            return 0;
        }
    }

    private Optional<PriceQuote> findMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(model.getMarket());
    }

    private String getCookieSubKey() {
        return model.getMarket().getMarketCodes();
    }

    private void resetInvalidPrice() {
        priceInput.setQuote(null);
        if (model.getLastValidPriceQuote() != null) {
            priceInput.setQuote(model.getLastValidPriceQuote());
        } else {
            findMarketPriceQuote().ifPresent(marketPriceQuote -> {
                PriceQuote newPriceQuote = PriceUtil.fromMarketPriceMarkup(marketPriceQuote, 0);
                if (validateQuote(newPriceQuote)) {
                    priceInput.setQuote(newPriceQuote);
                }
            });
        }
    }

    private void updateFeedback(PriceSpec priceSpec) {
        // TODO: We should show the recommended % price based on the selected amount: e.g.
        // amount range                     recommended price
        // 0.0001 BTC - 0.001 BTC           10-15%
        // 0.001 BTC - 0.01 BTC             2-10%
        Optional<Double> percentage = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, model.getMarket());
        if (percentage.isPresent()) {
            double percentageValue = percentage.get();
            String feedbackSentence;
            if (percentageValue < -0.05) {
                feedbackSentence = getFeedbackSentence(Res.get("bisqEasy.price.feedback.sentence.veryLow"));
            } else if (percentageValue < 0d) {
                feedbackSentence = getFeedbackSentence(Res.get("bisqEasy.price.feedback.sentence.low"));
            } else if (percentageValue < 0.05) {
                feedbackSentence = getFeedbackSentence(Res.get("bisqEasy.price.feedback.sentence.some"));
            } else if (percentageValue < 0.15) {
                feedbackSentence = getFeedbackSentence(Res.get("bisqEasy.price.feedback.sentence.good"));
            } else {
                feedbackSentence = getFeedbackSentence(Res.get("bisqEasy.price.feedback.sentence.veryGood"));
            }
            model.getFeedbackSentence().set(feedbackSentence);
        } else {
            model.getFeedbackSentence().set(null);
        }
    }

    private String getFeedbackSentence(String adjective) {
        return Res.get("bisqEasy.price.feedback.sentence", adjective);
    }
}
