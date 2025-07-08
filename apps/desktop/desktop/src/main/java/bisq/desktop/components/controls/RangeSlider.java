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

package bisq.desktop.components.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import lombok.Getter;

public class RangeSlider extends Control {
    // TODO: These do not need to be properties
    private final DoubleProperty min = new SimpleDoubleProperty(this, "min", 0);
    private final DoubleProperty max = new SimpleDoubleProperty(this, "max", 100);

    private final DoubleProperty lowValue = new SimpleDoubleProperty(this, "lowValue", 25);
    private final DoubleProperty highValue = new SimpleDoubleProperty(this, "highValue", 75);
    @Getter
    private final BooleanProperty lowThumbFocused = new SimpleBooleanProperty(false);
    @Getter
    private final BooleanProperty highThumbFocused = new SimpleBooleanProperty(false);

    public RangeSlider() {
        getStyleClass().add("range-slider");
        setSkin(createDefaultSkin());
    }

    public double getMin() {
        return min.get();
    }

    public void setMin(double value) {
        min.set(value);
    }

    public DoubleProperty minProperty() {
        return min;
    }

    public double getMax() {
        return max.get();
    }

    public void setMax(double value) {
        max.set(value);
    }

    public DoubleProperty maxProperty() {
        return max;
    }

    public double getLowValue() {
        return lowValue.get();
    }

    public void setLowValue(double value) {
        lowValue.set(value);
    }

    public DoubleProperty lowValueProperty() {
        return lowValue;
    }

    public double getHighValue() {
        return highValue.get();
    }

    public void setHighValue(double value) {
        highValue.set(value);
    }

    public DoubleProperty highValueProperty() {
        return highValue;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new bisq.desktop.components.controls.skins.RangeSliderSkin(this);
    }
}
