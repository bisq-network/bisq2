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

import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import org.fxmisc.richtext.StyleClassedTextArea;

public class BisqTaggableTextArea extends StyleClassedTextArea {
    @Setter
    @Getter
    private double heightCorrection = 0;

    public BisqTaggableTextArea() {
        setWrapText(true);
        setBackground(null);
        setEditable(false);
    }

    public void setText(String text) {
        clear();
        replaceText(0, 0, text);
    }

    @Override
    protected double computePrefHeight(double width) {
        if (isAutoHeight()) {
            if (getWidth() == 0.0) {
                Platform.runLater(this::requestLayout);
            } else {
                // The height calculation is about 8.5 px off in the chat messages
                // If corrects itself when clicking into the text field but no requestLayout calls or the like fixed it.
                // So we apply a manual fix
                return super.computePrefHeight(width) + heightCorrection;
            }
        }
        return super.computePrefHeight(width);
    }
}