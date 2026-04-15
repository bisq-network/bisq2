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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.passive;

import bisq.common.asset.Asset;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import lombok.Getter;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

public class MuSigPassiveAmountController implements bisq.desktop.common.view.Controller {
    private final MuSigPassiveAmountModel model;
    @Getter
    private final MuSigPassiveAmountView view;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigPassiveAmountController(ServiceProvider serviceProvider, boolean isLeftSideRangeAmount) {
        model = new MuSigPassiveAmountModel(isLeftSideRangeAmount);
        view = new MuSigPassiveAmountView(model, this);
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */


    @Override
    public void onActivate() {
        subscriptions.add(EasyBind.subscribe(model.getMarket(), market -> {
            reset();
            applyCode();
            applyFormattedAmount();
            applyTooltip();
        }));
        subscriptions.add(EasyBind.subscribe(model.getIsBaseCurrency(), isBaseCurrency -> {
            reset();
            applyCode();
            applyFormattedAmount();
            applyTooltip();
        }));
        subscriptions.add(EasyBind.subscribe(model.getAmount(), amount -> {
            applyFormattedAmount();
        }));
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        model.reset();
    }

    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void setMarket(Market market) {
        model.getMarket().set(market);
    }

    public void setAmount(Monetary value) {
        model.getAmount().set(value);
    }

    public void setIsBaseCurrency(boolean value) {
        model.getIsBaseCurrency().set(value);
    }

    public Monetary getAmount() {
        return model.getAmount().get();
    }


    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */



    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void reset() {
        model.getAmount().set(null);
        model.getFormattedAmount().set(null);
        model.getCode().set(null);
    }

    private void applyFormattedAmount() {
        Monetary amount = model.getAmount().get();
        if (amount != null) {
            model.getFormattedAmount().set(AmountFormatter.formatAmountByMonetaryType(amount));
        } else {
            model.getFormattedAmount().set(null);
        }
    }

    private void applyCode() {
        Market market = model.getMarket().get();
        if (market != null) {
            String code = getCode(market);
            model.getCode().set(code);
            model.getIsBtc().set(Asset.isBtc(code));
        } else {
            model.getCode().set(null);
        }
    }

    private void applyTooltip() {
        Market market = model.getMarket().get();
        if (market != null) {
            model.getTooltip().set(Res.get("muSig.offer.wizard.amount.display.tooltip.conversionInfo", getCode(market)));
        } else {
            model.getTooltip().set(null);
        }
    }

    private String getCode(Market market) {
        boolean isBaseCurrency = model.getIsBaseCurrency().get();
        return isBaseCurrency ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }
}
