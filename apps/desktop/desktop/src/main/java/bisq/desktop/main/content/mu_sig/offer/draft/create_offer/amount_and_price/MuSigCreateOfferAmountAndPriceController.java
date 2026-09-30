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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.MuSigCreateOfferAmountController;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.price.MuSigCreateOfferPriceController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.direction.DirectionSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class MuSigCreateOfferAmountAndPriceController implements Controller {
    private final MuSigCreateOfferAmountAndPriceModel model;
    @Getter
    private final MuSigCreateOfferAmountAndPriceView view;
    private final MuSigCreateOfferAmountController muSigCreateOfferAmountController;
    private final MuSigCreateOfferPriceController muSigCreateOfferPriceController;
    private final DirectionSelection directionSelection;
    private final MarketSelection marketSelection;

    public MuSigCreateOfferAmountAndPriceController(ServiceProvider serviceProvider,
                                                    CreateOfferUseCase createOfferUseCase,
                                                    Region owner,
                                                    Consumer<Boolean> navigationButtonsVisibleHandler,
                                                    Consumer<NavigationTarget> closeAndNavigateToHandler) {
        directionSelection = createOfferUseCase.getDirectionSelection();
        marketSelection = createOfferUseCase.getMarketSelection();
        muSigCreateOfferAmountController = new MuSigCreateOfferAmountController(createOfferUseCase,
                owner,
                navigationButtonsVisibleHandler,
                closeAndNavigateToHandler);
        muSigCreateOfferPriceController = new MuSigCreateOfferPriceController(serviceProvider,
                createOfferUseCase,
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


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void reset() {
        muSigCreateOfferPriceController.reset();
        model.reset();
    }


    public boolean validate() {
        return muSigCreateOfferPriceController.validate();
    }

    public ReadOnlyObjectProperty<PriceSpec> getPriceSpec() {
        return muSigCreateOfferPriceController.getPriceSpec();
    }

    private String getHeadline() {
        String baseCurrencyCode = marketSelection.getMarket().getBaseCurrencyCode();
        return directionSelection.getDisplayDirection().isBuy()
                ? Res.get("muSig.offer.wizard.amountAtPrice.buy.headline", baseCurrencyCode)
                : Res.get("muSig.offer.wizard.amountAtPrice.sell.headline", baseCurrencyCode);
    }
}
