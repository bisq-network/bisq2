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

package bisq.desktop.common.view;

import bisq.bisq_easy.NavigationTarget;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class NavigationModel implements Model {
    @Getter
    protected final ObjectProperty<View<? extends Parent, ? extends Model, ? extends Controller>> view = new SimpleObjectProperty<>();
    protected NavigationTarget navigationTarget;
    private Optional<NavigationTarget> resolvedTarget = Optional.empty();

    void setNavigationTarget(NavigationTarget navigationTarget) {
        this.navigationTarget = navigationTarget;
    }

    public NavigationTarget getNavigationTarget() {
        return navigationTarget;
    }

    Optional<NavigationTarget> getResolvedTarget() {
        return resolvedTarget;
    }

    void setResolvedTarget(Optional<NavigationTarget> selectedChildTarget) {
        this.resolvedTarget = selectedChildTarget;
    }

    public abstract NavigationTarget getDefaultNavigationTarget();

    void setView(View<? extends Parent, ? extends Model, ? extends Controller> view) {
        this.view.set(view);
    }
}
