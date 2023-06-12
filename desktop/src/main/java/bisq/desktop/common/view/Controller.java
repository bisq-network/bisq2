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

import javafx.scene.Parent;

/**
 * By default, we do not use caching. If a child controller should be cached by its parent Navigation controller,
 * it has to implement the Controller interface.
 */
public interface Controller {
    View<? extends Parent, ? extends Model, ? extends Controller> getView();

    // The internal methods should be only used by framework classes (e.g. NavigationController)
    default void onActivateInternal() {
        onActivate();
    }

    default void onDeactivateInternal() {
        onDeactivate();
    }

    void onActivate();

    void onDeactivate();

    default boolean useCaching() {
        return true;
    }
}
