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

package bisq.desktop.overlay;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.NavigationModel;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.Setter;

@Getter
public class OverlayModel extends NavigationModel {
    public static final double WIDTH = 920;
    public static final double HEIGHT = 550;
    public static final double TOP_MARGIN = 50;
    public static final double BOTTOM_MARGIN = 50;
    public static final double HORIZONTAL_MARGIN = 90;

    private final SettingsService settingsService;
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

    OverlayModel(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
    }

    double getDuration(double duration) {
        return settingsService.getUseAnimations().get() ? duration : 1;
    }
}