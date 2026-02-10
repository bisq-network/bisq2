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

package bisq.desktop.main.content.mu_sig.trade_limits.tab2;

import bisq.account.payment_method.fiat.FiatPaymentMethodChargebackRisk;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.MathUtils;
import bisq.desktop.common.converters.LongStringConverter;
import bisq.desktop.common.converters.PercentageStringConverter;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import bisq.presentation.formatters.PercentageFormatter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

public class TradeLimitsPreview {

    private final Controller controller;

    public TradeLimitsPreview() {
        controller = new Controller();
    }

    public GridPane getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final Set<Subscription> pins = new HashSet<>();

        private Controller() {
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            pins.add(EasyBind.subscribe(model.getDeposit(), value -> handleChange()));
            pins.add(EasyBind.subscribe(model.getAccountAge(), value -> handleChange()));
            pins.add(EasyBind.subscribe(model.getReputationScore(), value -> handleChange()));
            pins.add(EasyBind.subscribe(model.getAccountAgeWitnessScore(), value -> handleChange()));
            pins.add(EasyBind.subscribe(model.getDualAccountVerificationAge(), value -> handleChange()));
            pins.add(EasyBind.subscribe(model.getSignedFiatReceiptAge(), value -> handleChange()));
        }

        private final DoubleProperty reputation = new SimpleDoubleProperty();
        private final DoubleProperty accountAge = new SimpleDoubleProperty();
        private final BooleanProperty hadDualBankVerification = new SimpleBooleanProperty();
        private final StringProperty tradeLimit = new SimpleStringProperty();

        @Override
        public void onDeactivate() {
            pins.forEach(Subscription::unsubscribe);
            pins.clear();
        }

