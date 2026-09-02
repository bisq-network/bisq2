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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container;

import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container.fix.MuSigFixAmountController;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container.limits.MuSigAmountLimitsController;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container.range.MuSigRangeAmountController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.amount.AmountSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigAmountContainerController implements Controller {
    private final MuSigAmountContainerModel model;
    @Getter
    private final MuSigAmountContainerView view;
    private final MuSigRangeAmountController muSigRangeAmountController;
    private final MuSigFixAmountController muSigFixAmountController;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();
    private final AmountSelection amountSelection;
    private final MarketSelection marketSelection;

    public MuSigAmountContainerController(CreateOfferUseCase createOfferUseCase) {
        marketSelection = createOfferUseCase.getMarketSelection();
        amountSelection = createOfferUseCase.getAmountSelection();
        model = new MuSigAmountContainerModel();

        muSigFixAmountController = new MuSigFixAmountController(createOfferUseCase);
        muSigRangeAmountController = new MuSigRangeAmountController(createOfferUseCase);
        MuSigAmountLimitsController amountLimitsController = new MuSigAmountLimitsController(createOfferUseCase);

        view = new MuSigAmountContainerView(model, this,
                muSigFixAmountController.getView().getRoot(),
                muSigRangeAmountController.getView().getRoot(),
                amountLimitsController.getView().getRoot()
        );
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        applyDescription();

        pins.add(amountSelection.useRangeAmountObservable().addObserver(useRangeAmount -> {
            UIThread.run(() -> {
                model.getUseRangeAmount().set(useRangeAmount);
                applyDescription();
            });
        }));


        pins.add(amountSelection.useBaseCurrencyForAmountInputObservable().addObserver(useBaseCurrencyForAmountInput -> {
            UIThread.run(this::applyDescription);
        }));

        subscriptions.add(EasyBind.subscribe(muSigFixAmountController.getIsTextInputFocused(),
                isTextInputFocused -> {
                    if (!amountSelection.getUseRangeAmount()) {
                        model.getIsTextInputFocused().set(isTextInputFocused);
                    }
                }));
        subscriptions.add(EasyBind.subscribe(muSigRangeAmountController.getIsTextInputFocused(),
                isTextInputFocused -> {
                    if (amountSelection.getUseRangeAmount()) {
                        model.getIsTextInputFocused().set(isTextInputFocused);
                    }
                }));
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pins.forEach(Pin::unbind);
        pins.clear();

        model.getIsTextInputFocused().unbind();
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void applyDescription() {
        Market market = marketSelection.getMarket();
        boolean useRangeAmount = amountSelection.getUseRangeAmount();
        String code = getCode(market);
        model.getDescription().set(useRangeAmount
                ? Res.get("muSig.offer.create.amount.description.range", code)
                : Res.get("muSig.offer.create.amount.description.fixed", code));
    }

    private String getCode(Market market) {
        boolean useBaseCurrencyForAmountInput = amountSelection.getUseBaseCurrencyForAmountInput();
        return useBaseCurrencyForAmountInput ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }
}
