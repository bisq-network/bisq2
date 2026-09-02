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

package bisq.desktop.main.content.mu_sig.trade.trade_limits.simulation;

import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.monetary.Fiat;
import bisq.common.util.MathUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.PaymentMethodBasedAmountLimitsProvider;
import bisq.presentation.formatters.AmountFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class MuSigTradeLimitsSimulationController implements Controller {
    @Getter
    private final MuSigTradeLimitsSimulationView view;
    private final MuSigTradeLimitsSimulationModel model;
    private final Set<Subscription> pins = new HashSet<>();

    public MuSigTradeLimitsSimulationController(ServiceProvider serviceProvider) {
        List<FiatPaymentRail> fiatPaymentRails = Arrays.stream(FiatPaymentRail.values())
                .filter(e -> e != FiatPaymentRail.CUSTOM)
                .sorted(Comparator.comparing(PaymentRail::getShortDisplayString))
                .toList();
        model = new MuSigTradeLimitsSimulationModel(
                15,
                90,
                fiatPaymentRails);
        view = new MuSigTradeLimitsSimulationView(model, this);

        applySelectFiatPaymentRail(FiatPaymentRail.SEPA);
    }

    @Override
    public void onActivate() {
        pins.add(EasyBind.subscribe(model.getAccountAge(), value -> updateLimits()));
    }

    @Override
    public void onDeactivate() {
        pins.forEach(Subscription::unsubscribe);
        pins.clear();
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.MU_SIG_TRADE_LIMITS_OVERVIEW);
    }

    void onClose() {
        OverlayController.hide();
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/MuSig-trade-limits");
    }

    void onHasBisq1AccountAgeWitnessChanged(boolean selected) {
        model.getHasBisq1AccountAgeWitness().set(selected);
        updateLimits();
    }

    void onSelectFiatPaymentRail(FiatPaymentRail selectedItem) {
        if (selectedItem != null) {
            applySelectFiatPaymentRail(selectedItem);
        }
    }

    private void applySelectFiatPaymentRail(FiatPaymentRail paymentRail) {
        model.getSelectedFiatPaymentRail().set(paymentRail);
        
        Fiat maxTradeLimitInUsd = PaymentMethodBasedAmountLimitsProvider.evaluateLimitInUsd(paymentRail);
        String maxTradeLimit = AmountFormatter.formatQuoteAmount(maxTradeLimitInUsd);
        model.getFiatPaymentRailMaxLimit().set(Res.get("muSig.trade.limits.simulation.fiatRail.maxLimit", maxTradeLimit));
        updateLimits();
    }

    private void updateLimits() {
        FiatPaymentRail fiatPaymentRail = model.getSelectedFiatPaymentRail().get();
        if (fiatPaymentRail == null) {
            return;
        }

        double maxTradeLimit = PaymentMethodBasedAmountLimitsProvider.evaluateLimitInUsd(fiatPaymentRail).getValue() / 10000d;
        // We use 10% of max limit as default limit
        double defaultTradeLimit = maxTradeLimit * 0.1;    // 250-1000

        // AccountAgeWitnessScore
        double accountAgeWitnessScoreTradeLimitBoost = model.getHasBisq1AccountAgeWitness().get()
                ? defaultTradeLimit * 9
                : 0;

        // AccountAge
        double minAccountAge = model.getMinAccountAge();
        double maxAccountAge = model.getMaxAccountAge();
        double accountAge = MathUtils.roundDouble(model.getAccountAge().get(), 9);
        double accountAgeWeight = normalize(accountAge, minAccountAge, maxAccountAge);
        double tradeLimitBoostFromAccountAge = defaultTradeLimit * accountAgeWeight * 9;

        double tradeLimit = defaultTradeLimit +
                tradeLimitBoostFromAccountAge +
                accountAgeWitnessScoreTradeLimitBoost;
        tradeLimit = Math.min(maxTradeLimit, tradeLimit);

        // Rate is trade limit / 1000
        double derivedFromTradeLimit = MathUtils.roundDouble(tradeLimit / 1000, 0, RoundingMode.FLOOR);
        double rateLimit = Math.max(1, derivedFromTradeLimit);

        model.getTradeLimit().set(formatTradeLimit(tradeLimit));
        model.getRateLimit().set(formatRateLimit(rateLimit));
    }

    private static double normalize(double value, double minValue, double maxValue) {
        double range = maxValue - minValue;
        double offset = value - minValue;
        return range > 0 ? MathUtils.bounded(0, 1, offset / range) : 0;
    }

    private static String formatTradeLimit(double tradeLimit) {
        return MathUtils.roundDoubleToInt(tradeLimit) + " USD";
    }

    private static String formatRateLimit(Double rateLimit) {
        int rateLimitRounded = MathUtils.roundDoubleToInt(rateLimit);
        if (rateLimitRounded >= 5) {
            return Res.get("muSig.trade.limits.simulation.rateLimit.noLimit");
        } else if (rateLimitRounded == 1) {
            return Res.get("muSig.trade.limits.simulation.rateLimit.singular", rateLimitRounded);
        } else {
            return Res.get("muSig.trade.limits.simulation.rateLimit.plural", rateLimitRounded);
        }
    }
}
