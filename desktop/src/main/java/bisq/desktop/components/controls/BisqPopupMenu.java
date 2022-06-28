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

import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;

public class BisqPopupMenu extends BisqPopup {
    public BisqPopupMenu(List<BisqPopupMenuItem> items, Runnable onClose) {
        super();
        getStyleClass().add("bisq-popup-menu");

        VBox box = new VBox();
        box.setSpacing(5);

        for (BisqPopupMenuItem item : items) {
            Button button = new Button(item.getTitle());
            button.getStyleClass().add("bisq-popup-menu-item");
            button.setMaxWidth(Double.MAX_VALUE);

            button.setOnAction(evt -> {
                item.getAction().run();
                hide();
            });

            box.getChildren().add(button);
        }

        setContentNode(box);

        showingProperty().addListener((observable, wasShowing, isShowing) -> {
            if (wasShowing) {
                onClose.run();
            }
        });
    }
}
