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

package bisq.desktop.main.content.bisq_easy;

import bisq.common.data.Triple;
import bisq.desktop.common.Layout;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BisqEasyViewUtils {
    public static Triple<Label, HBox, VBox> getContainer(String headline, Node content) {
        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().add("bisq-easy-container-headline");
        HBox header = new HBox(10, headlineLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 30, 15, 30));
        header.getStyleClass().add("bisq-easy-container-header");

        VBox.setMargin(content, new Insets(0, 30, 15, 30));
        VBox vBox = new VBox(header, Layout.hLine(), content);
        vBox.setFillWidth(true);
        vBox.getStyleClass().add("bisq-easy-container");

        return new Triple<>(headlineLabel, header, vBox);
    }
}