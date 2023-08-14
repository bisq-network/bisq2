package bisq.desktop.components.controls.validator;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;

public class ValidationControl {

    private final ReadOnlyObjectWrapper<ValidatorBase> activeValidator = new ReadOnlyObjectWrapper<>();

    private final Control control;

    public ValidationControl(Control control) {
        this.control = control;
    }

    public ValidatorBase getActiveValidator() {
        return activeValidator.get();
    }

    public ReadOnlyObjectProperty<ValidatorBase> activeValidatorProperty() {
        return this.activeValidator.getReadOnlyProperty();
    }

    ReadOnlyObjectWrapper<ValidatorBase> activeValidatorWritableProperty() {
        return this.activeValidator;
    }

    /**
     * list of validators that will validate the text value upon calling
     * {{@link #validate()}
     */
    private final ObservableList<ValidatorBase> validators = FXCollections.observableArrayList();

    public ObservableList<ValidatorBase> getValidators() {
        return validators;
    }

    public void setValidators(ValidatorBase... validators) {
        this.validators.addAll(validators);
    }

    /**
     * validates the text value using the list of validators provided by the user
     * {{@link #setValidators(ValidatorBase...)}
     *
     * @return true if the value is valid else false
     */
    public boolean validate() {
        for (ValidatorBase validator : validators) {
            validator.setSrcControl(control);
            validator.validate();
            if (validator.getHasErrors()) {
                activeValidator.set(validator);
                return false;
            }
        }
        activeValidator.set(null);
        return true;
    }

    public void resetValidation() {
        control.pseudoClassStateChanged(ValidatorBase.PSEUDO_CLASS_ERROR, false);
        activeValidator.set(null);
    }
}
