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

import bisq.desktop.components.controls.skins.SwitchSkin;
import javafx.css.*;
import javafx.css.converter.PaintConverter;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Switch extends ToggleButton {

    public Switch() {
        this("");
    }

    public Switch(String text) {
        this(text, null);
    }

    public Switch(String text, Node graphic) {
        super(text, graphic);
        this.getStyleClass().add("bisq-toggle-button");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new SwitchSkin(this);
    }


    private final StyleableObjectProperty<Paint> toggleColor = new SimpleStyleableObjectProperty<>(StyleableProperties.TOGGLE_COLOR,
            Switch.this,
            "toggleColor");

    public Paint getToggleColor() {
        return toggleColor.get();
    }

    public StyleableObjectProperty<Paint> toggleColorProperty() {
        return toggleColor;
    }

    private final StyleableObjectProperty<Paint> unToggleColor = new SimpleStyleableObjectProperty<>(StyleableProperties.UN_TOGGLE_COLOR,
            Switch.this,
            "unToggleColor");

    public Paint getUnToggleColor() {
        return unToggleColor.get();
    }

    public StyleableObjectProperty<Paint> unToggleColorProperty() {
        return unToggleColor;
    }

    private final StyleableObjectProperty<Paint> toggleLineColor = new SimpleStyleableObjectProperty<>(
            StyleableProperties.TOGGLE_LINE_COLOR,
            Switch.this,
            "toggleLineColor");

    public Paint getToggleLineColor() {
        return toggleLineColor.get();
    }

    public StyleableObjectProperty<Paint> toggleLineColorProperty() {
        return toggleLineColor;
    }

    private final StyleableObjectProperty<Paint> unToggleLineColor = new SimpleStyleableObjectProperty<>(
            StyleableProperties.UN_TOGGLE_LINE_COLOR,
            Switch.this,
            "unToggleLineColor");

    public Paint getUnToggleLineColor() {
        return unToggleLineColor.get();
    }

    public StyleableObjectProperty<Paint> unToggleLineColorProperty() {
        return unToggleLineColor;
    }

    private final StyleableDoubleProperty size = new SimpleStyleableDoubleProperty(
            StyleableProperties.SIZE,
            Switch.this,
            "size");

    public double getSize() {
        return size.get();
    }

    public StyleableDoubleProperty sizeProperty() {
        return size;
    }


    private static class StyleableProperties {
        private static final CssMetaData<Switch, Paint> TOGGLE_COLOR = new CssMetaData<>("-bisq-toggle-color", PaintConverter.getInstance(), Color.valueOf("#009688")) {
            @Override
            public boolean isSettable(Switch control) {
                return !control.toggleColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(Switch control) {
                return control.toggleColorProperty();
            }
        };

        private static final CssMetaData<Switch, Paint> UN_TOGGLE_COLOR = new CssMetaData<>("-bisq-un-toggle-color", PaintConverter.getInstance(), Color.valueOf("#FAFAFA")) {
            @Override
            public boolean isSettable(Switch control) {
                return !control.unToggleColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(Switch control) {
                return control.unToggleColorProperty();
            }
        };

        private static final CssMetaData<Switch, Paint> TOGGLE_LINE_COLOR = new CssMetaData<>("-bisq-toggle-line-color", PaintConverter.getInstance(), Color.valueOf("#77C2BB")) {
            @Override
            public boolean isSettable(Switch control) {
                return !control.toggleLineColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(Switch control) {
                return control.toggleLineColorProperty();
            }
        };

        private static final CssMetaData<Switch, Paint> UN_TOGGLE_LINE_COLOR = new CssMetaData<>("-bisq-un-toggle-line-color", PaintConverter.getInstance(), Color.valueOf("#999999")) {
            @Override
            public boolean isSettable(Switch control) {
                return !control.unToggleLineColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(Switch control) {
                return control.unToggleLineColorProperty();
            }
        };

        private static final CssMetaData<Switch, Number> SIZE = new CssMetaData<>("-bisq-size", StyleConverter.getSizeConverter(), 10.0) {
            @Override
            public boolean isSettable(Switch control) {
                return !control.size.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Switch control) {
                return control.sizeProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> CHILD_STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(ToggleButton.getClassCssMetaData());
            Collections.addAll(styleables,
                    SIZE,
                    TOGGLE_COLOR,
                    UN_TOGGLE_COLOR,
                    TOGGLE_LINE_COLOR,
                    UN_TOGGLE_LINE_COLOR
            );
            CHILD_STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return StyleableProperties.CHILD_STYLEABLES;
    }
}

