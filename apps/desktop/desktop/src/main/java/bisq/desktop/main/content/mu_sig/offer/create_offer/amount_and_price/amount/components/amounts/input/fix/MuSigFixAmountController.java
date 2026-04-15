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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.fix;

import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.input.MuSigAmountTextInputController;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.slider.MuSigAmountSliderController;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.passive.MuSigPassiveAmountController;
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
    private final MuSigFixAmountModel model;
    @Getter
    private final MuSigFixAmountView view;
    private final MuSigAmountTextInputController amountTextInputController;
    private final MuSigPassiveAmountController passiveAmountController;
    private final MuSigAmountSliderController amountSliderController;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigFixAmountController(ServiceProvider serviceProvider) {
        model = new MuSigFixAmountModel();

        amountTextInputController = new MuSigAmountTextInputController(serviceProvider, true, false);
        passiveAmountController = new MuSigPassiveAmountController(serviceProvider, false);
        amountSliderController = new MuSigAmountSliderController(serviceProvider);

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
        subscriptions.add(EasyBind.subscribe(model.getInitialTradeAmount(),
                initialTradeAmount -> {
                    if (initialTradeAmount != null) {
                        model.getTradeAmount().set(initialTradeAmount);
                        if (model.getUseBaseCurrencyForAmountInput().get()) {
                            amountTextInputController.setAmount(initialTradeAmount.getBaseSideAmount());
                            passiveAmountController.setAmount(initialTradeAmount.getQuoteSideAmount());
                        } else {
                            amountTextInputController.setAmount(initialTradeAmount.getQuoteSideAmount());
                            passiveAmountController.setAmount(initialTradeAmount.getBaseSideAmount());
                        }
                    }
                }));

        subscriptions.add(EasyBind.subscribe(model.getMarket(),
                market -> {
                    amountTextInputController.setMarket(market);
                    passiveAmountController.setMarket(market);
                    applyPassiveAmount();
                }));

        subscriptions.add(EasyBind.subscribe(model.getUseBaseCurrencyForAmountInput(),
                useBaseCurrencyForAmountInput -> {
                    Monetary previousInputAmount = amountTextInputController.amountProperty().get();
                    Monetary previousDisplayAmount = passiveAmountController.getAmount();

                    amountTextInputController.setIsBaseCurrency(useBaseCurrencyForAmountInput);
                    amountTextInputController.setAmount(previousDisplayAmount);

                    passiveAmountController.setIsBaseCurrency(!useBaseCurrencyForAmountInput);
                    passiveAmountController.setAmount(previousInputAmount);

                    applyTradeAmount();
                }));

        subscriptions.add(EasyBind.subscribe(model.getPriceQuote(),
                priceQuote -> {
                    applyPassiveAmount();
                    applyTradeAmount();
                }));

        subscriptions.add(EasyBind.subscribe(amountTextInputController.amountProperty(),
                amount -> {
                    applyPassiveAmount();
                    applyTradeAmount();
                }));

        subscriptions.add(EasyBind.subscribe(amountTextInputController.textInputProperty(),
                textInput -> {
                    model.getAmountString().set(textInput);
                    applySumNumChars();
                }));

        subscriptions.add(EasyBind.subscribe(model.getAmountWidth(), width -> {
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
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void setMarket(Market value) {
        model.getMarket().set(value);
    }

    public void setPriceQuote(PriceQuote value) {
        model.getPriceQuote().set(value);
    }

    public void setInitialTradeAmount(TradeAmount value) {
        model.getInitialTradeAmount().set(value);
    }

    public ReadOnlyBooleanProperty getUseBaseCurrencyForAmountInput() {
        return model.getUseBaseCurrencyForAmountInput();
    }

    public ReadOnlyBooleanProperty getIsTextInputFocused() {
        return model.getIsTextInputFocused();
    }


    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */

    void onToggleInputMode() {
        model.getUseBaseCurrencyForAmountInput().set(!model.getUseBaseCurrencyForAmountInput().get());
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void applyTradeAmount() {
        boolean useBaseCurrencyForAmountInput = model.getUseBaseCurrencyForAmountInput().get();
        TradeAmount tradeAmount = useBaseCurrencyForAmountInput
                ? new TradeAmount(amountTextInputController.amountProperty().get(), passiveAmountController.getAmount())
                : new TradeAmount(passiveAmountController.getAmount(), amountTextInputController.amountProperty().get());
        model.getTradeAmount().set(tradeAmount);
    }

    private void applyPassiveAmount() {
        Monetary inputAmount = amountTextInputController.amountProperty().get();
        Monetary passiveAmount = toPassiveAmount(inputAmount, model.getUseBaseCurrencyForAmountInput().get());
        passiveAmountController.setAmount(passiveAmount);
    }

    private void applySumNumChars() {
        String amountString = amountTextInputController.textInputProperty().get();
        int AmountStringLength = amountString != null ? amountString.length() : 0;
        amountTextInputController.setSumOfNumChars(AmountStringLength);
        model.getSumOfNumChars().set(AmountStringLength);
    }

    @Nullable
    private Monetary toPassiveAmount(Monetary inputAmount, boolean useBaseCurrencyForAmountInput) {
        if (inputAmount == null) {
            return null;
        }
        PriceQuote priceQuote = model.getPriceQuote().get();
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
        boolean useBaseCurrencyForAmountInput = model.getUseBaseCurrencyForAmountInput().get();
        return useBaseCurrencyForAmountInput ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }
}
