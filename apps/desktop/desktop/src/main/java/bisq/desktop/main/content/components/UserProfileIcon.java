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

import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static bisq.desktop.main.content.components.UserProfileDisplay.DEFAULT_ICON_SIZE;

@Slf4j
public class UserProfileIcon extends StackPane implements LivenessScheduler.FormattedAgeConsumer {
    private final LivenessIndicator livenessIndicator = new LivenessIndicator();
    @Getter
    private final BisqTooltip tooltip = new BisqTooltip();
    @Getter
    private final StringProperty formattedAge = new SimpleStringProperty("");
    private final ImageView catHashImageView = new ImageView();
    @Nullable
    private UserProfile userProfile;
    @Getter
    private String tooltipText = "";
    private String userProfileInfo = "";
    private String livenessState = "";
    private String versionInfo = "";
    private final LivenessScheduler livenessScheduler;
    private final ChangeListener<Scene> sceneChangeListener;
    private double size;

    public UserProfileIcon() {
        this(DEFAULT_ICON_SIZE);
    }

    public UserProfileIcon(double size) {
        livenessScheduler = new LivenessScheduler(livenessIndicator, this);
        setSize(size);

        setAlignment(Pos.CENTER);
        getChildren().addAll(catHashImageView, livenessIndicator);
        sceneChangeListener = (ov, oldValue, newScene) -> handleSceneChange(oldValue, newScene);
    }

    private void handleSceneChange(Scene oldValue, Scene newScene) {
        if (oldValue == null && newScene != null) {
            livenessScheduler.start(userProfile);
        } else if (oldValue != null && newScene == null) {
            UIThread.runOnNextRenderFrame(() -> {
                if (getScene() == null) {
                    dispose();
                    sceneProperty().removeListener(sceneChangeListener);
                }
            });
        }
    }

    public void setUserProfile(@Nullable UserProfile userProfile) {
        this.userProfile = userProfile;

        if (userProfile == null) {
            dispose();
            return;
        }

        // Is cached in CatHash
        catHashImageView.setImage(CatHash.getImage(userProfile, size));

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
        catHashImageView.setImage(null);
        userProfile = null;
        Tooltip.uninstall(this, tooltip);
    }

    @Override
    public void setFormattedAge(String formattedAge) {
        this.formattedAge.set(StringUtils.isEmpty(formattedAge) ? Res.get("data.na") : formattedAge);
        if (formattedAge != null) {
            livenessState = "\n" + Res.get("user.userProfile.livenessState", formattedAge) + "\n";
        }
        updateTooltipText();
    }

    public void setSize(double size) {
        this.size = size;
        livenessIndicator.setSize(size);
        catHashImageView.setFitWidth(size);
        catHashImageView.setFitHeight(size);

        // We want to keep it centered, so we apply it to both sides with inverted numbers
        double adjustment = size * 0.9;
        double right = -adjustment / 2;
        double bottom = -adjustment / 2;
        double top = adjustment / 2;
        double left = adjustment / 2;
        StackPane.setMargin(livenessIndicator, new Insets(top, right, bottom, left));
    }

    public void setUseSecondTick(boolean useSecondTick) {
        livenessScheduler.setUseSecondTick(useSecondTick);
    }

    public void hideLivenessIndicator() {
        livenessIndicator.hide();
        livenessScheduler.disable();
    }

    private void updateTooltipText() {
        tooltipText = userProfileInfo + livenessState + versionInfo;
        tooltip.setText(tooltipText);
    }
}