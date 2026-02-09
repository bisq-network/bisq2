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

import bisq.desktop.components.containers.Spacer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class ValueSlider {
    private final Controller controller;

    public ValueSlider(double min, double max, double value, Function<Double, String> formatter, double majorTickUnit) {
        controller = new Controller(min, max, value, formatter, majorTickUnit);
    }

    public DoubleProperty valueProperty() {
        return controller.model.getValue();
    }

    public void setValue(double value) {
        controller.model.getValue().set(value);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(double min,
                           double max,
                           double value,
                           Function<Double, String> formatter,
                           double majorTickUnit) {
            model = new Model(min, max, value, formatter, majorTickUnit);
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
        private final double min;
        private final double max;
        private final Function<Double, String> formatter;
        private final double majorTickUnit;
        private final DoubleProperty value = new SimpleDoubleProperty();

        Model(double min, double max, double value, Function<Double, String> formatter, double majorTickUnit) {
            this.min = min;
            this.max = max;
            this.formatter = formatter;
            this.majorTickUnit = majorTickUnit;
            this.value.set(value);

        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Slider slider;

        private View(Model model, ValueSlider.Controller controller) {
            super(new VBox(2.5), model, controller);

            slider = new Slider();
            slider.setMin(model.getMin());
            slider.setMax(model.getMax());
            slider.setMajorTickUnit(model.getMajorTickUnit());
            slider.setMinorTickCount(0);
            slider.setSnapToTicks(true);

            Label min = new Label(model.getFormatter().apply(model.getMin()));
            min.getStyleClass().add("bisq-small-light-label-dimmed");

            Label max = new Label(model.getFormatter().apply(model.getMax()));
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