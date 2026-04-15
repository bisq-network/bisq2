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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components;

import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.fix.MuSigFixAmountController;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.range.MuSigRangeAmountController;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigAmountComponentsController implements Controller {
    private final MuSigAmountComponentsModel model;
    @Getter
    private final MuSigAmountComponentsView view;
    private final MuSigFixAmountController muSigFixAmountController;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final MuSigRangeAmountController muSigRangeAmountController;

    public MuSigAmountComponentsController(ServiceProvider serviceProvider) {
        model = new MuSigAmountComponentsModel();

        muSigFixAmountController = new MuSigFixAmountController(serviceProvider);
        muSigRangeAmountController = new MuSigRangeAmountController(serviceProvider);

        view = new MuSigAmountComponentsView(model, this,
                muSigFixAmountController.getView().getRoot(),
                muSigRangeAmountController.getView().getRoot()
        );
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        subscriptions.add(EasyBind.subscribe(model.getMarket(),
                market -> {
                    applyDescription();
                }));

        subscriptions.add(EasyBind.subscribe(model.getUseRangeAmount(),
                useRangeAmount -> {
                    applyDescription();
                }));

        subscriptions.add(EasyBind.subscribe(muSigFixAmountController.getUseBaseCurrencyForAmountInput(),
                useBaseCurrencyForAmountInput -> {
                    applyDescription();
                }));
        subscriptions.add(EasyBind.subscribe(muSigRangeAmountController.getUseBaseCurrencyForAmountInput(),
                useBaseCurrencyForAmountInput -> {
                    applyDescription();
                }));

        subscriptions.add(EasyBind.subscribe(muSigFixAmountController.getIsTextInputFocused(),
                isTextInputFocused -> {
                    if (!model.getUseRangeAmount().get()) {
                        model.getIsTextInputFocused().set(isTextInputFocused);
                    }
                }));
        subscriptions.add(EasyBind.subscribe(muSigRangeAmountController.getIsTextInputFocused(),
                isTextInputFocused -> {
                    if (model.getUseRangeAmount().get()) {
                        model.getIsTextInputFocused().set(isTextInputFocused);
                    }
                }));
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();

        model.getIsTextInputFocused().unbind();
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void setMarket(Market value) {
        model.getMarket().set(value);
        muSigFixAmountController.setMarket(value);
        muSigRangeAmountController.setMarket(value);
    }

    public void setPriceQuote(PriceQuote value) {
        muSigFixAmountController.setPriceQuote(value);
        muSigRangeAmountController.setPriceQuote(value);
    }

    public void setUseRangeAmount(boolean value) {
        model.getUseRangeAmount().set(value);
    }

    public void setInitialTradeAmount(TradeAmount value) {
        muSigFixAmountController.setInitialTradeAmount(value);
        muSigRangeAmountController.setInitialTradeAmount(value);
    }

    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void applyDescription() {
        Market market = model.getMarket().get();
        if (market == null) {
            return;
        }
        boolean useRangeAmount = model.getUseRangeAmount().get();
        String code = getCode(market);
        model.getDescription().set(useRangeAmount
                ? Res.get("muSig.offer.create.amount.description.range", code)
                : Res.get("muSig.offer.create.amount.description.fixed", code));
    }

    private String getCode(Market market) {
        boolean useBaseCurrencyForAmountInput = muSigFixAmountController.getUseBaseCurrencyForAmountInput().get();
        return useBaseCurrencyForAmountInput ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }
}
