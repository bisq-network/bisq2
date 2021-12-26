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

package network.misq.desktop.common.utils;

import javafx.scene.control.Labeled;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;

public class TooltipUtil {

    public static void showTooltipIfTruncated(SkinBase skinBase, Labeled labeled) {
        for (Object node : skinBase.getChildren()) {
            if (node instanceof Text) {
                String displayedText = ((Text) node).getText();
                String unTruncatedText = labeled.getText();
                if (displayedText.equals(unTruncatedText)) {
                    if (labeled.getTooltip() != null) {
                        labeled.setTooltip(null);
                    }
                } else if (unTruncatedText != null && !unTruncatedText.trim().isEmpty()) {
                    final Tooltip tooltip = new Tooltip(unTruncatedText);

                    // Force tooltip to use color, as it takes in some cases the color of the parent label
                    // and can't be overridden by class or nodeId
                    tooltip.setStyle("-fx-text-fill: -bs-rd-tooltip-truncated;");
                    labeled.setTooltip(tooltip);
                }
            }
        }
    }
}
