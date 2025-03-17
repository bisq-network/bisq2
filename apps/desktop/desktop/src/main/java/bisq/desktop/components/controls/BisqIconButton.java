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

import bisq.common.util.StringUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.Getter;

import javax.annotation.Nullable;

public class BisqIconButton extends Button {
    public static Button createCopyIconButton() {
        Button button = AwesomeDude.createIconButton(AwesomeIcon.COPY);
        button.getStyleClass().add("icon-button");
        button.setTooltip(new BisqTooltip(Res.get("action.copyToClipboard"), BisqTooltip.Style.DARK));
        return button;
    }

    public static Button createCopyIconButton(BisqTooltip.Style tooltipStyle, String style) {
        Button button = AwesomeDude.createIconButton(AwesomeIcon.COPY);
        button.getStyleClass().add("icon-button");
        button.getGraphic().getStyleClass().add(style);
        button.setTooltip(new BisqTooltip(Res.get("action.copyToClipboard"), tooltipStyle));
        return button;
    }

    public static Button createExternalLinkButton(@Nullable String text, String tooltip) {
        return createIconButton(AwesomeIcon.EXTERNAL_LINK, text, tooltip);
    }

    public static Button createDeleteIconButton() {
        Button button = AwesomeDude.createIconButton(AwesomeIcon.REMOVE_SIGN);
        button.getStyleClass().add("icon-button");
        button.setTooltip(new BisqTooltip(Res.get("action.delete"), BisqTooltip.Style.DARK));
        return button;
    }

    public static Button createInfoIconButton(String tooltipText) {
        Button button = AwesomeDude.createIconButton(AwesomeIcon.INFO_SIGN);
        button.getStyleClass().add("icon-button");
        button.setTooltip(new BisqTooltip(tooltipText, BisqTooltip.Style.DARK));
        return button;
    }

    public static Button createIconButton(AwesomeIcon icon) {
        return createIconButton(icon, null, null, Double.parseDouble(AwesomeDude.DEFAULT_ICON_SIZE));
    }

    public static Button createIconButton(AwesomeIcon icon, String text, String tooltip) {
        return createIconButton(icon, text, tooltip, Double.parseDouble(AwesomeDude.DEFAULT_ICON_SIZE));
    }

    public static Button createIconButton(AwesomeIcon icon, double iconSize) {
        return createIconButton(icon, null, null, iconSize);
    }

    public static Button createIconButton(AwesomeIcon icon,
                                          @Nullable String text,
                                          @Nullable String tooltip,
                                          double iconSize) {
        Label label = AwesomeDude.createIconLabel(icon, String.valueOf(iconSize));
        Button button = new Button();
        if (tooltip != null) {
            button.setTooltip(new BisqTooltip(tooltip));
        }
        button.setGraphic(label);
        button.getStyleClass().add("icon-button");
        if (StringUtils.isNotEmpty(text)) {
            button.setText(text);
            button.setGraphicTextGap(5);
            button.setContentDisplay(ContentDisplay.RIGHT);
        }
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
            button.setTooltip(new BisqTooltip(tooltip));
        }
        return button;
    }

    public static Button createIconButton(ImageView imageView) {
        return createIconButton(imageView, null);
    }

    public static Button createIconButton(ImageView imageView, @Nullable String tooltip) {
        Button button = new Button();
        button.setGraphic(imageView);
        button.getStyleClass().add("icon-button");
        if (tooltip != null) {
            button.setTooltip(new BisqTooltip(tooltip));
        }
        return button;
    }

    @Getter
    private final ImageView icon;

    public BisqIconButton() {
        super();

        icon = new ImageView();
        setGraphic(icon);
        getStyleClass().add("icon-button");
    }

    public BisqIconButton(String iconId) {
        this();
        icon.setId(iconId);
    }


    public void setIcon(AwesomeIcon icon) {
        setGraphic(AwesomeDude.createIconLabel(icon));
    }

    public void setIcon(ImageView imageView) {
        setGraphic(imageView);
    }

    public void setIcon(Node node) {
        setGraphic(node);
    }

    public void setIcon(AwesomeIcon icon, String iconSize) {
        setGraphic(AwesomeDude.createIconLabel(icon, iconSize));
    }

    public void setIcon(String iconId) {
        setGraphic(ImageUtil.getImageViewById(iconId));
    }
}
