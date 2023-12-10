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

import javafx.scene.control.Tooltip;
import javafx.util.Duration;

// Setting show delay by css does not work
public class BisqTooltip extends Tooltip {
    public BisqTooltip() {
        this(null, false);
    }

    public BisqTooltip(String text) {
        this(text, false);
    }

    public BisqTooltip(boolean useDarkStyle) {
        this(null, useDarkStyle);
    }

    public BisqTooltip(String text, boolean useDarkStyle) {
        super(text);
        setShowDelay(Duration.millis(100));
        setHideDelay(Duration.millis(100));
        useDarkStyle(useDarkStyle);
    }

    public void useDarkStyle(boolean useDarkStyle) {
        if (useDarkStyle) {
            getStyleClass().add("dark-tooltip");
        }
    }
}