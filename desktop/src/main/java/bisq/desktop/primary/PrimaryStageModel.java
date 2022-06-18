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

package bisq.desktop.primary;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.NavigationModel;
import bisq.desktop.common.view.NavigationTarget;
import bisq.settings.Cookie;
import bisq.settings.CookieKey;
import lombok.Getter;

import java.util.Optional;

@Getter
public class PrimaryStageModel extends NavigationModel {
    // Min supported screens: 1024x768
    public static final double MIN_WIDTH = 1000;
    public static final double MIN_HEIGHT = 730;
    
    private final String title;
    private final DefaultApplicationService applicationService;
    private final Optional<Double> stageX;
    private final Optional<Double> stageY;
    private final Optional<Double> stageWidth;
    private final Optional<Double> stageHeight;
    private final double prefWidth = MIN_WIDTH;
    private final double prefHeight = MIN_HEIGHT;

    public PrimaryStageModel(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        title = applicationService.getApplicationConfig().appName();

        Cookie cookie = applicationService.getSettingsService().getPersistableStore().getCookie();
        stageX = cookie.getAsOptionalDouble(CookieKey.STAGE_X);
        stageY = cookie.getAsOptionalDouble(CookieKey.STAGE_Y);
        stageWidth = cookie.getAsOptionalDouble(CookieKey.STAGE_W);
        stageHeight = cookie.getAsOptionalDouble(CookieKey.STAGE_H);
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.SPLASH;
    }
}
