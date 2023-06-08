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

package bisq.desktop.primary.overlay.bisq_easy.take_offer;

import bisq.desktop.common.view.NavigationModel;
import bisq.desktop.common.view.NavigationTarget;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class TakeOfferModel extends NavigationModel {
    private final IntegerProperty currentIndex = new SimpleIntegerProperty();
    private final StringProperty nextButtonText = new SimpleStringProperty();
    private final StringProperty backButtonText = new SimpleStringProperty();
    private final BooleanProperty closeButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty nextButtonVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty nextButtonDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty backButtonVisible = new SimpleBooleanProperty(true);
    @Setter
    private boolean amountVisible;
    @Setter
    private boolean settlementVisible;
    private final List<NavigationTarget> childTargets = new ArrayList<>();
    private final ObjectProperty<NavigationTarget> selectedChildTarget = new SimpleObjectProperty<>();
    @Setter
    private boolean animateRightOut = true;

    public TakeOfferModel() {
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.TAKE_OFFER_AMOUNT;
    }

    public void reset() {
        currentIndex.set(0);
        selectedChildTarget.set(childTargets.get(0));
        animateRightOut = true;
    }
}
