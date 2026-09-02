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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.price;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.offer.draft.components.MuSigPriceInput;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.direction.DirectionSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.price.spec.PriceSpecUtil;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static bisq.presentation.parser.PercentageParser.parse;

@Slf4j
public class MuSigCreateOfferPriceController implements Controller {
    private final MuSigCreateOfferPriceModel model;
    @Getter
    private final MuSigCreateOfferPriceView view;
    private final MuSigPriceInput priceInput;
    private final Region owner;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final MarketPriceService marketPriceService;
    private final SettingsService settingsService;
    private final Set<Pin> pins = new HashSet<>();
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final DirectionSelection directionSelection;
    private final PriceSelection priceSelection;
    private final MarketSelection marketSelection;

    public MuSigCreateOfferPriceController(ServiceProvider serviceProvider,
                                           CreateOfferUseCase createOfferUseCase,
                                           Region owner,
                                           Consumer<Boolean> navigationButtonsVisibleHandler) {
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        settingsService = serviceProvider.getSettingsService();
        marketSelection = createOfferUseCase.getMarketSelection();
        directionSelection = createOfferUseCase.getDirectionSelection();
        priceSelection = createOfferUseCase.getPriceSelection();

        priceInput = new MuSigPriceInput(serviceProvider.getBondedRolesService().getMarketPriceService(), createOfferUseCase);
        this.owner = owner;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new MuSigCreateOfferPriceModel();
        view = new MuSigCreateOfferPriceView(model, this, priceInput.getRoot());
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

    public ReadOnlyBooleanProperty getIsOverlayVisible() {
        return model.getIsOverlayVisible();
    }

    @Override
    public void onActivate() {
        Market market = marketSelection.getMarket();
        String marketCodes = market.getMarketCodes();
        model.getMarketCodes().set(marketCodes);

        model.getUseFixPrice().set(priceSelection.getUseFixPrice());

        //todo
        applyPriceSliderValue(0d);
       /*
        settingsService.getCookie().asString(CookieKey.CREATE_OFFER_PRICE)
                .ifPresentOrElse(
                        this::applyPriceFromCookie,
                        () -> applyPriceSliderValue(0d)
                );*/

        if (model.getPriceSpec().get() == null) {
            model.getPriceSpec().set(new MarketPriceSpec());
        }

        pins.add(priceSelection.useFixPriceObservable().addObserver(useFixPrice -> {
            UIThread.run(() -> {
                // In case of in invalid inputs we apply the value from the flip side before switching,
                // so that the then inactive field has a valid value again.
                if (!useFixPrice && !priceInput.isPriceValid().get()) {
                    applyPercentageString(model.getPercentageInput().get());
                } else if (useFixPrice && model.getErrorMessage().get() != null) {
                    onQuoteInput(priceSelection.getPriceQuote());
                }
                model.getUseFixPrice().set(useFixPrice);
                applyPriceSpec();
            });
        }));
        pins.add(priceSelection.pricePercentageObservable().addObserver(pricePercentage -> {
            UIThread.run(() -> {
                if (pricePercentage != null) {
                    model.getPercentage().set(pricePercentage);
                    model.getPercentageInput().set(PercentageFormatter.formatToPercent(pricePercentage));
                }
            });
        }));


        pins.add(priceSelection.priceQuoteObservable().addObserver(priceQuote ->
                UIThread.run(() -> onQuoteInput(priceQuote))));

        subscriptions.add(EasyBind.subscribe(priceInput.isPriceValid(), isPriceValid -> {
            if (isPriceValid != null && !isPriceValid) {
                model.getErrorMessage().set(priceInput.getErrorMessage());
                model.setLastValidPriceQuote(null);
            } else {
                model.getErrorMessage().set(null);
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getPriceSpec(), this::updateFeedback));
        subscriptions.add(EasyBind.subscribe(model.getPercentageInput(), percentageInput -> {
            if (percentageInput != null) {
                onPercentageInput(percentageInput);
                priceInput.setPercentage(percentageInput);
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getPriceSliderValue(), priceSliderValue -> {
            if (priceSliderValue != null) {
                double value = priceSliderValue.doubleValue() * (model.getMaxPercentage() - model.getMinPercentage()) + model.getMinPercentage();
                String percentageAsString = PercentageFormatter.formatToPercent(value);
                onPercentageInput(percentageAsString);
                priceInput.setPercentage(percentageAsString);
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getPercentage(), percentage -> {
            if (percentage != null) {
                applyPriceSliderValue(percentage.doubleValue());
            }
        }));

        priceInput.setDescription(Res.get("muSig.offer.create.price.tradePrice.inputBoxText", marketCodes));

        // We keep the feedback and overlay code for now as we might have usage later for it.
        // If not, we can remove all related code. Currently, it's just a copy of Bisq Easy...
        model.getShouldShowFeedback().set(false);

        applyPriceSpec();
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pins.forEach(Pin::unbind);
        pins.clear();

        model.getIsOverlayVisible().set(false);
        model.setShouldFocusPriceComponent(false);
        navigationButtonsVisibleHandler.accept(true);
    }

    void onPercentageFocused(boolean focused) {
        model.setFocused(focused);
        if (!focused) {
            try {
                double percentage = parse(model.getPercentageInput().get());
                String percentageAsString = PercentageFormatter.formatToPercent(percentage);
                // Need to change the value first otherwise it does not trigger an update
                model.getPercentageInput().set("");
                model.getPercentageInput().set(percentageAsString);
                onPercentageInput(percentageAsString);
            } catch (Exception e) {
                model.getErrorMessage().set(Res.get("muSig.offer.create.price.warn.invalidPrice.numberFormatException"));
            }
        }
    }

    void onPriceComponentUpdated() {
        if (!model.isShouldFocusPriceComponent()) {
            model.setShouldFocusPriceComponent(true);
        }
    }

    void onUpdatePriceSpec() {
        if (model.getUseFixPrice().get()) {
            boolean shouldRequestFocus = model.isShouldFocusPriceComponent();
            priceInput.activate(shouldRequestFocus);
        } else {
            priceInput.deactivate();
        }
    }

    private void onPercentageInput(String percentageAsString) {
        if (model.isFocused()) {
            return;
        }
        applyPercentageString(percentageAsString);
    }

    private void applyPercentageString(String percentageAsString) {
        if (StringUtils.isEmpty(percentageAsString)) {
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
            model.getErrorMessage().set(Res.get("muSig.offer.create.price.warn.invalidPrice.numberFormatException"));
        } catch (Exception e) {
            model.getErrorMessage().set(Res.get("muSig.offer.create.price.warn.invalidPrice.exception", e.getMessage()));
        }
    }

    void onToggleUseFixPrice() {
        boolean useFixPrice = !model.getUseFixPrice().get();
        priceSelection.onSetUseFixPrice(useFixPrice);
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

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseOverlay);
    }

    void onShowOverlay() {
        if (!model.getIsOverlayVisible().get()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getIsOverlayVisible().set(true);
        }
    }

    void onCloseOverlay() {
        if (model.getIsOverlayVisible().get()) {
            navigationButtonsVisibleHandler.accept(true);
            model.getIsOverlayVisible().set(false);
        }
    }

    private void applyPriceSpec() {
        if (model.getUseFixPrice().get()) {
            model.getPriceSpec().set(new FixPriceSpec(priceSelection.getPriceQuote()));
            // settingsService.setCookie(CookieKey.CREATE_OFFER_PRICE, priceInput.getPriceString().get());
        } else {
            double percentage = model.getPercentage().get();
            if (percentage == 0) {
                model.getPriceSpec().set(new MarketPriceSpec());
            } else {
                model.getPriceSpec().set(new FloatPriceSpec(percentage));
            }
            // settingsService.setCookie(CookieKey.CREATE_OFFER_PRICE, model.getPercentageInput().get());
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
        double pricePercentage = getPercentageFromPriceQuote(priceQuote);
        priceSelection.onSetPricePercentage(pricePercentage);
    }

    private void applyPriceSliderValue(double percentage) {
        // Only apply value from component to slider if we have no focus on slider (not used)
        if (!model.getSliderFocus().get()) {
            double sliderValue = (percentage - model.getMinPercentage()) / (model.getMaxPercentage() - model.getMinPercentage());
            model.getPriceSliderValue().set(sliderValue);
        }
    }

    private boolean validateQuote(PriceQuote priceQuote) {
        return validatePercentage(getPercentageFromPriceQuote(priceQuote));
    }

    private boolean validatePercentage(double percentage) {
        if (percentage >= model.getMinPercentage() && percentage <= model.getMaxPercentage()) {
            model.getErrorMessage().set(null);
            return true;
        } else {
            model.getErrorMessage().set(Res.get("muSig.offer.create.price.warn.invalidPrice.outOfRange"));
            return false;
        }
    }

    private double getPercentageFromPriceQuote(PriceQuote priceQuote) {
        try {
            Optional<Double> optionalPercentage = PriceSpecUtil.createFloatPriceAsPercentage(marketPriceService, priceQuote);
            if (optionalPercentage.isEmpty()) {
                log.error("optionalPercentage not present");
            }
            return optionalPercentage.orElse(0d);
        } catch (Exception e) {
            model.getErrorMessage().set(Res.get("muSig.offer.create.price.warn.invalidPrice.outOfRange"));
            return 0;
        }
    }

    private Optional<PriceQuote> findMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(marketSelection.getMarket());
    }

    private String getCookieSubKey() {
        return marketSelection.getMarket().getMarketCodes();
    }

    private void updateFeedback(PriceSpec priceSpec) {
        // TODO: We should show the recommended % price based on the selected amount: e.g.
        // amount range                     recommended price
        // 0.0001 BTC - 0.001 BTC           10-15%
        // 0.001 BTC - 0.01 BTC             2-10%
        Optional<Double> percentage = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, marketSelection.getMarket());
        if (percentage.isPresent()) {
            double percentageValue = percentage.get();
            String feedbackSentence;
            if (percentageValue < -0.05) {
                feedbackSentence = getFeedbackSentence(Res.get("muSig.offer.create.price.feedback.sentence.veryLow"));
            } else if (percentageValue < 0d) {
                feedbackSentence = getFeedbackSentence(Res.get("muSig.offer.create.price.feedback.sentence.low"));
            } else if (percentageValue < 0.05) {
                feedbackSentence = getFeedbackSentence(Res.get("muSig.offer.create.price.feedback.sentence.some"));
            } else if (percentageValue < 0.15) {
                feedbackSentence = getFeedbackSentence(Res.get("muSig.offer.create.price.feedback.sentence.good"));
            } else {
                feedbackSentence = getFeedbackSentence(Res.get("muSig.offer.create.price.feedback.sentence.veryGood"));
            }

            model.getShouldShowWarningIcon().set(percentageValue < 0.05);
            model.getFeedbackSentence().set(feedbackSentence);
        } else {
            model.getFeedbackSentence().set(null);
        }
    }

    private String getFeedbackSentence(String adjective) {
        return directionSelection.getDisplayDirection().isBuy()
                ? Res.get("muSig.offer.create.price.feedback.buyOffer.sentence", adjective)
                : Res.get("muSig.offer.create.price.feedback.sellOffer.sentence", adjective);
    }

    //todo
    private void applyPriceFromCookie(String price) {
        if (model.getUseFixPrice().get()) {
            priceInput.setPriceString(price);
            applyPercentageFromQuote(priceSelection.getPriceQuote());
            applyPriceSliderValue(model.getPercentage().get());
        } else {
            try {
                double percentage = parse(price);
                if (!validatePercentage(percentage)) {
                    return;
                }
                applyPriceSliderValue(percentage);
            } catch (Exception e) {
                log.error("Unable to apply price in cookie.", e);
            }
        }
    }
}
