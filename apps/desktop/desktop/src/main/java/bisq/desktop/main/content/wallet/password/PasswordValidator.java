package bisq.desktop.main.content.wallet.password;

import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;
import lombok.Setter;


@Setter
public final class PasswordValidator extends ValidatorBase {

    private boolean passwordsMatch = true;

    @Override
    protected void eval() {
        if (srcControl.get() instanceof TextInputControl) {
            evalTextInputField();
        }
    }

    private void evalTextInputField() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        String text = textField.getText();
        hasErrors.set(false);

        if (!passwordsMatch) {
            hasErrors.set(true);
            message.set(Res.get("wallet.password.passwordsDoNotMatch"));
        } else if (text.length() < 8) {
            hasErrors.set(true);
            message.set(Res.get("wallet.password.passwordTooShort"));
        } else if (text.length() > 50) {
            hasErrors.set(true);
            message.set(Res.get("wallet.password.passwordTooLong"));
        }
    }

}

