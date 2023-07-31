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

package bisq.desktop.main.content.user.reputation.components;

import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
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
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.util.function.BiFunction;

public class Simulation {

    private final Controller controller;

    public Simulation(BiFunction<String, Number, String> calculateSimScore) {
        controller = new Controller(calculateSimScore);
    }

    public VBox getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final BiFunction<String, Number, String> calculateSimScore;
        private Subscription agePin, ageAsStringPin, scorePin;

        private Controller(BiFunction<String, Number, String> calculateSimScore) {
            this.calculateSimScore = calculateSimScore;
            model = new Model();
            view = new View(model, this);

            model.getSimAmount().set("100");
            model.getSimAge().set(0);
            model.getSimAgeAsString().set("0");
        }

        @Override
        public void onActivate() {
            agePin = EasyBind.subscribe(model.getSimAge(), age -> model.getSimAgeAsString().set(String.valueOf(age)));
            ageAsStringPin = EasyBind.subscribe(model.getSimAgeAsString(), ageAsString -> {
                try {
                    model.getSimAge().set(Integer.parseInt(ageAsString));
                } catch (Exception e) {
                }
            });

            MonadicBinding<String> binding = EasyBind.combine(model.getSimAmount(), model.getSimAge(), calculateSimScore);
            scorePin = EasyBind.subscribe(binding, score -> model.getSimScore().set(score));
        }

        @Override
        public void onDeactivate() {
            agePin.unsubscribe();
            ageAsStringPin.unsubscribe();
            scorePin.unsubscribe();
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final IntegerProperty simAge = new SimpleIntegerProperty();
        private final StringProperty simAgeAsString = new SimpleStringProperty();
        private final StringProperty simAmount = new SimpleStringProperty();
        private final StringProperty simScore = new SimpleStringProperty();
    }

    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final MaterialTextField simAmount;
        private final AgeSlider simAgeSlider;
        private final MaterialTextField simScore;
        private final MaterialTextField ageField;

        private View(Model model,
                     Controller controller) {
            super(new VBox(10), model, controller);

            Label simHeadline = new Label(Res.get("user.reputation.sim.headline"));
            simHeadline.getStyleClass().addAll("bisq-text-1");
            simAmount = getInputField("user.reputation.sim.burnAmount");
            ageField = getInputField("user.reputation.sim.age");
            simAgeSlider = new AgeSlider(0, 400, 0);
            simScore = getField(Res.get("user.reputation.sim.score"));
            VBox.setMargin(simAgeSlider.getView().getRoot(), new Insets(15, 0, 0, 0));
            root.getChildren().addAll(simHeadline,
                    simAmount,
                    ageField,
                    simAgeSlider.getView().getRoot(),
                    simScore);
        }

        @Override
        protected void onViewAttached() {
            simAgeSlider.valueProperty().bindBidirectional(model.getSimAge());
            ageField.textProperty().bindBidirectional(model.getSimAgeAsString());
            simAmount.textProperty().bindBidirectional(model.getSimAmount());
            simScore.textProperty().bind(model.getSimScore());
        }

        @Override
        protected void onViewDetached() {
            simAgeSlider.valueProperty().unbindBidirectional(model.getSimAge());
            ageField.textProperty().unbindBidirectional(model.getSimAgeAsString());
            simAmount.textProperty().unbindBidirectional(model.getSimAmount());
            simScore.textProperty().unbind();
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