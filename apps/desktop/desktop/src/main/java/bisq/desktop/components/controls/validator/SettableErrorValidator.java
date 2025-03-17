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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SettableErrorValidator extends ValidatorBase {
    private final BooleanProperty isInvalid = new SimpleBooleanProperty();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> isInvalidListener = (observable, oldValue, newValue) -> eval();

    public SettableErrorValidator() {
        super();

        isInvalid.addListener(new WeakChangeListener<>(isInvalidListener));
    }

    public SettableErrorValidator(String message) {
        super(message);

        isInvalid.addListener(new WeakChangeListener<>(isInvalidListener));
    }

    @Override
    protected void eval() {
        hasErrors.set(getIsInvalid());
    }

    public boolean getIsInvalid() {
        return isInvalid.get();
    }

    public BooleanProperty isInvalidProperty() {
        return isInvalid;
    }

    public void setIsInvalid(boolean isInvalid) {
        this.isInvalid.set(isInvalid);
        eval();
    }
}