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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.fix;

import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.fix.slider.MuSigFixAmountSliderController;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.components.text_input.MuSigAmountTextInputController;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.components.passive.MuSigPassiveAmountController;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import javafx.beans.property.ReadOnlyBooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigFixAmountController implements Controller {
    private final CreateOfferDraftWorkflow createOfferDraftWorkflow;
    private final MuSigFixAmountModel model;
    @Getter
    private final MuSigFixAmountView view;
    private final MuSigAmountTextInputController amountTextInputController;
    private final MuSigPassiveAmountController passiveAmountController;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();

    public MuSigFixAmountController(ServiceProvider serviceProvider,
                                    CreateOfferDraftWorkflow createOfferDraftWorkflow) {
        this.createOfferDraftWorkflow = createOfferDraftWorkflow;
        model = new MuSigFixAmountModel();

        amountTextInputController = new MuSigAmountTextInputController(serviceProvider, true, false);
        passiveAmountController = new MuSigPassiveAmountController(serviceProvider, false);
        MuSigFixAmountSliderController amountSliderController = new MuSigFixAmountSliderController(serviceProvider, createOfferDraftWorkflow);

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
        pins.add(createOfferDraftWorkflow.useBaseCurrencyForAmountInputObservable().addObserver(useBaseCurrencyForAmountInput -> {
            UIThread.run(this::applyAllAmounts);
        }));

        pins.add(createOfferDraftWorkflow.fixTradeAmountObservable().addObserver(tradeAmount -> {
            UIThread.run(this::applyAllAmounts);
        }));

        subscriptions.add(EasyBind.subscribe(amountTextInputController.amountProperty(),
                amount -> {
                    createOfferDraftWorkflow.setFixTradeAmountFromInputAmount(amount);
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
        boolean useBaseCurrencyForAmountInput = createOfferDraftWorkflow.getUseBaseCurrencyForAmountInput();
        boolean value = !useBaseCurrencyForAmountInput;
        createOfferDraftWorkflow.setUseBaseCurrencyForAmountInput(value);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void applyAllAmounts() {
        applyInputAmount();
        applyPassiveAmount();
    }

    private void applyInputAmount() {
        TradeAmount tradeAmount = createOfferDraftWorkflow.getFixTradeAmount();
        Monetary inputAmount = createOfferDraftWorkflow.toInputAmount(tradeAmount, true);
        amountTextInputController.setAmount(inputAmount);
    }

    private void applyPassiveAmount() {
        TradeAmount tradeAmount = createOfferDraftWorkflow.getFixTradeAmount();
        Monetary passiveAmount = createOfferDraftWorkflow.toPassiveAmount(tradeAmount, true);
        passiveAmountController.setAmount(passiveAmount);
    }

    private void applySumNumChars() {
        String inputText = amountTextInputController.inputTextProperty().get();
        int AmountStringLength = inputText != null ? inputText.length() : 0;
        amountTextInputController.setSumOfNumChars(AmountStringLength);
        model.getSumOfNumChars().set(AmountStringLength);
    }

    @Nullable
    private Monetary toPassiveAmount(Monetary inputAmount, boolean useBaseCurrencyForAmountInput) {
        if (inputAmount == null) {
            return null;
        }
        PriceQuote priceQuote = createOfferDraftWorkflow.getPriceQuote();
        if (priceQuote == null) {
            return null;
        }
        if (useBaseCurrencyForAmountInput) {
            return priceQuote.toQuoteSideMonetary(inputAmount);
        } else {
            return priceQuote.toBaseSideMonetary(inputAmount);
        }
    }

    private String getCode(Market market) {
        boolean useBaseCurrencyForAmountInput = createOfferDraftWorkflow.getUseBaseCurrencyForAmountInput();
        return useBaseCurrencyForAmountInput ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }
}
