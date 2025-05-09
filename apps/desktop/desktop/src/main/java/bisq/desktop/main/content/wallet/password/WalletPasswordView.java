package bisq.desktop.main.content.wallet.password;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.validator.RequiredFieldValidator;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class WalletPasswordView extends View<VBox, WalletPasswordModel, WalletPasswordController> {
    private MaterialPasswordField currentPasswordField;
    private MaterialPasswordField passwordField;
    private MaterialPasswordField repeatedPasswordField;
    private Button pwButton;
    private ChangeListener<Boolean> passwordFieldFocusChangeListener;
    private ChangeListener<String> passwordFieldTextChangeListener;
    private ChangeListener<String> repeatedPasswordFieldChangeListener;

    public WalletPasswordView(WalletPasswordModel model, WalletPasswordController controller) {
        super(new VBox(20), model, controller);
        root.setPadding(new Insets(40, 0, 0, 0));
        currentPasswordField = new MaterialPasswordField(Res.get("wallet.send.password"));
        passwordField = new MaterialPasswordField(Res.get("wallet.password.enterPassword"));
        final RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator(Res.get("validation.empty"));
        passwordField.setValidators(requiredFieldValidator, model.getPasswordValidator());
        passwordFieldFocusChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue) validatePasswords();
        };

        passwordFieldTextChangeListener = (observable, oldvalue, newValue) -> {
            if (!Objects.equals(oldvalue, newValue)) validatePasswords();
        };

        repeatedPasswordField = new MaterialPasswordField(Res.get("wallet.password.confirmPassword"));
        repeatedPasswordField.setValidator(model.getPasswordValidator());
        repeatedPasswordFieldChangeListener = (observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) validatePasswords();
        };

        pwButton = new Button(Res.get("wallet.password.setPassword"));
        pwButton.setDisable(true);
        root.getChildren().addAll(currentPasswordField, passwordField, repeatedPasswordField, pwButton);
    }

    @Override
    protected void onViewAttached() {
        currentPasswordField.textProperty().bindBidirectional(model.getCurrentPassword());
        currentPasswordField.visibleProperty().bindBidirectional(model.getIsCurrentPasswordVisible());
        currentPasswordField.managedProperty().bindBidirectional(model.getIsCurrentPasswordVisible());
        passwordField.textProperty().bindBidirectional(model.getPassword());
        repeatedPasswordField.textProperty().bindBidirectional(model.getPasswordRepeat());
        pwButton.setOnAction(e -> {
            new Popup().backgroundInfo(Res.get("wallet.password.backupReminder"))
                    .actionButtonText(Res.get("wallet.password.setPassword"))
                    .onAction(controller::onApplyPassword)
                    .show();

        });


        passwordField.focusedProperty().addListener(passwordFieldFocusChangeListener);
        passwordField.textProperty().addListener(passwordFieldTextChangeListener);
        repeatedPasswordField.textProperty().addListener(repeatedPasswordFieldChangeListener);
    }


    @Override
    protected void onViewDetached() {
        currentPasswordField.textProperty().unbindBidirectional(model.getCurrentPassword());
        currentPasswordField.visibleProperty().unbindBidirectional(model.getIsCurrentPasswordVisible());
        currentPasswordField.managedProperty().unbindBidirectional(model.getIsCurrentPasswordVisible());
        passwordField.textProperty().unbindBidirectional(model.getPassword());
        repeatedPasswordField.textProperty().unbindBidirectional(model.getPasswordRepeat());
        passwordField.focusedProperty().removeListener(passwordFieldFocusChangeListener);
        passwordField.textProperty().removeListener(passwordFieldTextChangeListener);
        repeatedPasswordField.textProperty().removeListener(repeatedPasswordFieldChangeListener);
        pwButton.setOnAction(null);
    }

    private void validatePasswords() {
        model.getPasswordValidator().setPasswordsMatch(true);
        if (passwordField.validate()) {
            if (repeatedPasswordField.validate()) {
                if (passwordField.getText().equals(repeatedPasswordField.getText())) {
                    pwButton.setDisable(false);
                    return;
                } else {
                    model.getPasswordValidator().setPasswordsMatch(false);
                    repeatedPasswordField.validate();
                }
            }
        }
        pwButton.setDisable(true);
    }
}
