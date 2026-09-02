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
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final Set<Pin> pins = new HashSet<>();
    private final Set<Subscription> subscriptions = new HashSet<>();
    private boolean applyingDomainValue;
    private final DirectionSelection directionSelection;
    private final PriceSelection priceSelection;
    private final MarketSelection marketSelection;

    public MuSigCreateOfferPriceController(ServiceProvider serviceProvider,
                                           CreateOfferUseCase createOfferUseCase,
                                           Region owner,
                                           Consumer<Boolean> navigationButtonsVisibleHandler) {
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        marketSelection = createOfferUseCase.getMarketSelection();
        directionSelection = createOfferUseCase.getDirectionSelection();
        priceSelection = createOfferUseCase.getPriceSelection();

        priceInput = new MuSigPriceInput(createOfferUseCase);
        this.owner = owner;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new MuSigCreateOfferPriceModel();
        view = new MuSigCreateOfferPriceView(model, this, priceInput.getRoot(), priceInput.getTextInputControl());
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

        if (model.getPriceSpec().get() == null) {
            model.getPriceSpec().set(new MarketPriceSpec());
        }

        // The three domain observers are display-only projections. They re-read the current
        // domain state when executed and never write back: the domain already keeps quote and
        // percentage consistent on its authoritative side, and converting a projection back into
        // input is what used to drift the floating percentage and rewrite fixed prices.
        pins.add(priceSelection.useFixPriceObservable().addObserver(ignored ->
                UIThread.run(() -> {
                    model.getUseFixPrice().set(priceSelection.getUseFixPrice());
                    // The newly inactive field's validation state must not keep blocking
                    // navigation while the field is hidden.
                    model.getErrorMessage().set(null);
                    applyPriceSpecFromDomain();
                })));
        pins.add(priceSelection.pricePercentageObservable().addObserver(ignored ->
                UIThread.run(() -> renderPercentage(priceSelection.getPricePercentage()))));
        pins.add(priceSelection.priceQuoteObservable().addObserver(ignored ->
                UIThread.run(() -> {
                    PriceQuote quote = priceSelection.getPriceQuote();
                    model.getPriceAsString().set(quote == null ? "" : PriceFormatter.format(quote, true));
                    applyPriceSpecFromDomain();
                })));

        subscriptions.add(EasyBind.subscribe(priceInput.isPriceValid(), isPriceValid -> {
            if (isPriceValid != null && !isPriceValid) {
                model.getErrorMessage().set(priceInput.getErrorMessage());
            } else {
                model.getErrorMessage().set(null);
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getPriceSpec(), this::updateFeedback));
        // EasyBind fires each subscription once at registration with the retained model value.
        // That fire is a projection, not user input, and must not be committed to the domain.
        subscriptions.add(EasyBind.subscribe(model.getPercentageInput(), skipInitialFire(percentageInput -> {
            if (applyingDomainValue || percentageInput == null) {
                return;
            }
            onPercentageInput(percentageInput);
            priceInput.setPercentage(percentageInput);
        })));
        subscriptions.add(EasyBind.subscribe(model.getPriceSliderValue(), skipInitialFire(priceSliderValue -> {
            if (applyingDomainValue || priceSliderValue == null) {
                return;
            }
            if (priceSelection.getUseFixPrice()) {
                // The slider is disabled while the fixed price is authoritative; ignore residual events.
                return;
            }
            double value = priceSliderValue.doubleValue() * (model.getMaxPercentage() - model.getMinPercentage()) + model.getMinPercentage();
            commitPercentage(value);
        })));

        priceInput.setDescription(Res.get("muSig.offer.create.price.tradePrice.inputBoxText", marketCodes));

        // We keep the feedback and overlay code for now as we might have usage later for it.
        // If not, we can remove all related code. Currently, it's just a copy of Bisq Easy...
        model.getShouldShowFeedback().set(false);

        applyPriceSpecFromDomain();
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
            onPercentageInput(model.getPercentageInput().get());
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
        if (StringUtils.isEmpty(percentageAsString)) {
            // A cleared field is an aborted edit; the display returns to the authoritative value
            // and an error from a previous malformed entry must not keep blocking navigation.
            model.getErrorMessage().set(null);
            renderPercentage(priceSelection.getPricePercentage());
            return;
        }
        try {
            commitPercentage(parse(percentageAsString));
        } catch (NumberFormatException e) {
            model.getErrorMessage().set(Res.get("muSig.offer.create.price.warn.invalidPrice.numberFormatException"));
        } catch (Exception e) {
            model.getErrorMessage().set(Res.get("muSig.offer.create.price.warn.invalidPrice.exception", e.getMessage()));
        }
    }

    private void commitPercentage(double percentage) {
        if (!Double.isFinite(percentage)) {
            model.getErrorMessage().set(Res.get("muSig.offer.create.price.warn.invalidPrice.numberFormatException"));
            return;
        }
        model.getErrorMessage().set(null);
        // The domain clamps to the allowed range; finite out-of-range input is a successful
        // clamped edit, not an error.
        priceSelection.onSetPricePercentage(percentage);
        // Re-render the authoritative value: clamping onto an already-stored bound emits no
        // observable event.
        renderPercentage(priceSelection.getPricePercentage());
        applyPriceSpecFromDomain();
    }

    private static <T> Consumer<T> skipInitialFire(Consumer<T> delegate) {
        AtomicBoolean first = new AtomicBoolean(true);
        return value -> {
            if (first.getAndSet(false)) {
                return;
            }
            delegate.accept(value);
        };
    }

    private void renderPercentage(double pricePercentage) {
        applyingDomainValue = true;
        try {
            String percentageAsString = PercentageFormatter.formatToPercent(pricePercentage);
            model.getPercentageInput().set(percentageAsString);
            priceInput.setPercentage(percentageAsString);
            applyPriceSliderValue(pricePercentage);
        } finally {
            applyingDomainValue = false;
        }
    }

    private void applyPriceSpecFromDomain() {
        if (priceSelection.getUseFixPrice() && priceSelection.getPriceQuote() == null) {
            return;
        }
        model.getPriceSpec().set(priceSelection.createAndGetPriceSpec());
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


    private void applyPriceSliderValue(double percentage) {
        // Only apply value from component to slider if we have no focus on slider (not used)
        if (!model.getSliderFocus().get()) {
            double sliderValue = (percentage - model.getMinPercentage()) / (model.getMaxPercentage() - model.getMinPercentage());
            model.getPriceSliderValue().set(sliderValue);
        }
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

}
