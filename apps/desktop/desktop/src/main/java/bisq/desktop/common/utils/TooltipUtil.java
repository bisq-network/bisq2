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

package bisq.desktop.common.utils;

import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TooltipUtil {

    // Only works if label has not set preferredWidth, minWidth or maxWidth and if label layout is already done.
    public static void showTooltipIfTruncated(Labeled label) {
        Text text = new Text(label.getText());
        text.setFont(label.getFont());
        text.getStyleClass().addAll(label.getStyleClass());
        double textWidth = text.getLayoutBounds().getWidth();
        double labelWidth = label.getWidth();
        if (textWidth > labelWidth) {
            if (label.getTooltip() == null) {
                label.setTooltip(new Tooltip(label.getText()));
            }
        } else {
            label.setTooltip(null);
        }
    }
}
