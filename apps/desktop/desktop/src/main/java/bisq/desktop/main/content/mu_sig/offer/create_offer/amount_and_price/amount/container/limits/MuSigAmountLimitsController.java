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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.limits;

import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

import static bisq.mu_sig.MuSigTradeAmountLimits.MAX_TRADE_AMOUNT_IN_USD;
import static bisq.mu_sig.MuSigTradeAmountLimits.MIN_TRADE_AMOUNT_IN_USD;
import static bisq.presentation.formatters.AmountFormatter.formatAmountByMonetaryType;

@Slf4j
public class MuSigAmountLimitsController implements Controller {
    private final MuSigAmountLimitsModel model;
    @Getter
    private final MuSigAmountLimitsView view;
    private final Set<Pin> pins = new HashSet<>();
    private final CreateOfferDraftWorkflow createOfferDraftWorkflow;

    public MuSigAmountLimitsController(ServiceProvider serviceProvider,
                                       CreateOfferDraftWorkflow createOfferDraftWorkflow) {
        this.createOfferDraftWorkflow = createOfferDraftWorkflow;

        String minInUsd = Res.get("muSig.offer.create.amount.slider.limit.usd", formatAmountByMonetaryType(MIN_TRADE_AMOUNT_IN_USD));
        String maxInInUsd = Res.get("muSig.offer.create.amount.slider.limit.usd", formatAmountByMonetaryType(MAX_TRADE_AMOUNT_IN_USD));
        model = new MuSigAmountLimitsModel(minInUsd, maxInInUsd);
        view = new MuSigAmountLimitsView(model, this);
    }

    @Override
    public void onActivate() {
        pins.add(createOfferDraftWorkflow.inputAmountLimitsObservable().addObserver(inputAmountLimits -> {
            UIThread.run(() -> {
                model.getMin().set(formatAmountByMonetaryType(inputAmountLimits.getMin()));
                model.getMax().set(formatAmountByMonetaryType(inputAmountLimits.getMax()));
                model.getCode().set(inputAmountLimits.getMax().getCode());
            });
        }));
    }

    @Override
    public void onDeactivate() {
        pins.forEach(Pin::unbind);
        pins.clear();
    }
}
