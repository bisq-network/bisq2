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

public class DropdownMenuItem extends CustomMenuItem {
    private final BisqMenuItem menuItem;

    public DropdownMenuItem(String defaultIconId, String activeIconId, String text) {
        menuItem = new BisqMenuItem(defaultIconId, activeIconId, text);
        menuItem.getStyleClass().add("dropdown-menu-item-content");
        setContent(menuItem);
    }

    public DropdownMenuItem(String text) {
        this(null, null, text);
    }

    public DropdownMenuItem(String defaultIconId, String activeIconId) {
        this(defaultIconId, activeIconId, "");
    }

    public void setLabelText(String text) {
        menuItem.setText(text);
    }

    public Double getWidth() {
        return menuItem.getWidth();
    }

    public void updateWidth(Double width) {
        menuItem.setPrefWidth(width);
    }

    public String getLabelText() {
        return menuItem.getText();
    }
}
