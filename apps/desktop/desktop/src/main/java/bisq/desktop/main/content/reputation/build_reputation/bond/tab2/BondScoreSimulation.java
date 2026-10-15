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

package bisq.desktop.main.content.reputation.build_reputation.bond.tab2;

import bisq.common.util.MathUtils;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.main.content.reputation.build_reputation.ScoreSimulation;
import bisq.presentation.parser.DoubleParser;
import bisq.user.reputation.BondedReputationService;
import bisq.user.reputation.ProofOfBurnService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.concurrent.TimeUnit;

public class BondScoreSimulation extends ScoreSimulation {
    public BondScoreSimulation() {
        super();
    }

    @Override
    protected Controller createController() {
        return new Controller();
    }

    @Slf4j
    public static class Controller extends ScoreSimulation.Controller<Model, View> {
        private Subscription amountPin;

        protected Controller() {
            super(0,
                    0,
                    ProofOfBurnService.MAX_AGE_BOOST_DAYS);

            model.getAmount().set("100");
        }

        @Override
        protected Model createModel(int defaultAge, int ageSliderMin, int ageSliderMax) {
            return new Model(defaultAge, ageSliderMin, ageSliderMax);
        }

        @Override
        protected View createView(Model model) {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            amountPin = EasyBind.subscribe(model.getAmount(), amount -> calculateScore());
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            amountPin.unsubscribe();
        }

        @Override
        protected void calculateScore() {
            long ageInDays = model.getAge().get();
            long age = TimeUnit.DAYS.toMillis(ageInDays);
            long blockTime = System.currentTimeMillis() - age;
            try {
                // amountAsLong is the smallest unit of BSQ (100 = 1 BSQ)
                long amountAsLong = Math.max(0, MathUtils.roundDoubleToLong(DoubleParser.parse(model.getAmount().get()) * 100));
                long score = BondedReputationService.doCalculateScore(amountAsLong, blockTime);
                applyScore(score);
            } catch (Exception e) {
                log.error("Failed to calculate score", e);
            }
        }
    }

    @Getter
    protected static class Model extends ScoreSimulation.Model {
        private final StringProperty amount = new SimpleStringProperty();

        public Model(int defaultAge, int ageSliderMin, int ageSliderMax) {
            super(defaultAge, ageSliderMin, ageSliderMax);
        }
    }

    protected static class View extends ScoreSimulation.View<Model, Controller> {
        private final MaterialTextField amount;

        private View(Model model, Controller controller) {
            super(model, controller);

            amount = getInputField("reputation.sim.bsqAmount");
            root.getChildren().add(1, amount);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            amount.textProperty().bindBidirectional(model.getAmount());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            amount.textProperty().unbindBidirectional(model.getAmount());
        }
    }
}
