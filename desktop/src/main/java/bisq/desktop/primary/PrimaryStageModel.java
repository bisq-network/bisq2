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

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.user.Cookie;
import bisq.user.CookieKey;
import bisq.user.UserService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import lombok.Getter;

import java.util.Optional;

@Getter
public class PrimaryStageModel implements Model {
    private final UserService userService;
    private final String title;
    private final Optional<Double> stageX;
    private final Optional<Double> stageY;
    private final Optional<Double> stageWidth;
    private final Optional<Double> stageHeight;
    private final double minWidth = 800;
    private final double minHeight = 600;
    private final double prefWidth = 1600;
    private final double prefHeight = 1300;
    protected NavigationTarget navigationTarget;
    @Getter
    protected final ObjectProperty<View<? extends Parent, ? extends Model, ? extends Controller>> view = new SimpleObjectProperty<>();

    public PrimaryStageModel(DefaultServiceProvider serviceProvider) {
        userService = serviceProvider.getUserService();

        title = serviceProvider.getApplicationOptions().appName();

        Cookie cookie = userService.getUser().getCookie();
        stageX = cookie.getAsOptionalDouble(CookieKey.STAGE_X);
        stageY = cookie.getAsOptionalDouble(CookieKey.STAGE_Y);
        stageWidth = cookie.getAsOptionalDouble(CookieKey.STAGE_W);
        stageHeight = cookie.getAsOptionalDouble(CookieKey.STAGE_H);
    }

    public void setView(View<? extends Parent, ? extends Model, ? extends Controller> view) {
        this.view.set(view);
    }

    public void setStageX(double value) {
        userService.getUser().getCookie().putAsDouble(CookieKey.STAGE_X, value);
        userService.persist();
    }

    public void setStageY(double value) {
        userService.getUser().getCookie().putAsDouble(CookieKey.STAGE_Y, value);
        userService.persist();
    }

    public void setStageWidth(double value) {
        userService.getUser().getCookie().putAsDouble(CookieKey.STAGE_W, value);
        userService.persist();
    }

    public void setStageHeight(double value) {
        userService.getUser().getCookie().putAsDouble(CookieKey.STAGE_H, value);
        userService.persist();
    }
}
