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

import bisq.desktop.common.utils.ImageUtil;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import lombok.Getter;

public class BisqIconButton extends Button {
    @Getter
    private final ImageView icon;

    public BisqIconButton() {
        super();

        icon = new ImageView();
        setGraphic(icon);
    }

    public BisqIconButton(String iconId) {
        this();
        icon.setId(iconId);
    }

    public static Button createIconButton(AwesomeIcon icon) {
        Button button = AwesomeDude.createIconButton(icon);
        // todo none of the below works ;-(
        // with setFocusTraversable we dont get the ugly focus, though we might want to leave it to true
        // maybe its from the label inside the button...
         button.getStyleClass().add("icon-button");
        // button.setStyle("-fx-focus-color: transparent");
        // button.setStyle("-jfx-focus-color: transparent");
        return button;
    }

    public static Button createIconButton(String iconId) {
        Button button = new Button();
        button.setGraphic(ImageUtil.getImageViewById(iconId));
        button.getStyleClass().add("icon-button");
        return button;
    }
}
