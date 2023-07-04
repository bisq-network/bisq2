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

package bisq.desktop.components.containers;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class SectionBox extends VBox {
    public SectionBox(String headline) {
        this();
        Pane pane = getHeadline(headline);
        VBox.setMargin(pane, new Insets(0, -20, -20, -20));
        getChildren().add(pane);
    }

    public SectionBox() {
        setSpacing(30);
        setMinWidth(560);
        setPadding(new Insets(20, 20, 20, 20));
        setStyle("-fx-background-color: #181818; -fx-background-radius: 10");
    }

    public static Pane getHeadline(String headline) {
        Label label = new Label(headline);
        label.setStyle("-fx-font-size: 1.5em; -fx-text-fill: #ddd;");
        label.setPadding(new Insets(10, 10, 10, 10));
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: #111;");
        pane.getChildren().add(label);
        return pane;
    }
}