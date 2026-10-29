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

package bisq.desktop.main.content.settings;

import bisq.desktop.common.Layout;
import bisq.desktop.components.controls.MaterialTextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.fxmisc.easybind.Subscription;

import java.util.function.Consumer;

public class SettingsViewUtils {
    public static final double TEXT_FIELD_WIDTH = 500;

    public static Label getHeadline(String text) {
        Label headline = new Label(text);
        headline.getStyleClass().add("large-thin-headline");
        return headline;
    }

    public static Region getLineAfterHeadline(double spacing) {
        Region line = Layout.hLine();
        VBox.setMargin(line, new Insets(7.5 - spacing, 0, 20 - spacing, 0));
        return line;
    }

    /**
     * Shows the value in a settings text field and passes a typed value to {@code onCommit}. The field passes the typed
     * text only when it loses the focus, when the user presses Enter, or when the returned subscription is
     * unsubscribed, and only if the text differs from the shown value and the validators of the field accept it.
     * <p>
     * A bidirectional binding of the text would write each valid prefix of a typed value, e.g. 9 % while the user
     * types 90 %.
     * <p>
     * The field must not have a converter set with {@link MaterialTextField#setStringConverter}. That converter
     * formats the text when the field loses the focus, before this method validates it, e.g. 0.9 % becomes 1 %.
     * <p>
     * {@code onCommit} has to write the value to the service and not only to the model: the view unsubscribes when it
     * gets detached, and at that time the controller has already removed its bindings to the service.
     */
    public static <T> Subscription bindOnCommit(MaterialTextField field,
                                                ObservableValue<T> value,
                                                StringConverter<T> converter,
                                                Consumer<T> onCommit) {
        field.setText(converter.toString(value.getValue()));
        Runnable commit = () -> {
            if (!field.getText().equals(converter.toString(value.getValue())) && field.validate()) {
                onCommit.accept(converter.fromString(field.getText()));
                field.setText(converter.toString(value.getValue()));
            }
        };
        ChangeListener<T> valueListener = (observable, oldValue, newValue) -> {
            if (!field.textInputFocusedProperty().get()) {
                field.setText(converter.toString(newValue));
            }
        };
        ChangeListener<Boolean> focusListener = (observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                commit.run();
            }
        };
        EventHandler<ActionEvent> enterHandler = e -> commit.run();

        value.addListener(valueListener);
        field.textInputFocusedProperty().addListener(focusListener);
        field.getTextInputControl().addEventHandler(ActionEvent.ACTION, enterHandler);
        return () -> {
            commit.run();
            value.removeListener(valueListener);
            field.textInputFocusedProperty().removeListener(focusListener);
            field.getTextInputControl().removeEventHandler(ActionEvent.ACTION, enterHandler);
        };
    }
}
