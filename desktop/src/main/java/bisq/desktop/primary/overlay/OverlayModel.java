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

package bisq.desktop.primary.overlay;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationModel;
import bisq.desktop.common.view.NavigationTarget;
import bisq.settings.DisplaySettings;
import lombok.Getter;
import lombok.Setter;

@Getter
public class OverlayModel extends NavigationModel {
    public static final double WIDTH = 920;
    public static final double HEIGHT = 550;
    public static final double TOP_MARGIN = 50;
    public static final double BOTTOM_MARGIN = 50;
    public static final double HORIZONTAL_MARGIN = 90;

    private final DisplaySettings displaySettings;
    @Setter
    private double topMargin = TOP_MARGIN;
    @Setter
    private double bottomMargin = BOTTOM_MARGIN;
    @Setter
    private double horizontalMargin = HORIZONTAL_MARGIN;
    @Setter
    private Transitions.Type transitionsType = Transitions.DEFAULT_TYPE;

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.NONE;
    }

    OverlayModel(DefaultApplicationService applicationService) {
        displaySettings = applicationService.getSettingsService().getDisplaySettings();
    }

    double getDuration(double duration) {
        return displaySettings.isUseAnimations() ? duration : 1;
    }
}