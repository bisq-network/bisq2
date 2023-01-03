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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TextFieldSkin;

import javax.annotation.Nullable;

public class MaterialPasswordField extends MaterialTextField {
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
    }

    @Override
    protected TextInputControl createTextInputControl() {
        PasswordField passwordField = new PasswordField();
        passwordField.setSkin(new VisiblePasswordFieldSkin(passwordField, this));

        return passwordField;
    }

    static class VisiblePasswordFieldSkin extends TextFieldSkin {
        private boolean isMasked = true;

        public VisiblePasswordFieldSkin(PasswordField textField, MaterialPasswordField materialPasswordField) {
            super(textField);

            // iconButton is not created when we get called, so we delay it to next render frame
            UIThread.runOnNextRenderFrame(() -> {
                BisqIconButton iconButton = materialPasswordField.getIconButton();
                materialPasswordField.setIcon(AwesomeIcon.EYE_OPEN);
                iconButton.setOnAction(e -> {
                    iconButton.setIcon(isMasked ? AwesomeIcon.EYE_CLOSE : AwesomeIcon.EYE_OPEN);
                    isMasked = !isMasked;

                    // TODO if binding is used we don't get the text updated. With bi-dir binding it works
                    textField.setText(textField.getText());
                    textField.end();
                   /* if(!textField.textProperty().isBound()){
                        textField.setText(textField.getText());
                        textField.end();
                    }*/
                });
            });
        }

        @Override
        protected String maskText(String txt) {
            if (getSkinnable() instanceof PasswordField && isMasked) {
                return "•".repeat(txt.length());
            } else {
                return txt;
            }
        }
    }
}