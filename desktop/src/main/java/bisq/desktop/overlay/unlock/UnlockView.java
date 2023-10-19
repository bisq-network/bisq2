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

package bisq.desktop.overlay.unlock;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.validator.RequiredFieldValidator;
import bisq.desktop.components.controls.validator.TextMinLengthValidator;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

import static org.fxmisc.easybind.EasyBind.subscribe;

@Slf4j
public class UnlockView extends View<VBox, UnlockModel, UnlockController> {
    private final MaterialPasswordField password;
    private final Button unlockButton, cancelButton;
    private boolean isFirstTimeThatFocusChanges;
    private Subscription focusPin;

    public UnlockView(UnlockModel model, UnlockController controller) {
        super(new VBox(20), model, controller);

        root.setPrefWidth(750);
        root.setPadding(new Insets(30, 30, 30, 30));

        Label headline = new Label(Res.get("unlock.headline"));
        headline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        password = new MaterialPasswordField(Res.get("user.password.enterPassword"));
        password.setValidators(
                new RequiredFieldValidator(Res.get("validation.empty")),
                new TextMinLengthValidator(Res.get("validation.password.tooShort")));

        unlockButton = new Button(Res.get("unlock.button"));
        unlockButton.setDefaultButton(true);
        unlockButton.setFocusTraversable(false);

        cancelButton = new Button(Res.get("action.cancel"));
        cancelButton.setFocusTraversable(false);

        isFirstTimeThatFocusChanges = true;
        HBox buttons = new HBox(20, unlockButton, cancelButton);
        HBox.setMargin(buttons, new Insets(20, 0, 0, 0));
        root.getChildren().setAll(headline, password, buttons);
    }

    @Override
    protected void onViewAttached() {
        password.requestFocus();
        password.passwordProperty().bindBidirectional(model.getPassword());
        password.isMaskedProperty().bindBidirectional(model.getPasswordIsMasked());

        unlockButton.setOnAction(e -> controller.onUnlock());
        cancelButton.setOnAction(e -> controller.onCancel());

        focusPin = subscribe(password.textInputFocusedProperty(), this::validatePasswordWhenFocusOut);
    }

    @Override
    protected void onViewDetached() {
        password.passwordProperty().unbindBidirectional(model.getPassword());
        password.isMaskedProperty().unbindBidirectional(model.getPasswordIsMasked());

        unlockButton.setOnAction(null);
        cancelButton.setOnAction(null);

        focusPin.unsubscribe();
    }

    public boolean validatePassword() {
        return password.validate();
    }

    public void resetValidation() {
        password.resetValidation();
        isFirstTimeThatFocusChanges = true;
    }

    private void validatePasswordWhenFocusOut(boolean focused) {
        if (!focused && !isFirstTimeThatFocusChanges) {
            validatePassword();
        }
        if(isFirstTimeThatFocusChanges) {
            isFirstTimeThatFocusChanges = false;
        }
    }
}
