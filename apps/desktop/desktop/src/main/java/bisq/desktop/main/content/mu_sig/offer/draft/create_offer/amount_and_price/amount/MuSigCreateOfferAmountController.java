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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount;

import bisq.common.monetary.TradeAmount;
import bisq.common.observable.Pin;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container.MuSigAmountContainerController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.amount.AmountSelection;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class MuSigCreateOfferAmountController implements Controller {
    private static final PriceSpec MARKET_PRICE_SPEC = new MarketPriceSpec();

    private final MuSigCreateOfferAmountModel model;
    @Getter
    private final MuSigCreateOfferAmountView view;
    private final Region owner;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final AmountSelection amountSelection;
    private final Set<Pin> pins = new HashSet<>();

    public MuSigCreateOfferAmountController(CreateOfferUseCase createOfferUseCase,
                                            Region owner,
                                            Consumer<Boolean> navigationButtonsVisibleHandler,
                                            Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.owner = owner;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        amountSelection = createOfferUseCase.getAmountSelection();
        model = new MuSigCreateOfferAmountModel();

        MuSigAmountContainerController muSigAmountComponentsController = new MuSigAmountContainerController(createOfferUseCase);
        view = new MuSigCreateOfferAmountView(model, this, muSigAmountComponentsController.getView().getRoot());
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        pins.add(amountSelection.useRangeAmountObservable().addObserver(useRangeAmount -> {
            UIThread.run(() -> {
                model.getUseRangeAmount().set(useRangeAmount);
            });
        }));

        pins.add(amountSelection.userSpecificTradeAmountLimitObservable().addObserver(value -> {
            if (value != null) {
                UIThread.run(() -> applyUserSpecificTradeAmountLimitInfo(value));
            }
        }));
        pins.add(amountSelection.useBaseCurrencyForAmountInputObservable().addObserver(value -> {
            Optional<TradeAmount> userSpecificTradeAmountLimit = amountSelection.getUserSpecificTradeAmountLimit();
            if (value != null && userSpecificTradeAmountLimit != null) {
                UIThread.run(() -> applyUserSpecificTradeAmountLimitInfo(userSpecificTradeAmountLimit));
            }
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


    /* --------------------------------------------------------------------- */
    // UI handlers
    /* --------------------------------------------------------------------- */

    void onSetUseRangeAmount(boolean value) {
        amountSelection.onSetUseRangeAmount(value);
    }

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

    void onLearnHowToBuildReputation() {
        closeAndNavigateToHandler.accept(NavigationTarget.BUILD_REPUTATION);
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }

    private void applyUserSpecificTradeAmountLimitInfo(Optional<TradeAmount> userSpecificTradeAmountLimit) {
        model.getShouldShowAmountLimitInfo().set(userSpecificTradeAmountLimit.isPresent());
        model.getAmountLimitInfo().set(userSpecificTradeAmountLimit
                .map(amountSelection::toInputAmount)
                .map(monetary -> AmountFormatter.formatAmountWithCode(monetary, true))
                .map(formatted -> Res.get("muSig.offer.create.amount.limitInfo.buyer", formatted))
                .orElse(""));
    }
}
