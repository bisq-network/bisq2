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
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TextFieldSkin;

import javax.annotation.Nullable;

public class MaterialPasswordField extends MaterialTextField {
    private BooleanProperty isMasked;
    private final ObjectProperty<CharSequence> password = new SimpleObjectProperty<>();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<String> textListener = (observable, oldValue, newValue) -> password.set(newValue);
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<CharSequence> passwordListener = (observable, oldValue, newValue) -> {
        if ((newValue == null || newValue.isEmpty()) && !textProperty().isBound()) {
            setText("");
        }
    };

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

        textProperty().addListener(new WeakChangeListener<>(textListener));
        password.addListener(new WeakChangeListener<>(passwordListener));
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
        @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
        private final ChangeListener<Boolean> passwordFieldIsMaskedListener = (observable, oldValue, newValue) -> handleIsMaskedChange(newValue);

        public VisiblePasswordFieldSkin(PasswordFieldWithCopyEnabled textField,
                                        MaterialPasswordField passwordField) {
            super(textField);

            this.textField = textField;
            this.materialPasswordField = passwordField;

            // We get called from the constructor of passwordField, some fields might not be initiated yet,
            // so we need to take care of which fields we access.
            passwordField.isMaskedProperty().addListener(new WeakChangeListener<>(passwordFieldIsMaskedListener));
            iconButton = passwordField.getIconButton();
            iconButton.setOnAction(e -> {
                boolean isMasked = !passwordField.isMasked();
                passwordField.setIsMasked(isMasked);
                handleIsMaskedChange(isMasked);
            });

            UIThread.runOnNextRenderFrame(() -> passwordField.setIcon(AwesomeIcon.EYE_OPEN));
        }

        private void handleIsMaskedChange(boolean isMasked) {
            iconButton.setIcon(isMasked ? AwesomeIcon.EYE_CLOSE : AwesomeIcon.EYE_OPEN);

            // FIXME (low prio) if binding is used we don't get the text updated. With bi-dir binding it works
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