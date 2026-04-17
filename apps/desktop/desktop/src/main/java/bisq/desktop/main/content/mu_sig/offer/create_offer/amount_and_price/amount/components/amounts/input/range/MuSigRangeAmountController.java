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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.range;

import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.TradeAmount;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.input.MuSigAmountTextInputController;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.slider.MuSigAmountSliderController;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.passive.MuSigPassiveAmountController;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import javafx.beans.property.ReadOnlyBooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigRangeAmountController implements Controller {
    private final MuSigRangeAmountModel model;
    @Getter
    private final MuSigRangeAmountView view;
    private final MuSigAmountTextInputController minAmountInputController;
    private final MuSigPassiveAmountController minPassiveAmountController;
    private final MuSigAmountSliderController amountSliderController;
    private final MuSigAmountTextInputController maxAmountInputController;
    private final MuSigPassiveAmountController maxPassiveAmountController;
    private final CreateOfferDraftWorkflow createOfferDraftWorkflow;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();

    public MuSigRangeAmountController(ServiceProvider serviceProvider,
                                      CreateOfferDraftWorkflow createOfferDraftWorkflow) {
        this.createOfferDraftWorkflow = createOfferDraftWorkflow;
        model = new MuSigRangeAmountModel();

        minAmountInputController = new MuSigAmountTextInputController(serviceProvider, createOfferDraftWorkflow, false, true);
        maxAmountInputController = new MuSigAmountTextInputController(serviceProvider, createOfferDraftWorkflow, false, false);
        minPassiveAmountController = new MuSigPassiveAmountController(serviceProvider, createOfferDraftWorkflow, true);
        maxPassiveAmountController = new MuSigPassiveAmountController(serviceProvider, createOfferDraftWorkflow, false);
        amountSliderController = new MuSigAmountSliderController(serviceProvider, createOfferDraftWorkflow);

        view = new MuSigRangeAmountView(model, this,
                minAmountInputController.getView().getRoot(),
                maxAmountInputController.getView().getRoot(),
                minPassiveAmountController.getView().getRoot(),
                maxPassiveAmountController.getView().getRoot(),
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
            UIThread.run(() -> {
                applyAllAmounts();
            });
        }));

        pins.add(createOfferDraftWorkflow.minTradeAmountObservable().addObserver(tradeAmount -> {
            UIThread.run(() -> {
                applyAllAmounts();
            });
        }));
        pins.add(createOfferDraftWorkflow.maxTradeAmountObservable().addObserver(tradeAmount -> {
            UIThread.run(() -> {
                applyAllAmounts();
            });
        }));

        subscriptions.add(EasyBind.subscribe(minAmountInputController.amountProperty(),
                amount -> {
                    createOfferDraftWorkflow.setMinTradeAmountFromInputAmount(amount);
                }));


        subscriptions.add(EasyBind.subscribe(maxAmountInputController.amountProperty(),
                amount -> {
                    createOfferDraftWorkflow.setMaxTradeAmountFromInputAmount(amount);
                }));

        // UI specific
        subscriptions.add(EasyBind.subscribe(minAmountInputController.inputTextProperty(),
                inputText -> {
                    model.getMinAmountInputText().set(inputText);
                    applySumNumChars();
                }));
        subscriptions.add(EasyBind.subscribe(maxAmountInputController.inputTextProperty(),
                inputText -> {
                    model.getMaxAmountInputText().set(inputText);
                    applySumNumChars();
                }));

        subscriptions.add(EasyBind.subscribe(model.getMinAmountWidth(), width -> {
            if (width != null) {
                minAmountInputController.setAmountFieldWidth(width.doubleValue());
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getDashWidth(), width -> {
            if (width != null) {
                minAmountInputController.setDashFieldWidth(width.doubleValue());
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getMaxAmountWidth(), width -> {
            if (width != null) {
                maxAmountInputController.setAmountFieldWidth(width.doubleValue());
            }
        }));


        subscriptions.add(EasyBind.subscribe(minAmountInputController.focusedProperty(),
                focused -> {
                    applyIsTextInputFocused();
                }));

        subscriptions.add(EasyBind.subscribe(maxAmountInputController.focusedProperty(),
                focused -> {
                    applyIsTextInputFocused();
                }));
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
        boolean value = !createOfferDraftWorkflow.getUseBaseCurrencyForAmountInput();
        createOfferDraftWorkflow.setUseBaseCurrencyForAmountInput(value);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void applyAllAmounts() {
        applyMinInputAmount();
        applyMaxInputAAmount();
        applyMinPassiveAmount();
        applyMaxPassiveAmount();
    }

    private void applyMinInputAmount() {
        TradeAmount tradeAmount = createOfferDraftWorkflow.getMinTradeAmount();
        Monetary inputAmount = createOfferDraftWorkflow.getInputAmount(tradeAmount);
        minAmountInputController.setAmount(inputAmount);
    }

    private void applyMaxInputAAmount() {
        TradeAmount tradeAmount = createOfferDraftWorkflow.getMaxTradeAmount();
        Monetary inputAmount = createOfferDraftWorkflow.getInputAmount(tradeAmount);
        maxAmountInputController.setAmount(inputAmount);
    }

    private void applyMinPassiveAmount() {
        TradeAmount tradeAmount = createOfferDraftWorkflow.getMinTradeAmount();
        Monetary passiveAmount = createOfferDraftWorkflow.getPassiveAmount(tradeAmount);
        minPassiveAmountController.setAmount(passiveAmount);
    }

    private void applyMaxPassiveAmount() {
        TradeAmount tradeAmount = createOfferDraftWorkflow.getMaxTradeAmount();
        Monetary passiveAmount = createOfferDraftWorkflow.getPassiveAmount(tradeAmount);
        maxPassiveAmountController.setAmount(passiveAmount);
    }

    private void applySumNumChars() {
        String minAmountInputText = minAmountInputController.inputTextProperty().get();
        int minAmountInputTextLength = minAmountInputText != null ? minAmountInputText.length() : 0;
        String maxAmountInputText = maxAmountInputController.inputTextProperty().get();
        int maxAmountInputTextLength = maxAmountInputText != null ? maxAmountInputText.length() : 0;
        int sumOfNumChars = minAmountInputTextLength + maxAmountInputTextLength + 1; // for dash
        minAmountInputController.setSumOfNumChars(sumOfNumChars);
        maxAmountInputController.setSumOfNumChars(sumOfNumChars);
        model.getSumOfNumChars().set(sumOfNumChars);
    }

    private void applyIsTextInputFocused() {
        boolean focused = minAmountInputController.focusedProperty().get() || maxAmountInputController.focusedProperty().get();
        model.getIsTextInputFocused().set(focused);
    }

    private String getCode(Market market) {
        boolean useBaseCurrencyForAmountInput = createOfferDraftWorkflow.getUseBaseCurrencyForAmountInput();
        return useBaseCurrencyForAmountInput ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }
}
