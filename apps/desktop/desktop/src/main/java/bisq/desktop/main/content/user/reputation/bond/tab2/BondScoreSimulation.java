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

package bisq.desktop.main.content.user.reputation.bond.tab2;

import bisq.common.util.MathUtils;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import bisq.user.reputation.BondedReputationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class BondScoreSimulation {

    private final Controller controller;

    public BondScoreSimulation() {
        controller = new Controller();
    }

    public VBox getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private Subscription scorePin;

        private Controller() {
            model = new Model();
            view = new View(model, this);

            model.getAmount().set("100");
        }

        @Override
        public void onActivate() {
            scorePin = EasyBind.subscribe(model.getAmount(), amount -> model.getScore().set(calculateSimScore(amount)));
        }

        @Override
        public void onDeactivate() {
            scorePin.unsubscribe();
        }

        private String calculateSimScore(String amount) {
            try {
                // amountAsLong is the smallest unit of BSQ (100 = 1 BSQ)
                long amountAsLong = Math.max(0, MathUtils.roundDoubleToLong(Double.parseDouble(amount) * 100));
                long totalScore = BondedReputationService.doCalculateScore(amountAsLong);
                return String.valueOf(totalScore);
            } catch (Exception e) {
                return "";
            }
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final StringProperty amount = new SimpleStringProperty();
        private final StringProperty score = new SimpleStringProperty();
    }

    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final MaterialTextField amount, score;

        private View(Model model,
                     Controller controller) {
            super(new VBox(10), model, controller);

            Label simHeadline = new Label(Res.get("user.reputation.sim.headline"));
            simHeadline.getStyleClass().addAll("bisq-text-1");
            amount = getInputField("user.reputation.sim.burnAmount");
            int width = 195;
            amount.setMinWidth(width);
            amount.setMaxWidth(width);
            score = getField(Res.get("user.reputation.sim.score"));
            root.getChildren().addAll(simHeadline,
                    amount,
                    score);
        }

        @Override
        protected void onViewAttached() {
            amount.textProperty().bindBidirectional(model.getAmount());
            score.textProperty().bind(model.getScore());
        }

        @Override
        protected void onViewDetached() {
            amount.textProperty().unbindBidirectional(model.getAmount());
            score.textProperty().unbind();
        }

        private MaterialTextField getField(String description) {
            MaterialTextField field = new MaterialTextField(description);
            field.setEditable(false);
            field.setMinWidth(380);
            field.setMaxWidth(380);
            return field;
        }

        private MaterialTextField getInputField(String key) {
            MaterialTextField field = new MaterialTextField(Res.get(key), Res.get(key + ".prompt"));
            field.setMinWidth(380);
            field.setMaxWidth(380);
            return field;
        }
    }
}