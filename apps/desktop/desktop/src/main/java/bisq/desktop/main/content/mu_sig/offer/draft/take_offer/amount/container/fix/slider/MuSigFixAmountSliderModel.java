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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount.container.fix.slider;

import bisq.desktop.common.view.Model;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter(AccessLevel.PACKAGE)
public class MuSigFixAmountSliderModel implements Model {
    private final DoubleProperty maxAllowedValue = new SimpleDoubleProperty(1);

    // Domain-projected only: the controller writes the domain's slider value here and the view
    // applies it to the thumb. Nothing in the UI layer clamps this value - after a background
    // limit drop the thumb keeps showing the still-selected, now-invalid amount; user gestures
    // are clamped in the controller before they reach the domain.
    private final DoubleProperty getSliderValue = new SimpleDoubleProperty(0);
}