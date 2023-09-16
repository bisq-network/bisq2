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
import bisq.user.reputation.ReputationScore;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class UserProfileDisplay extends HBox {
    public static final double DEFAULT_ICON_SIZE = 30;
    private final BisqTooltip tooltip;
    private final UserProfileIcon userProfileIcon;
    private final ReputationScoreDisplay reputationScoreDisplay;
    @Getter
    private final Label userName;
    private UserProfile userProfile;

    public UserProfileDisplay() {
        this(null, DEFAULT_ICON_SIZE);
    }

    public UserProfileDisplay(double size) {
        this(null, size);
    }

    public UserProfileDisplay(@Nullable UserProfile userProfile) {
        this(userProfile, DEFAULT_ICON_SIZE);
    }

    public UserProfileDisplay(@Nullable UserProfile userProfile, double size) {
        super(10);
        setAlignment(Pos.CENTER_LEFT);

        userProfileIcon = new UserProfileIcon(size);
        userName = new Label();
        userName.getStyleClass().add("user-profile-display");
        reputationScoreDisplay = new ReputationScoreDisplay();
        reputationScoreDisplay.setScale(0.75);
        VBox vBox = new VBox(userName, reputationScoreDisplay);
        vBox.setFillWidth(true);
        vBox.setAlignment(Pos.CENTER_LEFT);
        
        HBox.setHgrow(vBox, Priority.ALWAYS);
        getChildren().addAll(userProfileIcon, vBox);

        tooltip = new BisqTooltip();
        tooltip.getStyleClass().add("medium-dark-tooltip");
        Tooltip.install(this, tooltip);

        if (userProfile != null) {
            setUserProfile(userProfile);
        }
    }

    public void setUserProfile(@Nullable UserProfile userProfile) {
        this.userProfile = userProfile;
        userName.setText(userProfile != null ? userProfile.getUserName() : null);
        userProfileIcon.setUserProfile(userProfile);
        applyTooltip();
    }

    private void applyTooltip() {
        String userProfileTooltip = userProfile != null
                ? userProfile.getTooltipString() :
                "";
        String reputationTooltip = reputationScoreDisplay != null ?
                "\n" + reputationScoreDisplay.getTooltipString() :
                "";
        tooltip.setText(userProfileTooltip + reputationTooltip);
    }

    public void setReputationScoreScale(double scale) {
        reputationScoreDisplay.setScale(scale);
    }

    public void setIconSize(double size) {
        userProfileIcon.setFitWidth(size);
        userProfileIcon.setFitHeight(size);
    }

    public void setReputationScore(@Nullable ReputationScore reputationScore) {
        reputationScoreDisplay.setReputationScore(reputationScore);
        applyTooltip();
    }
}