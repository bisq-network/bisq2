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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount_and_price;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.trade_wizard.amount.TradeWizardAmountController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.price.TradeWizardPriceController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideAmountSpec;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class TradeWizardAmountAndPriceController implements Controller {
    private final TradeWizardAmountAndPriceModel model;
    @Getter
    private final TradeWizardAmountAndPriceView view;
    private final Region owner;
    private final TradeWizardAmountController tradeWizardAmountController;
    private final TradeWizardPriceController tradeWizardPriceController;

    public TradeWizardAmountAndPriceController(ServiceProvider serviceProvider,
                                               Region owner,
                                               Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.owner = owner;
        tradeWizardAmountController = new TradeWizardAmountController(serviceProvider, owner, navigationButtonsVisibleHandler);
        tradeWizardPriceController = new TradeWizardPriceController(serviceProvider, owner);

        model = new TradeWizardAmountAndPriceModel();
        view = new TradeWizardAmountAndPriceView(model,
                this,
                tradeWizardAmountController.getView().getRoot(),
                tradeWizardAmountController.getView().getAmountLimitInfoOverlay(),
                tradeWizardPriceController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        model.setHeadline(getHeadline());
        model.getIsAmountOverlayVisible().bind(tradeWizardAmountController.getIsAmountOverlayVisible());
    }

    @Override
    public void onDeactivate() {
        model.getIsAmountOverlayVisible().unbind();
    }

    public void reset() {
        tradeWizardAmountController.reset();
        tradeWizardPriceController.reset();
        model.reset();
    }

    public void setIsCreateOfferMode(boolean isCreateOfferMode) {
        tradeWizardAmountController.setIsCreateOfferMode(isCreateOfferMode);
        model.setCreateOfferMode(isCreateOfferMode);
        model.setShowPriceSelection(isCreateOfferMode);
    }

    public void setDirection(Direction direction) {
        tradeWizardAmountController.setDirection(direction);
        tradeWizardPriceController.setDirection(direction);
        model.setDirection(direction);
    }

    public void setMarket(Market market) {
        tradeWizardAmountController.setMarket(market);
        tradeWizardPriceController.setMarket(market);
    }

    public void updateQuoteSideAmountSpecWithPriceSpec(PriceSpec priceSpec) {
        tradeWizardAmountController.updateQuoteSideAmountSpecWithPriceSpec(priceSpec);
    }

    public ReadOnlyObjectProperty<QuoteSideAmountSpec> getQuoteSideAmountSpec() {
        return tradeWizardAmountController.getQuoteSideAmountSpec();
    }

    public void setBitcoinPaymentMethods(List<BitcoinPaymentMethod> bitcoinPaymentMethods) {
        tradeWizardAmountController.setBitcoinPaymentMethods(bitcoinPaymentMethods);
    }

    public void setFiatPaymentMethods(List<FiatPaymentMethod> fiatPaymentMethods) {
        tradeWizardAmountController.setFiatPaymentMethods(fiatPaymentMethods);
    }

    public boolean validate() {
        return tradeWizardAmountController.validate()
                && tradeWizardPriceController.validate();
    }

    public ReadOnlyObjectProperty<PriceSpec> getPriceSpec() {
        return tradeWizardPriceController.getPriceSpec();
    }

    private String getHeadline() {
        if (model.isCreateOfferMode()) {
            return Res.get("bisqEasy.tradeWizard.amountAtPrice.headline");
        }

        return model.getDirection().isBuy()
                ? Res.get("bisqEasy.tradeWizard.amount.headline.buyer")
                : Res.get("bisqEasy.tradeWizard.amount.headline.seller");
    }
}
