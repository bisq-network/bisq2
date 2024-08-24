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

package bisq.desktop.main.content.settings;

import bisq.desktop.common.Layout;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class SettingsViewUtils {
    public static Label getHeadline(String text) {
        Label headline = new Label(text);
        headline.getStyleClass().add("large-thin-headline");
        return headline;
    }

    public static Region getLineAfterHeadline(double spacing) {
        Region line = Layout.hLine();
        VBox.setMargin(line, new Insets(7.5 - spacing, 0, 20 - spacing, 0));
        return line;
    }
}
