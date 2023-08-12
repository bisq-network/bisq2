package bisq.desktop.components.controls.validator;

import javafx.scene.control.TextInputControl;

public class TextMinLengthValidator extends ValidatorBase {

    private static final int DEFAULT_MIN_LENGTH = 8;
    private final int minLength;

    public TextMinLengthValidator(String message, int minLength) {
        super(message);
        this.minLength = minLength;
    }

    public TextMinLengthValidator(String message) {
        this(message, DEFAULT_MIN_LENGTH);
    }

    @Override
    protected void eval() {
        var textField = (TextInputControl) srcControl.get();
        hasErrors.set(textField.getText().length() < minLength);
    }
}