        private void handleChange() {
            FiatPaymentRail fiatPaymentRail = model.getFiatPaymentRail().get();
            FiatPaymentMethodChargebackRisk chargebackRisk = fiatPaymentRail.getChargebackRisk();

            double maxTradeLimit = 10000; // USD
            double maxTradeLimitByChargebackRisk = getMaxTradeLimitByChargebackRisk(chargebackRisk, maxTradeLimit); // 2500-10000
            double defaultTradeLimit = maxTradeLimitByChargebackRisk * 0.1;    // 250-1000
            double tradeLimit;

            double defaultRateLimit = getMaxRateLimitByChargebackRisk(chargebackRisk);  // 1 - unlimited
            double rateLimit;

            // Deposit
            double minDeposit = 0.25;
            double maxDeposit = 1;
            double deposit = model.getDeposit().get();
            double depositFactor = normalize(deposit, minDeposit, maxDeposit); // 0-1
            double depositTradeLimitBoost = defaultTradeLimit * depositFactor; // 0-500
            double depositRateLimitBoost = defaultRateLimit * depositFactor; // 0-1

            // AccountAge
            double minAccountAge = 15;
            double maxAccountAge = 60;
            double accountAge = MathUtils.roundDouble(model.getAccountAge().get(), 9);
            double normalizedAccountAge = normalize(accountAge, minAccountAge, maxAccountAge);
            double accountAgeFactor = accountAge >= minAccountAge ? 0.25 + normalizedAccountAge * 0.75 : 0;
            double accountAgeTradeLimitBoost = defaultTradeLimit * accountAgeFactor * 2; // 250-1000
            double accountAgeRateLimitBoost = defaultRateLimit * accountAgeFactor * 3; // 0-3

            // ReputationScore
            double minReputationScore = 0;
            double maxReputationScore = 100000; // 1000 BSQ -> 2000 USD
            double reputationScore = model.getReputationScore().get();
            double reputationScoreFactor = normalize(reputationScore, minReputationScore, maxReputationScore); // 0-1
            double reputationScoreTradeLimitBoost = defaultTradeLimit * reputationScoreFactor * 3; // 0-1500
            double reputationScoreRateLimitBoost = defaultRateLimit * reputationScoreFactor * 3; // 0-2

            // AccountAgeWitnessScore
            double minAccountAgeWitnessScore = 61; // 61 days
            double maxAccountAgeWitnessScore = 180; // 180 days
            double accountAgeWitnessScore = model.getAccountAgeWitnessScore().get();
            double normalizedAccountAgeWitnessScore = normalize(accountAgeWitnessScore, minAccountAgeWitnessScore, maxAccountAgeWitnessScore);
            double accountAgeWitnessScoreFactor = accountAgeWitnessScore >= minAccountAgeWitnessScore ? 0.5 + normalizedAccountAgeWitnessScore * 0.5 : 0;
            double accountAgeWitnessScoreTradeLimitBoost = defaultTradeLimit * accountAgeWitnessScoreFactor * 2; // 500-1000
            double accountAgeWitnessScoreRateLimitBoost = defaultRateLimit * accountAgeWitnessScoreFactor * 3; // 0-3

            // dualAccountVerificationAge
            double minDualAccountVerificationAge = 0;
            double maxDualAccountVerificationAge = 30;
            double dualAccountVerificationAge = MathUtils.roundDouble(model.getDualAccountVerificationAge().get(), 0);
            double normalizedDualAccountVerificationAge = normalize(dualAccountVerificationAge, minDualAccountVerificationAge, maxDualAccountVerificationAge);
            double dualAccountVerificationAgeFactor = dualAccountVerificationAge >= 1 ? 0.5 + normalizedDualAccountVerificationAge * 0.5 : 0; // 0 | 0.5 - 1
            double dualAccountVerificationAgeTradeLimitBoost = defaultTradeLimit * dualAccountVerificationAgeFactor * 18; // 0 | 5500
            double dualAccountVerificationAgeRateLimitBoost = defaultRateLimit * dualAccountVerificationAgeFactor * 9; // 0 | 5 - 10

            // signedFiatReceiptAge
            double minSignedFiatReceiptAge = 15;
            double maxSignedFiatReceiptAge = 60;
            double signedFiatReceiptAge = MathUtils.roundDouble(model.getSignedFiatReceiptAge().get(), 0);
            double normalizedSignedFiatReceiptAge = normalize(signedFiatReceiptAge, minSignedFiatReceiptAge, maxSignedFiatReceiptAge);
            double signedFiatReceiptAgeFactor = signedFiatReceiptAge >= 15 ? 0.25 + normalizedSignedFiatReceiptAge * 0.75 : 0; // 0 | 0.25 - 1
            double signedFiatReceiptAgeTradeLimitBoost = defaultTradeLimit * signedFiatReceiptAgeFactor * 9; // 0 | 1250 - 5000
            double signedFiatReceiptAgeRateLimitBoost = defaultRateLimit * signedFiatReceiptAgeFactor * 9; // 0-10


            tradeLimit = defaultTradeLimit +   // 500
                    depositTradeLimitBoost +  // 0 - 500; max accumulated: 1000
                    accountAgeTradeLimitBoost + // 0 - 1000; max accumulated: 2000
                    reputationScoreTradeLimitBoost + //0 - 2000; max accumulated: 4000
                    accountAgeWitnessScoreTradeLimitBoost + //0 - 1000; max accumulated: 5000
                    dualAccountVerificationAgeTradeLimitBoost + //0 - 5000; max accumulated: 5000 capped
                    signedFiatReceiptAgeTradeLimitBoost; //0 - 5000; max accumulated: 5000 capped
            tradeLimit = Math.min(maxTradeLimitByChargebackRisk, tradeLimit);
          /*  log.error("");
            log.error("########################");
            log.error("defaultTradeLimit={}", defaultTradeLimit);
            log.error("depositTradeLimitBoost={}", depositTradeLimitBoost);
            log.error("accountAgeTradeLimitBoost={}", accountAgeTradeLimitBoost);
            log.error("reputationScoreTradeLimitBoost={}", reputationScoreTradeLimitBoost);
            log.error("accountAgeWitnessScoreTradeLimitBoost={}", accountAgeWitnessScoreTradeLimitBoost);
            log.error("dualAccountVerificationAgeTradeLimitBoost={}", dualAccountVerificationAgeTradeLimitBoost);
            log.error("signedFiatReceiptAgeTradeLimitBoost={}", signedFiatReceiptAgeTradeLimitBoost);
            log.error("tradeLimit={}", tradeLimit);*/


            rateLimit = defaultRateLimit + // 1
                    depositRateLimitBoost + // 0 - 1; max accumulated: 2
                    accountAgeRateLimitBoost + // 0 - 1; max accumulated: 3
                    reputationScoreRateLimitBoost + // 0 - 1; max accumulated: 4
                    accountAgeWitnessScoreRateLimitBoost + // 0 - 1; max accumulated: 5
                    dualAccountVerificationAgeRateLimitBoost + // 0 - 10; max accumulated: 20
                    signedFiatReceiptAgeRateLimitBoost; // 0 - 10; max accumulated: 30
           /* log.error("########################");
            log.error("defaultRateLimit={}", defaultRateLimit);
            log.error("depositRateLimitBoost={}", depositRateLimitBoost);
            log.error("accountAgeRateLimitBoost={}", accountAgeRateLimitBoost);
            log.error("reputationScoreRateLimitBoost={}", reputationScoreRateLimitBoost);
            log.error("accountAgeWitnessScoreRateLimitBoost={}", accountAgeWitnessScoreRateLimitBoost);
            log.error("dualAccountVerificationAgeRateLimitBoost={}", dualAccountVerificationAgeRateLimitBoost);
            log.error("signedFiatReceiptAgeRateLimitBoost={}", signedFiatReceiptAgeRateLimitBoost);
            log.error("rateLimit={}", rateLimit);
            log.error("");*/

            model.getTradeLimit().set(formatTradeLimit(tradeLimit));
            model.getRateLimit().set(formatRateLimit(rateLimit));
        }

