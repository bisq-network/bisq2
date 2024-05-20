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

import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static bisq.desktop.main.content.components.UserProfileDisplay.DEFAULT_ICON_SIZE;

@Slf4j
public class UserProfileIcon extends ImageView {
    @Getter
    private BisqTooltip tooltip;
    @Nullable
    private String lastSeen;
    @Nullable
    @Getter
    private String tooltipText;
    @Nullable
    private UserProfile userProfile;

    public UserProfileIcon() {
        this(DEFAULT_ICON_SIZE);
    }

    public UserProfileIcon(double size) {
        setSize(size);
    }

    public void setLastSeen(@Nullable String lastSeen) {
        this.lastSeen = lastSeen;
        applyTooltipText();
    }

    public void setUserProfile(@Nullable UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) {
            tooltip = new BisqTooltip();
            applyTooltipText();
            tooltip.getStyleClass().add("medium-dark-tooltip");
            Tooltip.install(this, tooltip);
            setImage(CatHash.getImage(userProfile));
        } else {
            setImage(null);
            if (tooltip != null) {
                Tooltip.uninstall(this, tooltip);
            }
        }
    }

    public void releaseResources() {
        setImage(null);
    }

    public void setSize(double size) {
        setFitWidth(size);
        setFitHeight(size);
    }

    private void applyTooltipText() {
        if (userProfile != null && tooltip != null) {
            String tooltipString = userProfile.getTooltipString();
            String lastSeenString = lastSeen != null ? "\n" + Res.get("user.userProfile.lastSeenAgo", lastSeen) : "";
            tooltipText = tooltipString + lastSeenString;
            tooltip.setText(tooltipText);
        }
    }
}