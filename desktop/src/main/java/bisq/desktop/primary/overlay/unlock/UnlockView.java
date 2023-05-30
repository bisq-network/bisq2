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

package bisq.desktop.primary.overlay.unlock;

import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.utils.validation.PasswordValidator;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnlockView extends View<VBox, UnlockModel, UnlockController> {
    private Scene rootScene;
    private final MaterialPasswordField password;
    private final Button unlockButton, cancelButton;
    private final Label headline;

    public UnlockView(UnlockModel model, UnlockController controller) {
        super(new VBox(20), model, controller);

        root.setPrefWidth(750);
        root.setPadding(new Insets(30, 30, 30, 30));

        headline = new Label(Res.get("unlock.headline"));
        headline.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        password = new MaterialPasswordField(Res.get("user.password.enterPassword"));
        password.setValidator(new PasswordValidator());

        unlockButton = new Button(Res.get("unlock.button"));
        unlockButton.setDefaultButton(true);
        cancelButton = new Button(Res.get("cancel"));
        HBox buttons = new HBox(20, unlockButton, cancelButton);
        HBox.setMargin(buttons, new Insets(20, 0, 0, 0));
        root.getChildren().setAll(headline, password, buttons);
    }

    @Override
    protected void onViewAttached() {
        password.passwordProperty().bindBidirectional(model.getPassword());
        password.isMaskedProperty().bindBidirectional(model.getPasswordIsMasked());
        unlockButton.disableProperty().bind(model.getUnlockButtonDisabled());

        unlockButton.setOnAction(e -> controller.onUnlock());
        cancelButton.setOnAction(e -> controller.onCancel());

        // Replace the key handler of OverlayView as we do not support escape/enter at this popup
        rootScene = root.getScene();
        rootScene.setOnKeyReleased(keyEvent -> {
            KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, controller::onQuit);
            KeyHandlerUtil.handleDevModeKeyEvent(keyEvent);
            KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
            });
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, () -> {
            });
        });
    }

    @Override
    protected void onViewDetached() {
        password.passwordProperty().unbindBidirectional(model.getPassword());
        password.isMaskedProperty().unbindBidirectional(model.getPasswordIsMasked());
        unlockButton.disableProperty().unbind();

        unlockButton.setOnAction(null);
        cancelButton.setOnAction(null);
        rootScene.setOnKeyReleased(null);
    }
}
