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
import bisq.desktop.main.content.reputation.build_reputation.components.AgeSlider;
import bisq.i18n.Res;
import bisq.presentation.parser.DoubleParser;
import bisq.user.reputation.BondedReputationService;
import bisq.user.reputation.ProofOfBurnService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.concurrent.TimeUnit;

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
        private Subscription agePin, ageAsStringPin, amountPin;

        private Controller() {
            model = new Model();
            view = new View(model, this);

            model.getAmount().set("100");
            model.getAge().set(0);
            model.getAgeAsString().set("0");
        }

        @Override
        public void onActivate() {
            agePin = EasyBind.subscribe(model.getAge(), age -> model.getAgeAsString().set(String.valueOf(age)));
            ageAsStringPin = EasyBind.subscribe(model.getAgeAsString(), ageAsString -> {
                try {
                    model.getAge().set(Integer.parseInt(ageAsString));
                    calculateSimScore();
                } catch (Exception e) {
                }
            });
            amountPin = EasyBind.subscribe(model.getAmount(), amount -> calculateSimScore());
        }

        @Override
        public void onDeactivate() {
            agePin.unsubscribe();
            ageAsStringPin.unsubscribe();
            amountPin.unsubscribe();
        }

        private void calculateSimScore() {
            try {
                // amountAsLong is the smallest unit of BSQ (100 = 1 BSQ)
                long amountAsLong = Math.max(0, MathUtils.roundDoubleToLong(DoubleParser.parse(model.getAmount().get()) * 100));
                long ageInDays = Math.max(0, model.getAge().get());
                long age = TimeUnit.DAYS.toMillis(ageInDays);
                long blockTime = System.currentTimeMillis() - age;
                long totalScore = BondedReputationService.doCalculateScore(amountAsLong, blockTime);
                String score = String.valueOf(totalScore);
                model.getScore().set(score);
            } catch (Exception e) {
                log.error("Failed to calculate simScore", e);
            }
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final StringProperty amount = new SimpleStringProperty();
        private final IntegerProperty age = new SimpleIntegerProperty();
        private final StringProperty ageAsString = new SimpleStringProperty();
        private final StringProperty score = new SimpleStringProperty();
    }

    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private static final double MATERIAL_FIELD_WIDTH = 270;

        private final MaterialTextField amount;
        private final MaterialTextField score;
        private final AgeSlider simAgeSlider;
        private final MaterialTextField ageField;

        private View(Model model, Controller controller) {
            super(new VBox(10), model, controller);

            Label simHeadline = new Label(Res.get("reputation.sim.headline"));
            simHeadline.getStyleClass().addAll("bisq-text-1");
            amount = getInputField("reputation.sim.burnAmount");
            score = getField(Res.get("reputation.sim.score"));
            ageField = getInputField("reputation.sim.age");
            simAgeSlider = new AgeSlider(0, ProofOfBurnService.MAX_AGE_BOOST_DAYS, 0);
            VBox.setMargin(simAgeSlider.getView().getRoot(), new Insets(15, 0, 0, 0));
            root.getChildren().addAll(simHeadline,
                    amount,
                    ageField,
                    simAgeSlider.getView().getRoot(),
                    score);
        }

        @Override
        protected void onViewAttached() {
            simAgeSlider.valueProperty().bindBidirectional(model.getAge());
            ageField.textProperty().bindBidirectional(model.getAgeAsString());
            amount.textProperty().bindBidirectional(model.getAmount());
            score.textProperty().bind(model.getScore());
        }

        @Override
        protected void onViewDetached() {
            simAgeSlider.valueProperty().unbindBidirectional(model.getAge());
            ageField.textProperty().unbindBidirectional(model.getAgeAsString());
            amount.textProperty().unbindBidirectional(model.getAmount());
            score.textProperty().unbind();
        }

        private MaterialTextField getField(String description) {
            MaterialTextField field = new MaterialTextField(description);
            field.setEditable(false);
            field.setMinWidth(MATERIAL_FIELD_WIDTH);
            field.setMaxWidth(MATERIAL_FIELD_WIDTH);
            return field;
        }

        private MaterialTextField getInputField(String key) {
            MaterialTextField field = new MaterialTextField(Res.get(key), Res.get(key + ".prompt"));
            field.setMinWidth(MATERIAL_FIELD_WIDTH);
            field.setMaxWidth(MATERIAL_FIELD_WIDTH);
            return field;
        }
    }
}
