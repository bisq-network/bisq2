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

package bisq.desktop.components.controls.validator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Control;

import java.util.function.Supplier;

/**
 * An abstract class that defines the basic validation functionality for a certain control.
 */
public abstract class ValidatorBase {

    /**
     * This {@link PseudoClass} will be activated when a validation error occurs.
     */
    public static final PseudoClass PSEUDO_CLASS_ERROR = PseudoClass.getPseudoClass("error");

    /**
     * @param message will be set as the validator's {@link #message}.
     * @see #ValidatorBase()
     */
    public ValidatorBase(String message) {
        this();
        this.setMessage(message);
    }

    /**
     * When creating a new validator you need to define the validation condition by implementing {@link #eval()}.
     * <p>
     */
    public ValidatorBase() {

    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Will validate the source control.
     * <p>
     * Calls {@link #eval()} and then {@link #onEval()}.
     */
    public void validate() {
        eval();
        onEval();
    }

    /**
     * Should evaluate the validation condition and set {@link #hasErrors} to true or false. It should
     * be true when the value is invalid (it has errors) and false when the value is valid (no errors).
     * <p>
     * This method is fired once {@link #validate()} is called.
     */
    protected abstract void eval();

    /**
     * This method will update the source control after evaluating the validation condition (see {@link #eval()}).
     * <p>
     * If the validator isn't "passing" the {@link #PSEUDO_CLASS_ERROR :error} pseudoclass is applied to the
     * {@link #srcControl}.
     * <p>
     * Applies the {@link #PSEUDO_CLASS_ERROR :error} pseudo class
     */
    protected void onEval() {
        Node control = getSrcControl();
        boolean invalid = hasErrors.get();
        control.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, invalid);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The {@link Control}/{@link Node} that the validator is checking the value of.
     * <p>
     * Supports {@link Node}s because not all things that need validating are {@link Control}s.
     */
    protected SimpleObjectProperty<Node> srcControl = new SimpleObjectProperty<>();

    /**
     * @see #srcControl
     */
    public void setSrcControl(Node srcControl) {
        this.srcControl.set(srcControl);
    }

    /**
     * @see #srcControl
     */
    public Node getSrcControl() {
        return this.srcControl.get();
    }

    /**
     * @see #srcControl
     */
    public ObjectProperty<Node> srcControlProperty() {
        return this.srcControl;
    }

    /**
     * Tells whether the validator is "passing" or not.
     * <p>
     * In a validator's implementation of {@link #eval()}, if the value the validator is checking is invalid, it should
     * set this to <em>true</em>. If the value is <em>valid</em>, it should set this to <em>false</em>.
     * <p>
     * When <em>hasErrors</em> is true, the validator will automatically apply the {@link #PSEUDO_CLASS_ERROR :error}
     * pseudoclass to the {@link #srcControl}.
     */
    protected ReadOnlyBooleanWrapper hasErrors = new ReadOnlyBooleanWrapper(false);

    /**
     * @see #hasErrors
     */
    public boolean getHasErrors() {
        return hasErrors.get();
    }

    /**
     * The error message to display when the validator is <em>not</em> "passing."
     */
    protected SimpleStringProperty message = new SimpleStringProperty();

    /**
     * @see #message
     */
    public void setMessage(String msg) {
        this.message.set(msg);
    }

    /**
     * @see #message
     */
    public String getMessage() {
        return this.message.get();
    }

    /**
     * @see #message
     */
    public StringProperty messageProperty() {
        return this.message;
    }


    /***** Icon *****/
    protected SimpleObjectProperty<Supplier<Node>> iconSupplier = new SimpleObjectProperty<Supplier<Node>>();

    public void setIconSupplier(Supplier<Node> icon) {
        this.iconSupplier.set(icon);
    }

    public SimpleObjectProperty<Supplier<Node>> iconSupplierProperty() {
        return this.iconSupplier;
    }

    public Supplier<Node> getIconSupplier() {
        return iconSupplier.get();
    }

    /**
     * @param icon
     */
    public void setIcon(Node icon) {
        iconSupplier.set(() -> icon);
    }

    public Node getIcon() {
        if (iconSupplier.get() == null) {
            return null;
        }
        Node icon = iconSupplier.get().get();
        if (icon != null) {
            icon.getStyleClass().add("error-icon");
        }
        return icon;
    }
}