        private double normalize(double value, double minValue, double maxValue) {
            double range = maxValue - minValue;
            double offset = value - minValue;
            return range > 0 ? MathUtils.bounded(0, 1, offset / range) : 0;
        }


        private static String formatTradeLimit(double tradeLimit) {
            return MathUtils.roundDoubleToInt(tradeLimit) + " USD";
        }

        private static String formatRateLimit(Double rateLimit) {
            int rateLimitRounded = MathUtils.roundDoubleToInt(rateLimit);
            if (rateLimitRounded >= 10) {
                return "No rate limit";
            } else if (rateLimitRounded == 1) {
                return rateLimitRounded + " trade per day";
            } else {
                return rateLimitRounded + " trades per day";
            }
        }


        private double getMaxTradeLimitByChargebackRisk(FiatPaymentMethodChargebackRisk chargebackRisk,
                                                        double maxTradeLimit) {
            switch (chargebackRisk) {
                case VERY_LOW -> {
                    return maxTradeLimit;
                }
                case LOW -> {
                    return maxTradeLimit * 0.8;
                }
                case MEDIUM -> {
                    return maxTradeLimit * 0.65;
                }
                case MODERATE -> {
                    return maxTradeLimit * 0.5;
                }
                default -> {
                    return 0;
                }
            }
        }

        private double getMaxRateLimitByChargebackRisk(FiatPaymentMethodChargebackRisk chargebackRisk) {
            switch (chargebackRisk) {
                case VERY_LOW -> {
                    // We multiply and accumulate later, thus we reduce the max value to avoid overflow
                    return Double.MAX_VALUE / 100;
                }
                case LOW -> {
                    return 4;
                }
                case MEDIUM -> {
                    return 2;
                }
                case MODERATE -> {
                    return 1;
                }
                default -> {
                    return 0;
                }
            }
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<FiatPaymentRail> fiatPaymentRail = new SimpleObjectProperty<>(FiatPaymentRail.ZELLE);
        private final ObjectProperty<FiatPaymentMethodChargebackRisk> chargebackRisk = new SimpleObjectProperty<>(FiatPaymentMethodChargebackRisk.MODERATE);
        private final DoubleProperty deposit = new SimpleDoubleProperty();
        private final DoubleProperty reputationScore = new SimpleDoubleProperty();
        private final DoubleProperty accountAgeWitnessScore = new SimpleDoubleProperty();
        private final DoubleProperty accountAge = new SimpleDoubleProperty();
        private final DoubleProperty signedFiatReceiptAge = new SimpleDoubleProperty();
        private final DoubleProperty dualAccountVerificationAge = new SimpleDoubleProperty();
        private final StringProperty tradeLimit = new SimpleStringProperty();
        private final StringProperty rateLimit = new SimpleStringProperty();
    }

    private static class View extends bisq.desktop.common.view.View<GridPane, Model, Controller> {

        private final MaterialTextField tradeLimit, rateLimit;
        private final SliderWithValue deposit, accountAge,
                reputationScore, accountAgeWitnessScore,
                signedFiatReceiptAge, dualAccountVerificationAge;

