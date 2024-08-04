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

import bisq.user.RepublishUserProfileService;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class LivenessIndicator extends ImageView {
    private static final long RECENTLY_ACTIVE_TIME_SPAN = TimeUnit.HOURS.toMillis(1);
    private static final long ACTIVE_TIME_SPAN = RepublishUserProfileService.MIN_PAUSE_TO_NEXT_REPUBLISH * 2;

    @Nullable
    @Getter
    @Setter
    private String lastLivenessSignalAsString;
    private long lastLivenessSignal;
    private double size;

    public LivenessIndicator() {
    }

    public void setLastLivenessSignal(long lastLivenessSignal) {
        this.lastLivenessSignal = lastLivenessSignal;
        update();
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }

    public void setSize(double size) {
        this.size = size;
        update();
    }

    void update() {
        String color;
        if (wasActive()) {
            color = "green";
        } else if (wasRecentlyActive()) {
            color = "yellow";
        } else {
            color = "grey";
        }
        String sizePostFix = size < 60 ? "-small-dot" : "-dot";
        String id = color + sizePostFix;
        setId(id);
    }

    private boolean wasActive() {
        return lastLivenessSignal > 0 && lastLivenessSignal < ACTIVE_TIME_SPAN;
    }

    private boolean wasRecentlyActive() {
        return lastLivenessSignal > 0 && lastLivenessSignal < RECENTLY_ACTIVE_TIME_SPAN;
    }

    public void dispose() {

    }
}
