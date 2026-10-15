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

import bisq.desktop.components.controls.DropdownBisqMenuItem;
import javafx.css.PseudoClass;
import lombok.Getter;

@Getter
final class SortAndFilterDropdownMenuItem<T> extends DropdownBisqMenuItem {
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

    private final T menuItem;

    SortAndFilterDropdownMenuItem(String defaultIconId, String activeIconId, String text, T menuItem) {
        super(defaultIconId, activeIconId, text);

        this.menuItem = menuItem;
        getStyleClass().add("dropdown-menu-item");
        updateSelection(false);
    }

    void updateSelection(boolean isSelected) {
        getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
    }
}
