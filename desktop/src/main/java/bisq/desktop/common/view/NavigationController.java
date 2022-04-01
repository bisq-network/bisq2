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

    @Override
    public void onActivateInternal() {
        Navigation.addListener(host, this);
        onActivate();
    }

    @Override
    public void onDeactivateInternal() {
        Navigation.removeListener(host, this);
        onDeactivate();
    }

    protected Optional<Controller> findController(NavigationTarget navigationTarget, Optional<Object> data) {
        if (controllerCache.containsKey(navigationTarget)) {
            Controller controller = controllerCache.get(navigationTarget);
            if (controller instanceof InitWithDataController initWithDataController) {
                data.ifPresent(initWithDataController::initWithObject);
            }

            return Optional.of(controller);
        } else {
            return createController(navigationTarget)
                    .map(controller -> {
                        if (controller instanceof InitWithDataController initWithDataController) {
                            data.ifPresent(initWithDataController::initWithObject);
                        }
                        if (!(controller instanceof NonCachingController)) {
                            controllerCache.put(navigationTarget, controller);
                        }
                        return controller;
                    });
        }
    }

    protected abstract Optional<? extends Controller> createController(NavigationTarget navigationTarget);

    protected abstract NavigationModel getModel();
}
