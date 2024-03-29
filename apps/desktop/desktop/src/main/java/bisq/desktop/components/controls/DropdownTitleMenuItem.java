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

package bisq.desktop.components.controls;

import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;

public class DropdownTitleMenuItem extends CustomMenuItem {
    private final Label label;

    public DropdownTitleMenuItem(String text) {
        label = new Label(text);
        setContent(label);
        getStyleClass().add("dropdown-title-menu-item");
    }

    public String getLabelText() {
        return label.getText();
    }
}
