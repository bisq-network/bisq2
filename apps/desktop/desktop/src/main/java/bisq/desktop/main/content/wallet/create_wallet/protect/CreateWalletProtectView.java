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

package bisq.desktop.main.content.wallet.create_wallet.protect;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class CreateWalletProtectView extends View<StackPane, CreateWalletProtectModel, CreateWalletProtectController> {
    private final MaterialTextField passwordField, confirmPasswordField;
    private static final TextMaxLengthValidator PASSWORD_MAX_LENGTH_VALIDATOR =
            new TextMaxLengthValidator(100, Res.get("wallet.protectWallet.password.tooLong", 100));

    public CreateWalletProtectView(CreateWalletProtectModel model,
                               CreateWalletProtectController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        VBox content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("wallet.protectWallet.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        VBox.setMargin(headlineLabel, new Insets(0, 0, 10, 0));

        Label descriptionLabel = new Label(Res.get("wallet.protectWallet.description"));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(550);
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.getStyleClass().add("bisq-text-1");
        VBox.setMargin(descriptionLabel, new Insets(0, 0, 40, 0));

        passwordField = addField(Res.get("wallet.protectWallet.password.setPassword"), Res.get("wallet.protectWallet.password.setPassword.placeholder"));
        passwordField.setEditable(true);
        passwordField.setMaxWidth(500);
        passwordField.setValidators(PASSWORD_MAX_LENGTH_VALIDATOR);
        VBox.setMargin(passwordField, new Insets(0, 0, 10, 0));

        confirmPasswordField = addField(Res.get("wallet.protectWallet.password.confirmPassword"), Res.get("wallet.protectWallet.password.confirmPassword.placeholder"));
        confirmPasswordField.setEditable(true);
        confirmPasswordField.setMaxWidth(500);
        confirmPasswordField.setValidators(PASSWORD_MAX_LENGTH_VALIDATOR);

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, descriptionLabel, passwordField, confirmPasswordField, Spacer.fillVBox());

        root.getChildren().addAll(content);
    }

    @Override
    protected void onViewAttached() {
        passwordField.textProperty().bindBidirectional(model.getPassword());
        confirmPasswordField.textProperty().bindBidirectional(model.getConfirmPassword());
    }

    @Override
    protected void onViewDetached() {
        passwordField.resetValidation();
        confirmPasswordField.resetValidation();

        passwordField.textProperty().unbindBidirectional(model.getPassword());
        confirmPasswordField.textProperty().unbindBidirectional(model.getConfirmPassword());
    }

    private MaterialTextField addField(String description, @Nullable String prompt) {
        MaterialTextField field = new MaterialTextField(description, prompt);
        field.setEditable(false);
        return field;
    }

}
