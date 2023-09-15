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

import javafx.beans.property.StringProperty;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import javax.annotation.Nullable;

public class WrappingText extends TextFlow {
    private final Text textControl;

    public WrappingText() {
        this(null, null);
    }

    public WrappingText(String text) {
        this(text, null);
    }

    public WrappingText(@Nullable String text, @Nullable String styleClass) {
        textControl = text == null ? new Text() : new Text(text);
        if (styleClass != null) {
            addStyleClass(styleClass);
        }
        getChildren().add(textControl);
    }

    public final StringProperty textProperty() {
        return textControl.textProperty();
    }

    public void setText(String text) {
        textControl.setText(text);
    }

    public void addStyleClass(String styleClass) {
        textControl.getStyleClass().add(styleClass);
    }

    public void addStyleClasses(String... styleClasses) {
        textControl.getStyleClass().addAll(styleClasses);
    }

    public void removeStyleClass(String styleClass) {
        textControl.getStyleClass().remove(styleClass);
    }

    public void removeStyleClasses(String... styleClasses) {
        textControl.getStyleClass().removeAll(styleClasses);
    }
}