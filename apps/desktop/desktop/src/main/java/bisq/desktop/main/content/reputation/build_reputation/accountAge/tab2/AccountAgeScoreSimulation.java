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

package bisq.desktop.main.content.reputation.build_reputation.accountAge.tab2;

import bisq.desktop.main.content.reputation.build_reputation.ScoreSimulation;
import bisq.user.reputation.AccountAgeService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

public class AccountAgeScoreSimulation extends ScoreSimulation {
    public AccountAgeScoreSimulation() {
        super();
    }

    @Override
    protected Controller createController() {
        return new AccountAgeScoreSimulation.Controller();
    }

    @Slf4j
    public static class Controller extends ScoreSimulation.Controller<Model, View> {
        private Controller() {
            super(0,
                    0,
                    (int) AccountAgeService.MAX_DAYS_AGE_SCORE);
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
        protected void calculateScore() {
            long score = AccountAgeService.doCalculateScore(model.getAge().get());
            applyScore(score);
        }
    }

    @Getter
    protected static class Model extends ScoreSimulation.Model {
        public Model(int defaultAge, int ageSliderMin, int ageSliderMax) {
            super(defaultAge, ageSliderMin, ageSliderMax);
        }
    }

    protected static class View extends ScoreSimulation.View<Model, Controller> {
        private View(Model model, Controller controller) {
            super(model, controller);
        }
    }
}
