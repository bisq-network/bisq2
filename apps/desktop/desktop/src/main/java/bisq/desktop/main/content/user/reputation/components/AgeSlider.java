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

import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgeSlider {
    private final Controller controller;

    public AgeSlider(int min, int max, int value) {
        controller = new Controller(min, max, value);
    }

    public IntegerProperty valueProperty() {
        return controller.model.getValue();
    }

    public void setValue(int value) {
        controller.model.getValue().set(value);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(int min, int max, int value) {
            model = new Model(min, max, value);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final int min;
        private final int max;
        private final IntegerProperty value = new SimpleIntegerProperty();

        Model(int min, int max, int value) {
            this.min = min;
            this.max = max;
            this.value.set(value);
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Slider slider;

        private View(Model model, AgeSlider.Controller controller) {
            super(new VBox(10), model, controller);

            slider = new Slider();
            slider.setMin(model.getMin());
            slider.setMax(model.getMax());

            Label min = new Label(model.getMin() + " " + Res.get("temporal.days"));
            min.getStyleClass().add("bisq-small-light-label-dimmed");

            Label max = new Label(model.getMax() + " " + Res.get("temporal.days"));
            max.getStyleClass().add("bisq-small-light-label-dimmed");

            root.getChildren().addAll(slider, new HBox(min, Spacer.fillHBox(), max));
        }

        @Override
        protected void onViewAttached() {
            slider.valueProperty().bindBidirectional(model.getValue());
        }

        @Override
        protected void onViewDetached() {
            slider.valueProperty().unbindBidirectional(model.getValue());
        }
    }
}