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

@Getter
public class RangeSlider extends Control {
    private final DoubleProperty min = new SimpleDoubleProperty(0);
    private final DoubleProperty max = new SimpleDoubleProperty(100);
    private final DoubleProperty lowValue = new SimpleDoubleProperty(25);
    private final DoubleProperty highValue = new SimpleDoubleProperty(75);
    private final BooleanProperty lowThumbFocused = new SimpleBooleanProperty(false);
    private final BooleanProperty highThumbFocused = new SimpleBooleanProperty(false);

    public RangeSlider() {
        getStyleClass().add("range-slider");
        setSkin(createDefaultSkin());
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new bisq.desktop.components.controls.skins.RangeSliderSkin(this);
    }
}
