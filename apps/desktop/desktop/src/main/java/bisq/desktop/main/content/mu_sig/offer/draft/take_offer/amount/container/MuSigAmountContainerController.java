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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount.container;

import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount.container.fix.MuSigFixAmountController;
import bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount.container.limits.MuSigAmountLimitsController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferUseCase;
import bisq.offer.mu_sig.use_case.take_offer.amount.TakeOfferAmountService;
import bisq.offer.mu_sig.use_case.take_offer.market.TakeOfferMarketService;
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
    private final MuSigFixAmountController muSigFixAmountController;
    private final TakeOfferUseCase takeOfferService;
    private final TakeOfferMarketService takeOfferMarketService;
    private final TakeOfferAmountService takeOfferAmountService;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();

    public MuSigAmountContainerController(TakeOfferUseCase takeOfferService) {
        this.takeOfferService = takeOfferService;
        takeOfferMarketService = takeOfferService.getMarketService();
        takeOfferAmountService = takeOfferService.getAmountService();
        model = new MuSigAmountContainerModel();

        muSigFixAmountController = new MuSigFixAmountController(takeOfferService);
        MuSigAmountLimitsController amountLimitsController = new MuSigAmountLimitsController(takeOfferService);

        view = new MuSigAmountContainerView(model, this,
                muSigFixAmountController.getView().getRoot(),
                amountLimitsController.getView().getRoot()
        );
    }


    /* --------------------------------------------------------------------- */
    // Lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
        applyDescription();

        pins.add(takeOfferAmountService.useBaseCurrencyForAmountInputObservable().addObserver(useBaseCurrencyForAmountInput -> {
            UIThread.run(this::applyDescription);
        }));

        subscriptions.add(EasyBind.subscribe(muSigFixAmountController.getIsTextInputFocused(),
                isTextInputFocused -> {
                    model.getIsTextInputFocused().set(isTextInputFocused);
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
        // The observer queues this through UIThread.run; an already queued call can run after
        // disposal cleared the domain, where no market is present.
        takeOfferMarketService.findMarket().ifPresent(market ->
                model.getDescription().set(Res.get("muSig.offer.create.amount.description.fixed", getCode(market))));
    }

    private String getCode(Market market) {
        boolean useBaseCurrencyForAmountInput = takeOfferAmountService.getUseBaseCurrencyForAmountInput();
        return useBaseCurrencyForAmountInput ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode();
    }
}
