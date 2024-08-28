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
    public enum Style {
        DEFAULT,
        MEDIUM_DARK,
        DARK
    }

    public BisqTooltip() {
        this(null, Style.DEFAULT);
    }

    public BisqTooltip(String text) {
        this(text, Style.DEFAULT);
    }

    public BisqTooltip(Style style) {
        this(null, style);
    }

    public BisqTooltip(String text, Style style) {
        super(text);
        setShowDelay(Duration.millis(100));
        setHideDelay(Duration.millis(100));
        useStyle(style);

        setMaxWidth(800);
        setWrapText(true);
    }

    public void useStyle(Style style) {
        switch (style) {
            case DEFAULT -> {
                // Force font color as color from css gets shadowed by parent
                setStyle("-fx-text-fill: -fx-dark-text-color !important;");
            }
            case MEDIUM_DARK -> {
                getStyleClass().add("medium-dark-tooltip");
                // Force font color as color from css gets shadowed by parent
                setStyle("-fx-text-fill: -bisq-light-grey-10 !important;");
            }
            case DARK -> {
                getStyleClass().add("dark-tooltip");
                // Force font color as color from css gets shadowed by parent
                setStyle("-fx-text-fill: -fx-light-text-color !important;");
            }
        }
    }
}