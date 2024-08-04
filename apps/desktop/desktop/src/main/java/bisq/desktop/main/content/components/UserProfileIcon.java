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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static bisq.desktop.main.content.components.UserProfileDisplay.DEFAULT_ICON_SIZE;

@Slf4j
public class UserProfileIcon extends StackPane {
    @Getter
    private final BisqTooltip tooltip = new BisqTooltip();
    private final LivenessIndicator livenessIndicator;
    @Nullable
    @Getter
    private String tooltipText;
    @Nullable
    private UserProfile userProfile;
    private final ImageView userProfileIcon = new ImageView();

    public UserProfileIcon() {
        this(DEFAULT_ICON_SIZE);
    }

    public UserProfileIcon(double size) {
        livenessIndicator = new LivenessIndicator();

        tooltip.getStyleClass().add("medium-dark-tooltip");
        setAlignment(Pos.CENTER);
        getChildren().addAll(userProfileIcon, livenessIndicator);
        setSize(size);
    }

    public void applyData(@Nullable UserProfile userProfile,
                          @Nullable String lastLivenessSignalAsString,
                          long lastLivenessSignal) {
        livenessIndicator.setLastLivenessSignal(lastLivenessSignal);
        livenessIndicator.setLastLivenessSignalAsString(lastLivenessSignalAsString);
        setUserProfile(userProfile);
    }

    public void setUserProfile(@Nullable UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) {
            Tooltip.install(this, tooltip);
            applyTooltipText();
            userProfileIcon.setImage(CatHash.getImage(userProfile));
        } else {
            dispose();
        }
    }

    public void dispose() {
        livenessIndicator.dispose();

        userProfileIcon.setImage(null);
        userProfile = null;
        if (tooltip != null) {
            Tooltip.uninstall(this, tooltip);
        }
    }

    public void setSize(double size) {
        livenessIndicator.setSize(size);
        userProfileIcon.setFitWidth(size);
        userProfileIcon.setFitHeight(size);

        // We want to keep it centered, so we apply it to both sides with inverted numbers
        double adjustment = size * 0.9;
        double right = -adjustment / 2;
        double bottom = -adjustment / 2;
        double top = adjustment / 2;
        double left = adjustment / 2;
        StackPane.setMargin(livenessIndicator, new Insets(top, right, bottom, left));
    }

    public void hideLivenessIndicator() {
        livenessIndicator.hide();
    }

    private void applyTooltipText() {
        if (userProfile != null && tooltip != null) {
            String tooltipString = userProfile.getTooltipString();
            String lastSeenAsString = livenessIndicator.getLastLivenessSignalAsString();
            String lastSeenString = lastSeenAsString != null ? "\n" + Res.get("user.userProfile.lastSeenAgo", lastSeenAsString) : "";
            String version = userProfile.getApplicationVersion();
            if (version.isEmpty()) {
                version = Res.get("data.na");
            }
            String versionString = lastSeenAsString != null ? "\n" + Res.get("user.userProfile.version", version) : "";
            tooltipText = tooltipString + lastSeenString + versionString;
            tooltip.setText(tooltipText);
        }
    }
}