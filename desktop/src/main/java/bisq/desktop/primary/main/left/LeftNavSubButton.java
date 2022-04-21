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

package bisq.desktop.primary.main.left;

import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationTarget;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class LeftNavSubButton extends LeftNavButton {

    private final String fullTitle;
    private final double labelXPosExpanded;

    LeftNavSubButton(String title, ToggleGroup toggleGroup, NavigationTarget navigationTarget) {
        super(title, null, toggleGroup, navigationTarget);
        this.fullTitle = title;

        label.setLayoutX(81);
        labelXPosExpanded = label.getLayoutX();
        log.error("labelXPosExpanded "+labelXPosExpanded);
    }

    @Override
    protected void applyStyle() {
        if (selectedProperty.get()) {
            getStyleClass().remove("bisq-darkest-bg");
            getStyleClass().add("bisq-dark-bg");
            label.getStyleClass().remove("bisq-sub-nav-label");
            label.getStyleClass().add("bisq-sub-nav-label-selected");
        } else {
            getStyleClass().remove("bisq-dark-bg");
            getStyleClass().add("bisq-darkest-bg");
            label.getStyleClass().remove("bisq-sub-nav-label-selected");
            label.getStyleClass().add("bisq-sub-nav-label");
        }
    }

    @Override
    public void setMenuExpanded(boolean menuExpanded, int duration) {
        if (menuExpanded) {
            Tooltip.uninstall(this, tooltip);
            Transitions.animateLeftSubNavigation(label, labelXPosExpanded, duration);
            label.setText(fullTitle);
        } else {
            Tooltip.install(this, tooltip);
            Transitions.animateLeftSubNavigation(label, 33.5, duration);
            label.setText(fullTitle.substring(0, 1));
        }
    }
}