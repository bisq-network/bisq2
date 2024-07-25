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

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.CustomMenuItem;
import lombok.Getter;

public class DropdownMenuItem extends CustomMenuItem {
    @Getter
    private final BisqMenuItem bisqMenuItem;

    public DropdownMenuItem(String defaultIconId, String activeIconId, String text) {
        bisqMenuItem = new BisqMenuItem(defaultIconId, activeIconId, text);
        bisqMenuItem.getStyleClass().add("dropdown-menu-item-content");
        setContent(bisqMenuItem);

        // Using DropdownMenuItem.setOnAction leads to duplicate execution of the handler on MacOS 14.5 / M3.
        // Might be a bug in the MacOS specific JavaFX libraries as other devs could not reproduce the issue.
        // Using an eventFilter and consuming the event solves the issue.
        // This happens only when bisqMenuItem is used inside the DropdownMenuItem
        bisqMenuItem.addEventFilter(ActionEvent.ANY, Event::consume);
    }

    public DropdownMenuItem(String text) {
        this(null, null, text);
    }

    public DropdownMenuItem(String defaultIconId, String activeIconId) {
        this(defaultIconId, activeIconId, "");
    }

    public void setLabelText(String text) {
        bisqMenuItem.setText(text);
    }

    public Double getWidth() {
        return bisqMenuItem.getWidth();
    }

    public void updateWidth(Double width) {
        bisqMenuItem.setPrefWidth(width);
    }

    public String getLabelText() {
        return bisqMenuItem.getText();
    }
}
