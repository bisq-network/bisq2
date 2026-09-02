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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container.fix;

import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.draft.amount_components.passive.MuSigPassiveAmountController;
import bisq.desktop.main.content.mu_sig.offer.draft.amount_components.text_input.MuSigAmountTextInputController;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container.fix.slider.MuSigFixAmountSliderController;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.amount.AmountSelection;
import javafx.beans.property.ReadOnlyBooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigFixAmountController implements Controller {
    private final MuSigFixAmountModel model;
    @Getter
    private final MuSigFixAmountView view;
    private final MuSigAmountTextInputController amountTextInputController;
    private final MuSigPassiveAmountController passiveAmountController;
    private final AmountSelection amountSelection;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();

    public MuSigFixAmountController(CreateOfferUseCase createOfferUseCase) {
        amountSelection = createOfferUseCase.getAmountSelection();
        model = new MuSigFixAmountModel();

        amountTextInputController = new MuSigAmountTextInputController(true, false);
        passiveAmountController = new MuSigPassiveAmountController(false);
        MuSigFixAmountSliderController amountSliderController = new MuSigFixAmountSliderController(createOfferUseCase);

        view = new MuSigFixAmountView(model, this,
                amountTextInputController.getView().getRoot(),
                passiveAmountController.getView().getRoot(),
                amountSliderController.getView().getRoot()
        );
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        // Domain specific
        pins.add(amountSelection.useBaseCurrencyForAmountInputObservable().addObserver(useBaseCurrencyForAmountInput -> {
            UIThread.run(this::applyAllAmounts);
        }));

        pins.add(amountSelection.fixTradeAmountObservable().addObserver(tradeAmount -> {
            UIThread.run(this::applyAllAmounts);
        }));

        subscriptions.add(EasyBind.subscribe(amountTextInputController.amountProperty(),
                amount -> {
                    amountSelection.onSetFixTradeAmountFromInputAmount(amount);
                    applyInputAmount();
                }));

        // UI specific
        subscriptions.add(EasyBind.subscribe(amountTextInputController.inputTextProperty(),
                inputText -> {
                    model.getAmountInputText().set(inputText);
                    applySumNumChars();
                }));

        subscriptions.add(EasyBind.subscribe(model.getAmountInputFieldWidth(), width -> {
            if (width != null) {
                amountTextInputController.setAmountFieldWidth(width.doubleValue());
            }
        }));

        model.getIsTextInputFocused().bind(amountTextInputController.focusedProperty());
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pins.forEach(Pin::unbind);
        pins.clear();
        model.getIsTextInputFocused().unbind();
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public ReadOnlyBooleanProperty getIsTextInputFocused() {
        return model.getIsTextInputFocused();
    }


    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */

    void onToggleInputMode() {
        boolean value = !amountSelection.getUseBaseCurrencyForAmountInput();
        amountSelection.onSetUseBaseCurrencyForAmountInput(value);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void applyAllAmounts() {
        applyInputAmount();
        applyPassiveAmount();
    }

    private void applyInputAmount() {
        Monetary inputAmount = amountSelection.getFixInputAmount();
        amountTextInputController.setAmount(inputAmount);
    }

    private void applyPassiveAmount() {
        Monetary passiveAmount = amountSelection.getFixPassiveAmount();
        passiveAmountController.setAmount(passiveAmount);
    }

    private void applySumNumChars() {
        String inputText = amountTextInputController.inputTextProperty().get();
        int amountStringLength = inputText != null ? inputText.length() : 0;
        amountTextInputController.setSumOfNumChars(amountStringLength);
        model.getSumOfNumChars().set(amountStringLength);
    }
}
