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

import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class NavigationController implements Controller, Navigation.Listener {
    protected final Map<NavigationTarget, Controller> controllerCache = new ConcurrentHashMap<>();
    protected final NavigationTarget host;

    public NavigationController(NavigationTarget host) {
        this.host = host;
    }

    @Override
    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        Optional<NavigationTarget> candidate = Optional.of(navigationTarget);
        while (candidate.isPresent()) {
            Optional<Controller> controller = findController(candidate.get(), data);
            if (controller.isPresent()) {
                getModel().select(candidate.get(), controller.get().getView());
                break;
            } else {
                candidate = candidate.get().getParent();
            }
        }
    }

    protected Optional<Controller> findController(NavigationTarget navigationTarget, Optional<Object> data) {
        // We only cache in case there are no data, as otherwise usually a cached would be stale when visited again 
        // and data might be missing. If we want to cache that we need to pack the data with the target into a different
        // cache data structure.
        if (data.isPresent()) {
            return createController(navigationTarget, data);
        }

        if (controllerCache.containsKey(navigationTarget)) {
            return Optional.of(controllerCache.get(navigationTarget));
        } else {
            return createController(navigationTarget, data)
                    .map(controller -> {
                        controllerCache.put(navigationTarget, controller);
                        return controller;
                    });
        }
    }

    @Override
    public void onViewAttached() {
        Navigation.addListener(host, this);
    }

    @Override
    public void onViewDetached() {
        Navigation.removeListener(host, this);
    }

    protected abstract Optional<Controller> createController(NavigationTarget navigationTarget, Optional<Object> data);

    protected abstract NavigationModel getModel();
}
