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

import bisq.desktop.common.threading.UIThread;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TextFieldSkin;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class MaterialPasswordField extends MaterialTextField {
    private BooleanProperty isMasked;
    private final ObjectProperty<CharSequence> password = new SimpleObjectProperty<>();

    public MaterialPasswordField() {
        this(null, null, null);
    }

    public MaterialPasswordField(String description) {
        this(description, null, null);
    }

    public MaterialPasswordField(String description, String prompt) {
        this(description, prompt, null);
    }

    public MaterialPasswordField(@Nullable String description, @Nullable String prompt, @Nullable String help) {
        super(description, prompt, help);

        //todo remove String objects for password 
        textProperty().addListener(new WeakReference<>(
                (ChangeListener<String>) (observable, oldValue, newValue) -> {
                    password.set(newValue);
                }).get());

        password.addListener(new WeakReference<>(
                (ChangeListener<CharSequence>) (observable, oldValue, newValue) -> {
                    if ((newValue == null || newValue.length() == 0) && !textProperty().isBound()) {
                        setText("");
                    }
                }).get());

        password.set(getText());
    }

    public ObjectProperty<CharSequence> passwordProperty() {
        return password;
    }

    @Override
    protected TextInputControl createTextInputControl() {
        PasswordFieldWithCopyEnabled passwordField = new PasswordFieldWithCopyEnabled();
        passwordField.setSkin(new VisiblePasswordFieldSkin(passwordField, this));
        return passwordField;
    }

    @Override
    protected double computeMinHeight(double width) {
        return getBgHeight() + 20;
    }

    public boolean isMasked() {
        return isMaskedProperty().get();
    }

    public BooleanProperty isMaskedProperty() {
        if (isMasked == null) {
            isMasked = new SimpleBooleanProperty(true);
        }
        return isMasked;
    }

    public void setIsMasked(boolean isMasked) {
        isMaskedProperty().set(isMasked);
    }

    static class VisiblePasswordFieldSkin extends TextFieldSkin {
        private final PasswordFieldWithCopyEnabled textField;
        private final BisqIconButton iconButton;
        private final MaterialPasswordField materialPasswordField;

        public VisiblePasswordFieldSkin(PasswordFieldWithCopyEnabled textField, MaterialPasswordField materialPasswordField) {
            super(textField);

            this.textField = textField;
            this.materialPasswordField = materialPasswordField;

            // We get called from the constructor of materialPasswordField, some fields might not be initiated yet, 
            // so we need to take care of which fields we access.
            materialPasswordField.isMaskedProperty().addListener(
                    new WeakReference<>((ChangeListener<Boolean>) (observable, oldValue, newValue) ->
                            handleIsMaskedChange(newValue)).get());
            iconButton = materialPasswordField.getIconButton();
            iconButton.setOnAction(e -> {
                boolean isMasked = !materialPasswordField.isMasked();
                materialPasswordField.setIsMasked(isMasked);
                handleIsMaskedChange(isMasked);
            });

            UIThread.runOnNextRenderFrame(() -> materialPasswordField.setIcon(AwesomeIcon.EYE_OPEN));
        }

        private void handleIsMaskedChange(boolean isMasked) {
            iconButton.setIcon(isMasked ? AwesomeIcon.EYE_CLOSE : AwesomeIcon.EYE_OPEN);

            // TODO if binding is used we don't get the text updated. With bi-dir binding it works
            textField.setText(textField.getText());
            textField.end();
        }

        @Override
        protected String maskText(String txt) {
            // Getting called from constructor when materialPasswordField is null
            if (materialPasswordField != null && !materialPasswordField.isMasked()) {
                return txt;
            } else {
                return super.maskText(txt);
            }
        }
    }
}