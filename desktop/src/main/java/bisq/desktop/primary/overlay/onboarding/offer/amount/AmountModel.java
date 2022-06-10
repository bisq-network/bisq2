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

package bisq.desktop.primary.overlay.onboarding.offer.amount;

import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.view.Model;
import javafx.beans.property.*;
import lombok.Getter;

@Getter
public class AmountModel implements Model {
    private final ObjectProperty<Monetary> baseSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> quoteSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Quote> fixPrice = new SimpleObjectProperty<>();
    private final StringProperty direction = new SimpleStringProperty();
    private final ObjectProperty<Monetary> minAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> maxAmount = new SimpleObjectProperty<>();
    private final DoubleProperty sliderMin = new SimpleDoubleProperty();
    private final DoubleProperty sliderMax = new SimpleDoubleProperty();
    private final DoubleProperty sliderValue = new SimpleDoubleProperty();
    private final BooleanProperty sliderFocus = new SimpleBooleanProperty();

    AmountModel() {
    }
}