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

import bisq.desktop.common.view.Model;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class NewProfilePopupModel implements Model {
    private final IntegerProperty currentStepProperty = new SimpleIntegerProperty(0);

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
