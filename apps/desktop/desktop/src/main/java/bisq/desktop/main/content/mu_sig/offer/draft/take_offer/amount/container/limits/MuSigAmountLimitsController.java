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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount.container.limits;

import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferUseCase;
import bisq.offer.mu_sig.use_case.take_offer.amount.TakeOfferAmountService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

import static bisq.presentation.formatters.AmountFormatter.formatAmountByMonetaryType;

@Slf4j
public class MuSigAmountLimitsController implements Controller {
    private final MuSigAmountLimitsModel model;
    @Getter
    private final MuSigAmountLimitsView view;
    private final TakeOfferAmountService takeOfferAmountService;
    private final Set<Pin> pins = new HashSet<>();

    public MuSigAmountLimitsController(TakeOfferUseCase takeOfferService) {
        takeOfferAmountService = takeOfferService.getAmountService();

        model = new MuSigAmountLimitsModel();
        view = new MuSigAmountLimitsView(model, this);
    }

    @Override
    public void onActivate() {
        pins.add(takeOfferAmountService.inputAmountLimitsInUsdObservable().addObserver(limitsInUsd -> {
            // Disposal clears the limits; the null fire clears the tooltips with them.
            UIThread.run(() -> applyLimitsInUsd(limitsInUsd));
        }));
        pins.add(takeOfferAmountService.inputAmountLimitsObservable().addObserver(inputAmountLimits -> {
            // Disposal clears the limits; the null fire carries nothing to display.
            if (inputAmountLimits == null) {
                return;
            }
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

    private void applyLimitsInUsd(MonetaryRange limitsInUsd) {
        model.getMinInUsd().set(limitsInUsd == null ? "" : toUsdEquivalent(limitsInUsd.getMin()));
        model.getMaxInUsd().set(limitsInUsd == null ? "" : toUsdEquivalent(limitsInUsd.getMax()));
    }

    private static String toUsdEquivalent(Monetary amountInUsd) {
        return Res.get("muSig.offer.create.amount.slider.limit.usd", formatAmountByMonetaryType(amountInUsd));
    }
}
