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

package bisq.desktop.primary.main.content.newProfilePopup;

import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import lombok.Getter;

public class NewProfilePopupModel implements Model {
    private final IntegerProperty currentStepProperty = new SimpleIntegerProperty(0);
    @Getter
    protected final ObjectProperty<View<? extends Parent, ? extends Model, ? extends Controller>> view = new SimpleObjectProperty<>();

    public NewProfilePopupModel() {
    }

    void setView(View<? extends Parent, ? extends Model, ? extends Controller> view) {
        this.view.set(view);
    }

    public IntegerProperty currentStepProperty() {
        return currentStepProperty;
    }

    public void increaseStep() {
        currentStepProperty.set(currentStepProperty.get() + 1);
    }

    public void decreaseStep() {
        currentStepProperty.set(currentStepProperty.get() - 1);
    }

    public boolean isLastStep() {
        return currentStepProperty.get() == 2;
    }
}
