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
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

public class BisqMenuItem extends Button {
    public static final String ICON_CSS_STYLE = "menu-item-icon";

    private ImageView defaultIcon, activeIcon, buttonIcon;

    public BisqMenuItem(String defaultIconId, String activeIconId, String text) {
        if (text != null && !text.isEmpty()) {
            setText(text);
            setGraphicTextGap(10);
            setAlignment(Pos.CENTER_LEFT);
        }

        if (defaultIconId != null && activeIconId != null) {
            defaultIcon = ImageUtil.getImageViewById(defaultIconId);
            activeIcon = ImageUtil.getImageViewById(activeIconId);
            defaultIcon.getStyleClass().add(ICON_CSS_STYLE);
            activeIcon.getStyleClass().add(ICON_CSS_STYLE);
            buttonIcon = defaultIcon;
            setGraphic(buttonIcon);
            attachListeners();
        }

        getStyleClass().add("bisq-menu-item");
    }

    public BisqMenuItem(String text) {
        this(null, null, text);
    }

    public BisqMenuItem(String defaultIconId, String activeIconId) {
        this(defaultIconId, activeIconId, null);
    }

    public void useIconOnly() {
        double size = 29;
        setMaxSize(size, size);
        setMinSize(size, size);
        setPrefSize(size, size);
        setAlignment(Pos.CENTER);
        getStyleClass().add("icon-only");
    }

    public void setTooltip(String tooltip) {
        if (tooltip != null) {
            Tooltip.install(this, new BisqTooltip(tooltip));
        }
    }

    private void attachListeners() {
        setOnMouseEntered(e -> updateIcon(activeIcon));
        setOnMouseExited(e -> updateIcon(defaultIcon));
        setOnMouseClicked(e -> updateIcon(defaultIcon));
    }

    private void updateIcon(ImageView newIcon) {
        if (buttonIcon != newIcon) {
            buttonIcon = newIcon;
            setGraphic(buttonIcon);
        }
    }
}
