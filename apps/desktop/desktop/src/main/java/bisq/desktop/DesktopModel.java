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

package bisq.desktop;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.common.view.NavigationModel;
import javafx.geometry.Rectangle2D;
import lombok.Getter;
import lombok.Setter;

@Getter
public class DesktopModel extends NavigationModel {
    // Min supported screens: 1024x768
    public static final double MIN_WIDTH = 1000;
    public static final double MIN_HEIGHT = 730;
    public static final double PREF_WIDTH = 1400;
    public static final double PREF_HEIGHT = 950;

    private final String title;
    @Setter
    private double stageX;
    @Setter
    private double stageY;
    @Setter
    private double stageWidth;
    @Setter
    private double stageHeight;
    @Setter
    private boolean isInitializeApplicationServiceCompleted;
    @Setter
    private boolean isUnlocked;

    public DesktopModel(String title) {
        this.title = title;
    }

    // The size of the main window when no size is saved
    public static double defaultStageWidth(Rectangle2D screenBounds) {
        return Math.max(MIN_WIDTH, Math.min(PREF_WIDTH, screenBounds.getWidth()));
    }

    public static double defaultStageHeight(Rectangle2D screenBounds) {
        return Math.max(MIN_HEIGHT, Math.min(PREF_HEIGHT, screenBounds.getHeight()));
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.SPLASH;
    }
}
