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
import java.util.concurrent.TimeUnit;

import static bisq.desktop.main.content.components.UserProfileDisplay.DEFAULT_ICON_SIZE;

@Slf4j
public class UserProfileIcon extends StackPane {
    @Getter
    private final BisqTooltip tooltip = new BisqTooltip();
    @Nullable
    private String lastSeenAsString;
    @Nullable
    @Getter
    private String tooltipText;
    @Nullable
    private UserProfile userProfile;
    private final ImageView userProfileIcon = new ImageView();
    private final ImageView lastSeenDot = new ImageView();
    private double size;
    private long lastSeen;

    public UserProfileIcon() {
        this(DEFAULT_ICON_SIZE);
    }

    public UserProfileIcon(double size) {
        setAlignment(Pos.CENTER);
        getChildren().addAll(userProfileIcon, lastSeenDot);
        setSize(size);
    }

    public void applyData(@Nullable UserProfile userProfile, @Nullable String lastSeenAsString, long lastSeen) {
        this.lastSeenAsString = lastSeenAsString;
        setLastSeen(lastSeen);
        setUserProfile(userProfile);
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        updateLastSeenDot();
    }

    public void setLastSeenAsString(@Nullable String lastSeenAsString) {
        this.lastSeenAsString = lastSeenAsString;
        applyTooltipText();
    }

    public void setUserProfile(@Nullable UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) {
            applyTooltipText();
            tooltip.getStyleClass().add("medium-dark-tooltip");
            Tooltip.install(this, tooltip);
            userProfileIcon.setImage(CatHash.getImage(userProfile));
        } else {
            releaseResources();
        }
    }

    public void releaseResources() {
        userProfileIcon.setImage(null);
        if (tooltip != null) {
            Tooltip.uninstall(this, tooltip);
        }
    }

    public void setSize(double size) {
        this.size = size;
        userProfileIcon.setFitWidth(size);
        userProfileIcon.setFitHeight(size);
        updateLastSeenDot();

        // We want to keep it centered, so we apply it to both sides with inverted numbers
        double adjustMent = size * 0.9;
        double right = -adjustMent / 2;
        double bottom = -adjustMent / 2;
        double top = adjustMent / 2;
        double left = adjustMent / 2;
        StackPane.setMargin(lastSeenDot, new Insets(top, right, bottom, left));
    }

    private void updateLastSeenDot() {
        boolean wasSeenRecently = lastSeen > 0 && lastSeen < TimeUnit.HOURS.toMillis(6);
        String color;
        if (wasSeenRecently) {
            boolean wasSeenMostRecently = lastSeen < TimeUnit.HOURS.toMillis(3);
            if (wasSeenMostRecently) {
                color = "green";
            } else {
                color = "yellow";
            }
        } else {
            color = "grey";
        }
        String sizePostFix = size < 60 ? "-small-dot" : "-dot";
        String id = color + sizePostFix;
        lastSeenDot.setId(id);
    }

    private void applyTooltipText() {
        if (userProfile != null && tooltip != null) {
            String tooltipString = userProfile.getTooltipString();
            String lastSeenString = lastSeenAsString != null ? "\n" + Res.get("user.userProfile.lastSeenAgo", lastSeenAsString) : "";
            tooltipText = tooltipString + lastSeenString;
            tooltip.setText(tooltipText);
        }
    }
}