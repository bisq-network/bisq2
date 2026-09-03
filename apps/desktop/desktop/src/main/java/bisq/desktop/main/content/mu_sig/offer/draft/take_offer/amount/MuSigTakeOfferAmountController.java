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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount;

import bisq.common.monetary.TradeAmount;
import bisq.common.observable.Pin;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount.container.MuSigAmountContainerController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferUseCase;
import bisq.offer.mu_sig.use_case.take_offer.amount.TakeOfferAmountService;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class MuSigTakeOfferAmountController implements Controller {
    private static final PriceSpec MARKET_PRICE_SPEC = new MarketPriceSpec();

    private final MuSigTakeOfferAmountModel model;
    @Getter
    private final MuSigTakeOfferAmountView view;
    private final TakeOfferUseCase takeOfferService;
    private final TakeOfferAmountService takeOfferAmountService;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final Set<Pin> pins = new HashSet<>();

    public MuSigTakeOfferAmountController(TakeOfferUseCase takeOfferService,
                                          Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.takeOfferService = takeOfferService;
        takeOfferAmountService = takeOfferService.getAmountService();
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new MuSigTakeOfferAmountModel();

        MuSigAmountContainerController muSigAmountComponentsController = new MuSigAmountContainerController(takeOfferService);
        view = new MuSigTakeOfferAmountView(model, this, muSigAmountComponentsController.getView().getRoot());
    }

    public void init(MuSigOffer muSigOffer) {
        Direction takersDisplayDirection = muSigOffer.getTakersDisplayDirection();
        model.setHeadline(takersDisplayDirection.isBuy()
                ? Res.get("muSig.offer.taker.amount.headline.buyer")
                : Res.get("muSig.offer.taker.amount.headline.seller"));
    }

    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        pins.add(takeOfferAmountService.userSpecificTradeAmountLimitObservable().addObserver(value -> {
            UIThread.run(this::applyAmountLimitInfo);
        }));
        pins.add(takeOfferAmountService.useBaseCurrencyForAmountInputObservable().addObserver(value -> {
            UIThread.run(this::applyAmountLimitInfo);
        }));
    }

    @Override
    public void onDeactivate() {
        pins.forEach(Pin::unbind);
        pins.clear();
        navigationButtonsVisibleHandler.accept(true);
        model.getIsOverlayVisible().set(false);
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public ReadOnlyBooleanProperty getIsOverlayVisible() {
        return model.getIsOverlayVisible();
    }

    public void reset() {
        model.reset();
    }


    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseOverlay);
    }

    void onShowOverlay() {
        if (!model.getIsOverlayVisible().get()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getIsOverlayVisible().set(true);
        }
    }

    void onCloseOverlay() {
        if (model.getIsOverlayVisible().get()) {
            navigationButtonsVisibleHandler.accept(true);
            model.getIsOverlayVisible().set(false);
        }
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }

    private void applyAmountLimitInfo() {
        Optional<TradeAmount> userSpecificTradeAmountLimit = takeOfferAmountService.getUserSpecificTradeAmountLimit();
        model.getShouldShowAmountLimitInfo().set(userSpecificTradeAmountLimit.isPresent());
        model.getAmountLimitInfo().set(userSpecificTradeAmountLimit
                .map(takeOfferService::toInputAmount)
                .map(monetary -> AmountFormatter.formatAmountWithCode(monetary, true))
                .map(formatted -> Res.get("muSig.offer.create.amount.limitInfo.buyer", formatted))
                .orElse(""));
    }
}
