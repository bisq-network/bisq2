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
import bisq.desktop.components.robohash.RoboHash;
import bisq.user.profile.UserProfile;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProfileIcon extends ImageView {
    public UserProfileIcon(double size) {
        setSize(size);
    }

    public void setUserProfile(UserProfile userProfile) {
        BisqTooltip tooltip = new BisqTooltip(userProfile.getTooltipString());
        tooltip.getStyleClass().add("medium-dark-tooltip");
        Tooltip.install(this, tooltip);
        setImage(RoboHash.getImage(userProfile.getPubKeyHash()));
    }

    public void releaseResources() {
        setImage(null);
    }

    public void setSize(double size) {
        setFitWidth(size);
        setFitHeight(size);
    }
}