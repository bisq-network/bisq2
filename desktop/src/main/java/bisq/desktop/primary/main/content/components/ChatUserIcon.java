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

package bisq.desktop.primary.main.content.components;

import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.robohash.RoboHash;
import bisq.user.profile.UserProfile;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatUserIcon extends Pane {
    private final ImageView roboIcon;
    private final Tooltip tooltip;

    public ChatUserIcon(double size) {
        tooltip = new BisqTooltip();
        tooltip.setId("proof-of-burn-tooltip");
        roboIcon = new ImageView();
        roboIcon.setFitWidth(size);
        roboIcon.setFitHeight(size);

        getChildren().add(roboIcon);
    }

    public void setChatUser(UserProfile userProfile) {
        roboIcon.setImage(RoboHash.getImage(userProfile.getPubKeyHash()));
    }

    public void releaseResources() {
        roboIcon.setImage(null);
    }
}