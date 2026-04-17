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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price;

import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.MuSigCreateOfferAmountController;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.price.MuSigCreateOfferPriceController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class MuSigCreateOfferAmountAndPriceController implements Controller {
    private final MuSigCreateOfferAmountAndPriceModel model;
    @Getter
    private final MuSigCreateOfferAmountAndPriceView view;
    private final MuSigCreateOfferAmountController muSigCreateOfferAmountController;
    private final MuSigCreateOfferPriceController muSigCreateOfferPriceController;
    private final CreateOfferDraftWorkflow createOfferDraftWorkflow;
    private Subscription priceSpecPin;

    public MuSigCreateOfferAmountAndPriceController(ServiceProvider serviceProvider,
                                                    CreateOfferDraftWorkflow createOfferDraftWorkflow,
                                                    Region owner,
                                                    Consumer<Boolean> navigationButtonsVisibleHandler,
                                                    Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.createOfferDraftWorkflow = createOfferDraftWorkflow;
        muSigCreateOfferAmountController = new MuSigCreateOfferAmountController(serviceProvider,
                createOfferDraftWorkflow,
                owner,
                navigationButtonsVisibleHandler,
                closeAndNavigateToHandler);
        muSigCreateOfferPriceController = new MuSigCreateOfferPriceController(serviceProvider,
                createOfferDraftWorkflow,
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
        priceSpecPin = EasyBind.subscribe(muSigCreateOfferPriceController.getPriceSpec(),
                muSigCreateOfferAmountController::updateAmountSpecWithPriceSpec);

    }

    @Override
    public void onDeactivate() {
        priceSpecPin.unsubscribe();
        model.getIsAmountOverlayVisible().unbind();
        model.getIsPriceOverlayVisible().unbind();
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void reset() {
        muSigCreateOfferAmountController.reset();
        muSigCreateOfferPriceController.reset();
        model.reset();
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

    private String getHeadline() {
        String baseCurrencyCode = createOfferDraftWorkflow.getMarket().getBaseCurrencyCode();
        return createOfferDraftWorkflow.getDirection().isBuy()
                ? Res.get("muSig.offer.wizard.amountAtPrice.buy.headline", baseCurrencyCode)
                : Res.get("muSig.offer.wizard.amountAtPrice.sell.headline", baseCurrencyCode);
    }
}
