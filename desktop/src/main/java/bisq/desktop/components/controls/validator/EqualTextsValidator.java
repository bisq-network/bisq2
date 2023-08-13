package bisq.desktop.components.controls.validator;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

public class EqualTextsValidator extends ValidatorBase {

    private final SimpleObjectProperty<Node> other = new SimpleObjectProperty<>();

    public EqualTextsValidator(String message, Node other) {
        super(message);
        this.other.set(other);
    }

    @Override
    protected void eval() {
        var textField = (TextInputControl) srcControl.get();
        var otherTextField = (TextInputControl) other.get();
        hasErrors.set(!textField.getText().equals(otherTextField.getText()));
    }
}
