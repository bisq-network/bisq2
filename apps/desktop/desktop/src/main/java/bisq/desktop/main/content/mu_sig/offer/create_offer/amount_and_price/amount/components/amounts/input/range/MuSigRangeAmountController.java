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
public class MuSigRangeAmountController implements Controller {
    private final MuSigRangeAmountModel model;
    @Getter
    private final MuSigRangeAmountView view;
    private final MuSigAmountTextInputController minAmountInputController;
    private final MuSigPassiveAmountController minPassiveAmountController;
    private final MuSigAmountSliderController amountSliderController;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final MuSigAmountTextInputController maxAmountInputController;
    private final MuSigPassiveAmountController maxPassiveAmountController;

    public MuSigRangeAmountController(ServiceProvider serviceProvider) {
        model = new MuSigRangeAmountModel();

        minAmountInputController = new MuSigAmountTextInputController(serviceProvider, false, true);
        maxAmountInputController = new MuSigAmountTextInputController(serviceProvider, false, false);
        minPassiveAmountController = new MuSigPassiveAmountController(serviceProvider, true);
        maxPassiveAmountController = new MuSigPassiveAmountController(serviceProvider, false);
        amountSliderController = new MuSigAmountSliderController(serviceProvider);

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
        subscriptions.add(EasyBind.subscribe(model.getInitialTradeAmount(),
                initialTradeAmount -> {
                    if (initialTradeAmount != null) {
                        model.getMinTradeAmount().set(initialTradeAmount);
                        model.getMaxTradeAmount().set(initialTradeAmount);
                        if (model.getUseBaseCurrencyForAmountInput().get()) {
                            minAmountInputController.setAmount(initialTradeAmount.getBaseSideAmount());
                            maxAmountInputController.setAmount(initialTradeAmount.getBaseSideAmount());
                            minPassiveAmountController.setAmount(initialTradeAmount.getQuoteSideAmount());
                            maxPassiveAmountController.setAmount(initialTradeAmount.getQuoteSideAmount());
                        } else {
                            minAmountInputController.setAmount(initialTradeAmount.getQuoteSideAmount());
                            maxAmountInputController.setAmount(initialTradeAmount.getQuoteSideAmount());
                            minPassiveAmountController.setAmount(initialTradeAmount.getBaseSideAmount());
                            maxPassiveAmountController.setAmount(initialTradeAmount.getBaseSideAmount());
                        }
                    }
                }));

        subscriptions.add(EasyBind.subscribe(model.getMarket(),
                market -> {
                    minAmountInputController.setMarket(market);
                    maxAmountInputController.setMarket(market);
                    minPassiveAmountController.setMarket(market);
                    maxPassiveAmountController.setMarket(market);
                    applyMinPassiveAmount();
                    applyMaxPassiveAmount();
                }));

        subscriptions.add(EasyBind.subscribe(model.getUseBaseCurrencyForAmountInput(),
                useBaseCurrencyForAmountInput -> {
                    Monetary previousMinInputAmount = minAmountInputController.amountProperty().get();
                    Monetary previousMaxInputAmount = maxAmountInputController.amountProperty().get();
                    Monetary previousMinPassiveAmount = minPassiveAmountController.getAmount();
                    Monetary previousMaxPassiveAmount = maxPassiveAmountController.getAmount();

                    minAmountInputController.setIsBaseCurrency(useBaseCurrencyForAmountInput);
                    maxAmountInputController.setIsBaseCurrency(useBaseCurrencyForAmountInput);
                    minAmountInputController.setAmount(previousMinPassiveAmount);
                    maxAmountInputController.setAmount(previousMaxPassiveAmount);

                    minPassiveAmountController.setIsBaseCurrency(!useBaseCurrencyForAmountInput);
                    maxPassiveAmountController.setIsBaseCurrency(!useBaseCurrencyForAmountInput);
                    minPassiveAmountController.setAmount(previousMinInputAmount);
                    maxPassiveAmountController.setAmount(previousMaxInputAmount);

                    applyMinTradeAmount();
                    applyMaxTradeAmount();
                }));

        subscriptions.add(EasyBind.subscribe(model.getPriceQuote(),
                priceQuote -> {
                    applyMinPassiveAmount();
                    applyMaxPassiveAmount();
                    applyMinTradeAmount();
                    applyMaxTradeAmount();
                }));

        subscriptions.add(EasyBind.subscribe(minAmountInputController.amountProperty(),
                amount -> {
                    applyMinPassiveAmount();
                    applyMaxPassiveAmount();
                    applyMinTradeAmount();
                    applyMaxTradeAmount();
                }));


        subscriptions.add(EasyBind.subscribe(minAmountInputController.textInputProperty(),
                textInput -> {
                    model.getMinAmountString().set(textInput);
                    applySumNumChars();
                }));
        subscriptions.add(EasyBind.subscribe(maxAmountInputController.textInputProperty(),
                textInput -> {
                    model.getMaxAmountString().set(textInput);
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

    private void applyMinTradeAmount() {
        boolean useBaseCurrencyForAmountInput = model.getUseBaseCurrencyForAmountInput().get();
        model.getMinTradeAmount().set(useBaseCurrencyForAmountInput
                ? new TradeAmount(minAmountInputController.amountProperty().get(), minPassiveAmountController.getAmount())
                : new TradeAmount(minPassiveAmountController.getAmount(), minAmountInputController.amountProperty().get()));
    }

    private void applyMaxTradeAmount() {
        boolean useBaseCurrencyForAmountInput = model.getUseBaseCurrencyForAmountInput().get();
        model.getMaxTradeAmount().set(useBaseCurrencyForAmountInput
                ? new TradeAmount(maxAmountInputController.amountProperty().get(), maxPassiveAmountController.getAmount())
                : new TradeAmount(maxPassiveAmountController.getAmount(), maxAmountInputController.amountProperty().get()));
    }

    private void applyMinPassiveAmount() {
        Monetary inputAmount = minAmountInputController.amountProperty().get();
        Monetary passiveAmount = toPassiveAmount(inputAmount, model.getUseBaseCurrencyForAmountInput().get());
        minPassiveAmountController.setAmount(passiveAmount);
    }

    private void applyMaxPassiveAmount() {
        Monetary inputAmount = maxAmountInputController.amountProperty().get();
        Monetary passiveAmount = toPassiveAmount(inputAmount, model.getUseBaseCurrencyForAmountInput().get());
        maxPassiveAmountController.setAmount(passiveAmount);
    }


    private void applySumNumChars() {
        String minAmountString = minAmountInputController.textInputProperty().get();
        int minAmountStringLength = minAmountString != null ? minAmountString.length() : 0;
        String maxAmountString = maxAmountInputController.textInputProperty().get();
        int maxAmountStringLength = maxAmountString != null ? maxAmountString.length() : 0;
        int sumOfNumChars = minAmountStringLength + maxAmountStringLength + 1; // for dash
        minAmountInputController.setSumOfNumChars(sumOfNumChars);
        maxAmountInputController.setSumOfNumChars(sumOfNumChars);
        model.getSumOfNumChars().set(sumOfNumChars);
    }

    private void applyIsTextInputFocused() {
        boolean focused = minAmountInputController.focusedProperty().get() || maxAmountInputController.focusedProperty().get();
        model.getIsTextInputFocused().set(focused);
    }

    private String getCode(Market market) {
        boolean useBaseCurrencyForAmountInput = model.getUseBaseCurrencyForAmountInput().get();
        return useBaseCurrencyForAmountInput ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }
}
