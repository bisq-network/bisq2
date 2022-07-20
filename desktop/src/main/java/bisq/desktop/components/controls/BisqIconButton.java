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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import lombok.Getter;

import javax.annotation.Nullable;

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
        button.getStyleClass().add("icon-button");
        return button;
    }


    public static Button createIconButton(AwesomeIcon icon, String fontSize) {
        Label label = AwesomeDude.createIconLabel(icon, fontSize);
        Button button = new Button();
        button.setGraphic(label);
        button.getStyleClass().add("icon-button");
        return button;
    }

    public static Button createIconButton(String iconId) {
        return createIconButton(iconId, null);
    }

    public static Button createIconButton(String iconId, @Nullable String tooltip) {
        Button button = new Button();
        button.setGraphic(ImageUtil.getImageViewById(iconId));
        button.getStyleClass().add("icon-button");
        if (tooltip != null) {
            button.setTooltip(new Tooltip(tooltip));
        }
        return button;
    }
}
