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

package bisq.desktop.main.content.settings;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import lombok.Getter;

// Handled jfx only concerns, others which can be re-used by other frontends are in OfferbookEntity
public class SettingsModel implements Model {

    private final DefaultServiceProvider serviceProvider;
    @Getter
    private final ObjectProperty<View<? extends Parent, ? extends Model, ? extends Controller>> view = new SimpleObjectProperty<>();
    @Getter
    private NavigationTarget selectedNavigationTarget = NavigationTarget.NETWORK_INFO;

    public SettingsModel(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public void activate() {
    }

    public void deactivate() {
    }

    public void selectView(NavigationTarget navigationTarget, View<? extends Parent, ? extends Model, ? extends Controller> view) {
        this.selectedNavigationTarget = navigationTarget;
        this.view.set(view);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package private
    ///////////////////////////////////////////////////////////////////////////////////////////////////
}
