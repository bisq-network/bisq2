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

package bisq.desktop.main.content.mu_sig.create_offer.amount_and_price;

import bisq.account.payment_method.PaymentMethod;
import bisq.common.market.Market;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.create_offer.amount_and_price.amount.MuSigCreateOfferAmountController;
import bisq.desktop.main.content.mu_sig.create_offer.amount_and_price.price.MuSigCreateOfferPriceController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideAmountSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.mu_sig.MuSigOffer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class MuSigCreateOfferAmountAndPriceController implements Controller {
    private final MuSigCreateOfferAmountAndPriceModel model;
    @Getter
    private final MuSigCreateOfferAmountAndPriceView view;
    private final MuSigCreateOfferAmountController muSigCreateOfferAmountController;
    private final MuSigCreateOfferPriceController muSigCreateOfferPriceController;

    public MuSigCreateOfferAmountAndPriceController(ServiceProvider serviceProvider,
                                                    Region owner,
                                                    Consumer<Boolean> navigationButtonsVisibleHandler,
                                                    Consumer<NavigationTarget> closeAndNavigateToHandler) {
        muSigCreateOfferAmountController = new MuSigCreateOfferAmountController(serviceProvider,
                owner,
                navigationButtonsVisibleHandler,
                closeAndNavigateToHandler);
        muSigCreateOfferPriceController = new MuSigCreateOfferPriceController(serviceProvider,
                owner,
                navigationButtonsVisibleHandler);

        model = new MuSigCreateOfferAmountAndPriceModel();
        view = new MuSigCreateOfferAmountAndPriceView(model,
                this,
                muSigCreateOfferAmountController.getView().getRoot(),
                muSigCreateOfferAmountController.getView().getOverlay(),
                muSigCreateOfferPriceController.getView().getRoot(),
                muSigCreateOfferPriceController.getView().getOverlay());
    }


    @Override
    public void onActivate() {
        model.setHeadline(getHeadline());
        model.getIsAmountOverlayVisible().bind(muSigCreateOfferAmountController.getIsOverlayVisible());
        model.getIsPriceOverlayVisible().bind(muSigCreateOfferPriceController.getIsOverlayVisible());
    }

    @Override
    public void onDeactivate() {
        model.getIsAmountOverlayVisible().unbind();
        model.getIsPriceOverlayVisible().unbind();
    }

    public void reset() {
        muSigCreateOfferAmountController.reset();
        muSigCreateOfferPriceController.reset();
        model.reset();
    }

    public void setDirection(Direction direction) {
        muSigCreateOfferAmountController.setDirection(direction);
        muSigCreateOfferPriceController.setDirection(direction);
        model.setDirection(direction);
    }

    public void setMarket(Market market) {
        muSigCreateOfferAmountController.setMarket(market);
        muSigCreateOfferPriceController.setMarket(market);
        model.setMarket(market);
    }

    public void updateAmountSpecWithPriceSpec(PriceSpec priceSpec) {
        if(!muSigCreateOfferPriceController.getPriceSpec().isNotNull().get()) {
            muSigCreateOfferAmountController.updateBaseSideAmountSpecWithPriceSpec(priceSpec);
        }
    }

    public ReadOnlyObjectProperty<BaseSideAmountSpec> getBaseSideAmountSpec() {
        return muSigCreateOfferAmountController.getBaseSideAmountSpec();
    }

    public void setPaymentMethods(List<PaymentMethod<?>> paymentMethods) {
        muSigCreateOfferAmountController.setPaymentMethods(paymentMethods);
    }

    public boolean validate() {
        return muSigCreateOfferAmountController.validate()
                && muSigCreateOfferPriceController.validate();
    }

    public ReadOnlyObjectProperty<PriceSpec> getPriceSpec() {
        return muSigCreateOfferPriceController.getPriceSpec();
    }

    public void setInitialData(MuSigOffer muSigOffer) {
        setDirection(muSigOffer.getDirection());
        setMarket(muSigOffer.getMarket());
        muSigCreateOfferAmountController.setBaseSideAmountSpec((BaseSideAmountSpec) muSigOffer.getAmountSpec());
        muSigCreateOfferPriceController.setPriceSpec(muSigOffer.getPriceSpec());
    }

    private String getHeadline() {
        String quoteCurrencyCode = model.getMarket().getQuoteCurrencyCode();
        return model.getDirection().isBuy()
                ? Res.get("bisqEasy.tradeWizard.amountAtPrice.buy.headline", quoteCurrencyCode)
                : Res.get("bisqEasy.tradeWizard.amountAtPrice.sell.headline", quoteCurrencyCode);
    }
}
