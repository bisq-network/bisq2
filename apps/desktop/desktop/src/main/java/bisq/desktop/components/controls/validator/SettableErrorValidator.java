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
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;

@Slf4j
public class SettableErrorValidator extends ValidatorBase {
    private final BooleanProperty invalid = new SimpleBooleanProperty();

    public SettableErrorValidator() {
        super();

        invalid.addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) -> eval()).get());
    }

    @Override
    protected void eval() {
        hasErrors.set(getInvalid());
    }

    public boolean getInvalid() {
        return invalid.get();
    }

    public BooleanProperty invalidProperty() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        log.error("setInvalid {}", invalid);
        this.invalid.set(invalid);
        eval();
    }
}