        private View(Model model, Controller controller) {
            super(new GridPane(15, 15), model, controller);

            GridPaneUtil.setGridPaneTwoColumnsConstraints(root);

            int rowIndex = 0;

            deposit = new SliderWithValue(0.25, 0.25, 1,
                    "muSig.tradeLimits.tab2.preview.deposit",
                    e -> PercentageFormatter.formatToPercentWithSymbol(e, 0),
                    new PercentageStringConverter(0.3, 2),
                    0.01);
            accountAge = new SliderWithValue(0, 0, 60,
                    "muSig.tradeLimits.tab2.preview.accountAge",
                    value -> String.valueOf(MathUtils.roundDouble(value, 0)),
                    new LongStringConverter(0),
                    1);
            GridPane.setHgrow(deposit.getViewRoot(), Priority.ALWAYS);
            GridPane.setHgrow(accountAge.getViewRoot(), Priority.ALWAYS);
            root.add(deposit.getViewRoot(), 0, rowIndex, 1, 1);
            root.add(accountAge.getViewRoot(), 1, rowIndex, 1, 1);

            accountAgeWitnessScore = new SliderWithValue(0, 0, 180,
                    "muSig.tradeLimits.tab2.preview.accountAgeWitnessScore",
                    value -> String.valueOf(MathUtils.roundDouble(value, 0)),
                    new LongStringConverter(0),
                    1);
            reputationScore = new SliderWithValue(0, 0, 100000,
                    "muSig.tradeLimits.tab2.preview.reputationScore",
                    value -> String.valueOf(MathUtils.roundDouble(value, 0)),
                    new LongStringConverter(0),
                    1);
            GridPane.setHgrow(accountAgeWitnessScore.getViewRoot(), Priority.ALWAYS);
            GridPane.setHgrow(reputationScore.getViewRoot(), Priority.ALWAYS);
            root.add(accountAgeWitnessScore.getViewRoot(), 0, ++rowIndex, 1, 1);
            root.add(reputationScore.getViewRoot(), 1, rowIndex, 1, 1);

            signedFiatReceiptAge = new SliderWithValue(0, 0, 60,
                    "muSig.tradeLimits.tab2.preview.signedFiatReceiptAge",
                    value -> String.valueOf(MathUtils.roundDouble(value, 0)),
                    new LongStringConverter(0),
                    1);

            dualAccountVerificationAge = new SliderWithValue(0, 0, 30,
                    "muSig.tradeLimits.tab2.preview.dualAccountVerificationAge",
                    value -> String.valueOf(MathUtils.roundDouble(value, 0)),
                    new LongStringConverter(0),
                    1);
            GridPane.setHgrow(signedFiatReceiptAge.getViewRoot(), Priority.ALWAYS);
            GridPane.setHgrow(dualAccountVerificationAge.getViewRoot(), Priority.ALWAYS);
            root.add(signedFiatReceiptAge.getViewRoot(), 0, ++rowIndex, 1, 1);
            root.add(dualAccountVerificationAge.getViewRoot(), 1, rowIndex, 1, 1);


            tradeLimit = new MaterialTextField(Res.get("muSig.tradeLimits.tab2.preview.tradeLimit"));
            tradeLimit.setEditable(false);
            rateLimit = new MaterialTextField(Res.get("muSig.tradeLimits.tab2.preview.rateLimit"));
            rateLimit.setEditable(false);

            root.add(tradeLimit, 0, ++rowIndex, 1, 1);
            root.add(rateLimit, 1, rowIndex, 1, 1);
        }

        @Override
        protected void onViewAttached() {
            model.getDeposit().bind(deposit.valueProperty());
            model.getAccountAge().bind(accountAge.valueProperty());
            model.getReputationScore().bind(reputationScore.valueProperty());
            model.getAccountAgeWitnessScore().bind(accountAgeWitnessScore.valueProperty());
            model.getSignedFiatReceiptAge().bind(signedFiatReceiptAge.valueProperty());
            model.getDualAccountVerificationAge().bind(dualAccountVerificationAge.valueProperty());


            tradeLimit.textProperty().bind(model.getTradeLimit());
            rateLimit.textProperty().bind(model.getRateLimit());
        }

        @Override
        protected void onViewDetached() {
            model.getDeposit().unbind();
            model.getAccountAge().unbind();
            model.getReputationScore().unbind();
            model.getAccountAgeWitnessScore().unbind();
            model.getSignedFiatReceiptAge().unbind();
            model.getDualAccountVerificationAge().unbind();

            tradeLimit.textProperty().unbind();
            rateLimit.textProperty().unbind();
        }
    }
}
