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

package bisq.desktop.main.content.mu_sig.offer.listing;

import bisq.desktop.components.controls.DropdownMenuItem;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import lombok.Getter;

@Getter
final class MarketsTypeMenuItem extends DropdownMenuItem {
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

    private final MarketType marketType;

    MarketsTypeMenuItem(MarketType marketType, Label displayLabel) {
        super("check-white", "check-white", displayLabel);

        this.marketType = marketType;
        getStyleClass().add("dropdown-menu-item");
        updateSelection(false);
    }

    public void dispose() {
        setOnAction(null);
    }

    void updateSelection(boolean isSelected) {
        getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
    }

    boolean isSelected() {
        return getContent().getPseudoClassStates().contains(SELECTED_PSEUDO_CLASS);
    }
}
