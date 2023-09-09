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

package bisq.desktop.main.content.components;

import bisq.desktop.components.controls.BisqTooltip;
import bisq.user.profile.UserProfile;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProfileDisplay extends Label {
    public static final double DEFAULT_ICON_SIZE = 30;
    private UserProfileIcon userProfileIcon;

    public UserProfileDisplay(UserProfile userProfile) {
        this(userProfile, DEFAULT_ICON_SIZE);
    }

    public UserProfileDisplay(UserProfile userProfile, double size) {
        userProfileIcon = new UserProfileIcon(size);
        userProfileIcon.setUserProfile(userProfile);

        setText(userProfile.getUserName());
        setGraphic(userProfileIcon);
        setGraphicTextGap(10);
        BisqTooltip tooltip = new BisqTooltip(userProfile.getTooltipString());
        tooltip.getStyleClass().add("medium-dark-tooltip");
        Tooltip.install(this, tooltip);
    }

    public void setIconSize(double size) {
        userProfileIcon.setFitWidth(size);
        userProfileIcon.setFitHeight(size);
    }
}