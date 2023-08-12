package bisq.desktop.components.controls.validator;

import javafx.scene.control.TextInputControl;

public class RequiredFieldValidator extends ValidatorBase {

    public RequiredFieldValidator(String message) {
        super(message);
    }

    @Override
    protected void eval() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        hasErrors.set(textField.getText() == null || textField.getText().isEmpty());
    }
}
