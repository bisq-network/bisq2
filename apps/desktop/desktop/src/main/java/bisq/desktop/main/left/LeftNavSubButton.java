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

package bisq.desktop.main.left;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

@Slf4j
class LeftNavSubButton extends LeftNavButton {
    private final static double LABEL_X_POS_COLLAPSED = 31;
    static final int HEIGHT = 28;

    private final String fullTitle;
    @Getter
    private final LeftNavButton parentButton;

    LeftNavSubButton(String title, ToggleGroup toggleGroup, NavigationTarget navigationTarget, LeftNavButton parentButton) {
        super(title, null, toggleGroup, navigationTarget, false, null);
        this.fullTitle = title;
        this.parentButton = parentButton;
        EasyBind.subscribe(parentButton.getIsSubMenuExpanded(), parentSelected -> {
            setVisible(parentSelected);
            setManaged(parentSelected);
        });
    }

    @Override
    protected void applyStyle() {
        Layout.chooseStyleClass(buttonLabel, "bisq-text-green", "bisq-text-grey-9", isSelected());
    }

    @Override
    public void setHorizontalExpanded(boolean menuExpanded, int duration) {
        if (menuExpanded) {
            Tooltip.uninstall(this, tooltip);
            Transitions.animateLeftSubNavigation(buttonLabel, LABEL_X_POS_EXPANDED, duration);
            buttonLabel.setText(fullTitle);
        } else {
            Tooltip.install(this, tooltip);
            Transitions.animateLeftSubNavigation(buttonLabel, LABEL_X_POS_COLLAPSED, duration);
            buttonLabel.setText(fullTitle.substring(0, 1));
        }
    }

    @Override
    protected int calculateHeight() {
        return HEIGHT;
    }

    @Override
    protected void updateLayoutYNumMessagesBadge() {
        numMessagesBadge.setLayoutY((LeftNavSubButton.HEIGHT - 16) / 2d);
    }
}