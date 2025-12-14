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

import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

public class SliderWithValue {

    private final Controller controller;

    public SliderWithValue(double defaultValue,
                           double minValue,
                           double maxValue,
                           String descriptionKey,
                           Function<Double, String> formatter,
                           StringConverter<Number> stringConverter,
                           double majorTickUnit) {
        controller = new Controller(defaultValue, minValue, maxValue, descriptionKey, formatter, stringConverter, majorTickUnit);
    }

    public VBox getViewRoot() {
        return controller.getView().getRoot();
    }

    public ReadOnlyDoubleProperty valueProperty() {
        return controller.model.getValue();
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;

        private Controller(double defaultValue,
                           double minValue,
                           double maxValue,
                           String descriptionKey,
                           Function<Double, String> formatter,
                           StringConverter<Number> stringConverter,
                           double majorTickUnit) {
            model = new Model(defaultValue, minValue, maxValue, descriptionKey, formatter, stringConverter, majorTickUnit);
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
        private final double defaultValue;
        private final double minValue;
        private final double maxValue;
        private final DoubleProperty value = new SimpleDoubleProperty();
        private final String descriptionKey;
        private final Function<Double, String> formatter;
        private final StringConverter<Number> stringConverter;
        private final double majorTickUnit;

        public Model(double defaultValue,
                     double minValue,
                     double maxValue,
                     String descriptionKey,
                     Function<Double, String> formatter,
                     StringConverter<Number> stringConverter,
                     double majorTickUnit) {
            this.descriptionKey = descriptionKey;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.formatter = formatter;
            this.stringConverter = stringConverter;
            this.majorTickUnit = majorTickUnit;

            this.value.set(defaultValue);
        }
    }

    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final MaterialTextField value;
        private final ValueSlider valueSlider;

        private View(Model model, Controller controller) {
            super(new VBox(5), model, controller);

            value = new MaterialTextField(Res.get(model.getDescriptionKey()), Res.get(model.getDescriptionKey() + ".prompt"));
            valueSlider = new ValueSlider(model.getMinValue(), model.getMaxValue(), model.getDefaultValue(), model.getFormatter(), model.getMajorTickUnit());
            value.setStringConverter(model.getStringConverter());

            root.getChildren().addAll(value, valueSlider.getView().getRoot());
        }

        @Override
        protected void onViewAttached() {
            Bindings.bindBidirectional(value.textProperty(), model.getValue(), model.getStringConverter());
            valueSlider.valueProperty().bindBidirectional(model.getValue());
        }

        @Override
        protected void onViewDetached() {
            Bindings.unbindBidirectional(value.textProperty(), model.getValue());
            valueSlider.valueProperty().unbindBidirectional(model.getValue());
        }
    }
}
