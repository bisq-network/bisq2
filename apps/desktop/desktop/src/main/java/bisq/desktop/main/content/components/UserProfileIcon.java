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
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.UUID;

import static bisq.desktop.main.content.components.UserProfileDisplay.DEFAULT_ICON_SIZE;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Slf4j
public class UserProfileIcon extends StackPane implements LivenessScheduler.FormattedAgeConsumer {
    // As we are used in a hashSet we want to be sure to have a controlled EqualsAndHashCode
    @EqualsAndHashCode.Include
    private final String id = UUID.randomUUID().toString();

    private final LivenessIndicator livenessIndicator = new LivenessIndicator();
    @Getter
    private final BisqTooltip tooltip = new BisqTooltip();
    private final ImageView userProfileIcon = new ImageView();
    @Nullable
    private UserProfile userProfile;
    @Getter
    private String tooltipText = "";
    private String userProfileInfo = "";
    private String lastSeen = "";
    private String versionInfo = "";
    private final LivenessScheduler livenessScheduler;
    private final ChangeListener<Scene> sceneChangeListener;

    public UserProfileIcon() {
        this(DEFAULT_ICON_SIZE);
    }

    public UserProfileIcon(double size) {
        livenessScheduler = new LivenessScheduler(livenessIndicator, this);
        setSize(size);

        tooltip.getStyleClass().add("medium-dark-tooltip");
        setAlignment(Pos.CENTER);
        getChildren().addAll(userProfileIcon, livenessIndicator);
        sceneChangeListener = (ov, oldValue, newScene) -> handleSceneChange(oldValue, newScene);
    }

    private void handleSceneChange(Scene oldValue, Scene newScene) {
        if (oldValue == null && newScene != null) {
            livenessScheduler.start(userProfile);
        } else if (oldValue != null && newScene == null) {
            dispose();
            sceneProperty().removeListener(sceneChangeListener);
        }
    }

    public void setUserProfile(@Nullable UserProfile userProfile) {
        this.userProfile = userProfile;

        if (userProfile == null) {
            dispose();
            return;
        }

        // Is cached in CatHash
        userProfileIcon.setImage(CatHash.getImage(userProfile));

        userProfileInfo = userProfile.getTooltipString();
        String version = userProfile.getApplicationVersion();
        if (version.isEmpty()) {
            version = Res.get("data.na");
        }
        versionInfo = Res.get("user.userProfile.version", version);
        updateTooltipText();

        Tooltip.install(this, tooltip);

        if (getScene() == null) {
            sceneProperty().addListener(sceneChangeListener);
        } else {
            livenessScheduler.start(userProfile);
        }
    }

    public void dispose() {
        livenessScheduler.dispose();
        userProfileIcon.setImage(null);
        userProfile = null;
        Tooltip.uninstall(this, tooltip);
    }

    @Override
    public void setFormattedAge(String formattedAge) {
        if (formattedAge != null) {
            lastSeen = "\n" + Res.get("user.userProfile.lastSeenAgo", formattedAge) + "\n";
        }
        updateTooltipText();
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
        livenessScheduler.disable();
    }

    private void updateTooltipText() {
        tooltipText = userProfileInfo + lastSeen + versionInfo;
        tooltip.setText(tooltipText);
    }
}