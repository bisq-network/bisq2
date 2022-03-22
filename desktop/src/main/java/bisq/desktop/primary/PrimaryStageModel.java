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
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.settings.Cookie;
import bisq.settings.CookieKey;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import lombok.Getter;

import java.util.Optional;

@Getter
public class PrimaryStageModel implements Model {
    private final String title;
    private final DefaultApplicationService applicationService;
    private final Optional<Double> stageX;
    private final Optional<Double> stageY;
    private final Optional<Double> stageWidth;
    private final Optional<Double> stageHeight;
    private final double minWidth = 800;
    private final double minHeight = 600;
    private final double prefWidth = 1100;
    private final double prefHeight = 1100;
    @Getter
    private final ObjectProperty<View<? extends Parent, ? extends Model, ? extends Controller>> view = new SimpleObjectProperty<>();

    public PrimaryStageModel(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        title = applicationService.getApplicationConfig().appName();

        Cookie cookie = applicationService.getSettingsService().getPersistableStore().getCookie();
        stageX = cookie.getAsOptionalDouble(CookieKey.STAGE_X);
        stageY = cookie.getAsOptionalDouble(CookieKey.STAGE_Y);
        stageWidth = cookie.getAsOptionalDouble(CookieKey.STAGE_W);
        stageHeight = cookie.getAsOptionalDouble(CookieKey.STAGE_H);
    }

    public void setView(View<? extends Parent, ? extends Model, ? extends Controller> view) {
        this.view.set(view);
    }
}
