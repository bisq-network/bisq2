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

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class NavigationController implements Controller {
    protected final NavigationTarget host;
    private final Map<NavigationTarget, Controller> controllerCache = new ConcurrentHashMap<>();
    // We do not hold the controller as we don't want to pin a reference in case it's a non caching controller
    private Optional<String> childControllerClassName = Optional.empty();
    // We don't implement Navigation.Listener to avoid that subclasses might forget to call super.
    private final Navigation.Listener navigationListener;

    public NavigationController(NavigationTarget host) {
        this.host = host;

        navigationListener = (navigationTarget, data) -> {
            onNavigate2(navigationTarget, data);
            Optional<NavigationTarget> candidate = Optional.of(navigationTarget);
            while (candidate.isPresent()) {
                Optional<Controller> childController = findController(candidate.get(), data);
                if (childController.isPresent()) {
                    // If that controller is handling that navigationTarget and creates a childController we 
                    // apply the child view to our view.
                    if (childControllerClassName.isEmpty() ||
                            !childControllerClassName.get().equals(childController.get().getClass().toString())) {
                        // log.debug("{}: Apply child controller. childController={}",
                        //          this.getClass().getSimpleName(), childController.get().getClass().getSimpleName());
                        getModel().applyChild(candidate.get(), childController.get().getView());
                        childControllerClassName = Optional.of(childController.get().getClass().toString());
                    } else {
                        // We might get called from the navigation event dispatcher when child views gets attached and
                        // apply their default navigationTargets. 
                        // log.debug("{}: We have applied already that child controller. childController={}",
                        //         this.getClass().getSimpleName(), childController.get().getClass().getSimpleName());
                    }
                    break;
                } else {
                    // At NavigationTarget.ROOT we don't have a parent and candidate is not present, exiting the while loop
                    candidate = candidate.get().getParent();
                }
            }
        };
    }

    public void onNavigate2(NavigationTarget navigationTarget, Optional<Object> data) {
    }

    @Override
    public void onActivateInternal() {
        Navigation.addListener(host, navigationListener);
        onActivate();
    }

    @Override
    public void onDeactivateInternal() {
        Navigation.removeListener(host, navigationListener);
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
                        if (controller instanceof CachingController) {
                            controllerCache.put(navigationTarget, controller);
                        }
                        return controller;
                    });
        }
    }

    protected abstract Optional<? extends Controller> createController(NavigationTarget navigationTarget);

    protected abstract NavigationModel getModel();
}
