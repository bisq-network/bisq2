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

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.user.profile_card.ProfileCardController;
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

    private final UserProfileIcon userProfileIcon;
    private final ReputationScoreDisplay reputationScoreDisplay;
    @Getter
    private final Label userName;

    public UserProfileDisplay() {
        this(null, DEFAULT_ICON_SIZE, null, true);
    }

    public UserProfileDisplay(double size) {
        this(size, true);
    }

    public UserProfileDisplay(double size, boolean openProfileCardOnClick) {
        this(null, size, null, openProfileCardOnClick);
    }

    public UserProfileDisplay(@Nullable UserProfile userProfile) {
        this(userProfile, true);
    }

    public UserProfileDisplay(@Nullable UserProfile userProfile, boolean openProfileCardOnClick) {
        this(userProfile, DEFAULT_ICON_SIZE, null, openProfileCardOnClick);
    }

    public UserProfileDisplay(@Nullable UserProfile userProfile, @Nullable ReputationScore reputationScore) {
        this(userProfile, reputationScore, true);
    }

    public UserProfileDisplay(@Nullable UserProfile userProfile,
                              @Nullable ReputationScore reputationScore,
                              boolean openProfileCardOnClick) {
        this(userProfile, DEFAULT_ICON_SIZE, reputationScore, openProfileCardOnClick);
    }

    public UserProfileDisplay(@Nullable UserProfile userProfile,
                              double size,
                              @Nullable ReputationScore reputationScore,
                              boolean openProfileCardOnClick) {
        super(10);

        getStyleClass().add("user-profile-display");
        setAlignment(Pos.CENTER_LEFT);

        userProfileIcon = new UserProfileIcon(size);
        userName = new Label();
        userName.getStyleClass().add("user-profile-display-name");
        reputationScoreDisplay = new ReputationScoreDisplay();
        reputationScoreDisplay.setScale(0.75);
        VBox vBox = new VBox(userName, reputationScoreDisplay);
        vBox.getStyleClass().add("username-and-reputation-box");
        vBox.setFillWidth(true);
        vBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(vBox, Priority.ALWAYS);
        getChildren().addAll(userProfileIcon, vBox);

        if (userProfile != null) {
            setUserProfile(userProfile, openProfileCardOnClick);
        }

        if (reputationScore != null) {
            setReputationScore(reputationScore);
        }
    }

    public void setUserProfile(@Nullable UserProfile userProfile) {
        setUserProfile(userProfile, true);
    }

    public void setUserProfile(@Nullable UserProfile userProfile, boolean openProfileCardOnClick) {
        userProfileIcon.setUserProfile(userProfile, openProfileCardOnClick);
        applyTooltip();
        if (userProfile != null) {
            userName.setText(userProfile.getUserName());
        }
        if (openProfileCardOnClick) {
            userName.setOnMouseClicked(e ->
                    Navigation.navigateTo(NavigationTarget.PROFILE_CARD, new ProfileCardController.InitData(userProfile)));
            userName.getStyleClass().add("hand-cursor");
        }
    }

    public void dispose() {
        userName.setOnMouseClicked(null);
        userProfileIcon.dispose();
        setReputationScore(null);
    }

    private void applyTooltip() {
        userProfileIcon.getTooltip().setText(getTooltipText());
    }

    public String getTooltipText() {
        String reputationTooltip = reputationScoreDisplay != null ?
                "\n" + reputationScoreDisplay.getTooltipString() :
                "";
        return userProfileIcon.getTooltipText() + reputationTooltip;
    }

    public void setReputationScore(@Nullable ReputationScore reputationScore) {
        reputationScoreDisplay.setReputationScore(reputationScore);
        applyTooltip();
    }

    public Tooltip getTooltip() {
        return userProfileIcon.getTooltip();
    }

    public void setReputationScoreDisplayScale(double scale) {
        reputationScoreDisplay.setScale(scale);
    }
}
