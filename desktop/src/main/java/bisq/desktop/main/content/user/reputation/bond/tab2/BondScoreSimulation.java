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
import bisq.desktop.main.content.user.reputation.components.AgeSlider;
import bisq.i18n.Res;
import bisq.user.reputation.BondedReputationService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;

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
        private Subscription agePin, ageAsStringPin, scorePin;

        private Controller() {
            model = new Model();
            view = new View(model, this);

            model.getAmount().set("100");
            model.getLockTime().set("10000");
            model.getAgeAsInt().set(0);
            model.getAge().set("0");
        }

        @Override
        public void onActivate() {
            agePin = EasyBind.subscribe(model.getAgeAsInt(), age -> model.getAge().set(String.valueOf(age)));
            ageAsStringPin = EasyBind.subscribe(model.getAge(), ageAsString -> {
                try {
                    model.getAgeAsInt().set(Integer.parseInt(ageAsString));
                } catch (Exception e) {
                }
            });

            MonadicBinding<String> binding = EasyBind.combine(model.getAmount(), model.getLockTime(), model.getAgeAsInt(), this::calculateSimScore);
            scorePin = EasyBind.subscribe(binding, score -> model.getScore().set(score));
        }

        @Override
        public void onDeactivate() {
            agePin.unsubscribe();
            ageAsStringPin.unsubscribe();
            scorePin.unsubscribe();
        }

        private String calculateSimScore(String amount, String lockTime, Number age) {
            try {
                // amountAsLong is the smallest unit of BSQ (100 = 1 BSQ)
                long amountAsLong = MathUtils.roundDoubleToLong(Double.parseDouble(amount) * 100);
                long lockTimeAsLong = Long.parseLong(lockTime);
                long ageInDays = age.intValue();
                long totalScore = BondedReputationService.doCalculateScore(amountAsLong, lockTimeAsLong, ageInDays);
                return String.valueOf(totalScore);
            } catch (Exception e) {
                return "";
            }
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final IntegerProperty ageAsInt = new SimpleIntegerProperty();
        private final StringProperty lockTime = new SimpleStringProperty();
        private final StringProperty age = new SimpleStringProperty();
        private final StringProperty amount = new SimpleStringProperty();
        private final StringProperty score = new SimpleStringProperty();
    }

    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final MaterialTextField amount, score, lockTime, age;
        private final AgeSlider ageSlider;

        private View(Model model,
                     Controller controller) {
            super(new VBox(10), model, controller);

            Label simHeadline = new Label(Res.get("user.reputation.sim.headline"));
            simHeadline.getStyleClass().addAll("bisq-text-1");
            amount = getInputField("user.reputation.sim.burnAmount");
            int width = 195;
            amount.setMinWidth(width);
            amount.setMaxWidth(width);
            lockTime = getInputField("user.reputation.sim.lockTime");
            lockTime.setMinWidth(width);
            lockTime.setMaxWidth(width);
            age = getInputField("user.reputation.sim.age");
            ageSlider = new AgeSlider(0, 400, 0);
            score = getField(Res.get("user.reputation.sim.score"));
            VBox.setMargin(ageSlider.getView().getRoot(), new Insets(15, 0, 0, 0));
            root.getChildren().addAll(simHeadline,
                    new HBox(10, amount, lockTime),
                    age,
                    ageSlider.getView().getRoot(),
                    score);
        }

        @Override
        protected void onViewAttached() {
            ageSlider.valueProperty().bindBidirectional(model.getAgeAsInt());
            lockTime.textProperty().bindBidirectional(model.getLockTime());
            age.textProperty().bindBidirectional(model.getAge());
            amount.textProperty().bindBidirectional(model.getAmount());
            score.textProperty().bind(model.getScore());
        }

        @Override
        protected void onViewDetached() {
            ageSlider.valueProperty().unbindBidirectional(model.getAgeAsInt());
            lockTime.textProperty().unbindBidirectional(model.getLockTime());
            age.textProperty().unbindBidirectional(model.getAge());
            amount.textProperty().unbindBidirectional(model.getAmount());
            score.textProperty().unbind();
        }

        private MaterialTextField getField(String description) {
            MaterialTextField field = new MaterialTextField(description);
            field.setEditable(false);
            field.setMinWidth(400);
            field.setMaxWidth(400);
            return field;
        }

        private MaterialTextField getInputField(String key) {
            MaterialTextField field = new MaterialTextField(Res.get(key), Res.get(key + ".prompt"));
            field.setMinWidth(400);
            field.setMaxWidth(400);
            return field;
        }
    }
}