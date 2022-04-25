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

import bisq.desktop.skins.BisqToggleButtonSkin;
import javafx.css.*;
import javafx.css.converter.PaintConverter;
import javafx.scene.control.Skin;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BisqToggleButton extends ToggleButton {

    public BisqToggleButton() {
        this.getStyleClass().add("bisq-toggle-button");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new BisqToggleButtonSkin(this);
    }

    
    private final StyleableObjectProperty<Paint> toggleColor = new SimpleStyleableObjectProperty<>(StyleableProperties.TOGGLE_COLOR,
            BisqToggleButton.this,
            "toggleColor");

    public Paint getToggleColor() {
        return toggleColor.get();
    }

    public StyleableObjectProperty<Paint> toggleColorProperty() {
        return toggleColor;
    }
    
    private final StyleableObjectProperty<Paint> untoggleColor = new SimpleStyleableObjectProperty<>(StyleableProperties.UNTOGGLE_COLOR,
            BisqToggleButton.this,
            "unToggleColor");

    public Paint getUnToggleColor() {
        return untoggleColor.get();
    }

    public StyleableObjectProperty<Paint> unToggleColorProperty() {
        return untoggleColor;
    }
    
    private final StyleableObjectProperty<Paint> toggleLineColor = new SimpleStyleableObjectProperty<>(
            StyleableProperties.TOGGLE_LINE_COLOR,
            BisqToggleButton.this,
            "toggleLineColor");

    public Paint getToggleLineColor() {
        return toggleLineColor.get();
    }

    public StyleableObjectProperty<Paint> toggleLineColorProperty() {
        return toggleLineColor;
    }
    
    private final StyleableObjectProperty<Paint> untoggleLineColor = new SimpleStyleableObjectProperty<>(
            StyleableProperties.UNTOGGLE_LINE_COLOR,
            BisqToggleButton.this,
            "unToggleLineColor");

    public Paint getUnToggleLineColor() {
        return untoggleLineColor.get();
    }

    public StyleableObjectProperty<Paint> unToggleLineColorProperty() {
        return untoggleLineColor;
    }

    private final StyleableDoubleProperty size = new SimpleStyleableDoubleProperty(
            StyleableProperties.SIZE,
            BisqToggleButton.this,
            "size");

    public double getSize() {
        return size.get();
    }

    public StyleableDoubleProperty sizeProperty() {
        return size;
    }


    private static class StyleableProperties {
        private static final CssMetaData<BisqToggleButton, Paint> TOGGLE_COLOR = new CssMetaData<>("-bisq-toggle-color", PaintConverter.getInstance(), Color.valueOf("#009688")) {
            @Override
            public boolean isSettable(BisqToggleButton control) {
                return !control.toggleColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(BisqToggleButton control) {
                return control.toggleColorProperty();
            }
        };

        private static final CssMetaData<BisqToggleButton, Paint> UNTOGGLE_COLOR = new CssMetaData<>("-bisq-untoggle-color", PaintConverter.getInstance(), Color.valueOf("#FAFAFA")) {
            @Override
            public boolean isSettable(BisqToggleButton control) {
                return !control.untoggleColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(BisqToggleButton control) {
                return control.unToggleColorProperty();
            }
        };

        private static final CssMetaData<BisqToggleButton, Paint> TOGGLE_LINE_COLOR = new CssMetaData<>("-bisq-toggle-line-color", PaintConverter.getInstance(), Color.valueOf("#77C2BB")) {
            @Override
            public boolean isSettable(BisqToggleButton control) {
                return !control.toggleLineColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(BisqToggleButton control) {
                return control.toggleLineColorProperty();
            }
        };

        private static final CssMetaData<BisqToggleButton, Paint> UNTOGGLE_LINE_COLOR = new CssMetaData<>("-bisq-untoggle-line-color", PaintConverter.getInstance(), Color.valueOf("#999999")) {
            @Override
            public boolean isSettable(BisqToggleButton control) {
                return !control.untoggleLineColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(BisqToggleButton control) {
                return control.unToggleLineColorProperty();
            }
        };

        private static final CssMetaData<BisqToggleButton, Number> SIZE = new CssMetaData<>("-bisq-size", StyleConverter.getSizeConverter(), 10.0) {
            @Override
            public boolean isSettable(BisqToggleButton control) {
                return !control.size.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(BisqToggleButton control) {
                return control.sizeProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> CHILD_STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(ToggleButton.getClassCssMetaData());
            Collections.addAll(styleables,
                    SIZE,
                    TOGGLE_COLOR,
                    UNTOGGLE_COLOR,
                    TOGGLE_LINE_COLOR,
                    UNTOGGLE_LINE_COLOR
            );
            CHILD_STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return StyleableProperties.CHILD_STYLEABLES;
    }
}

