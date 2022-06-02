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

import bisq.desktop.common.threading.UIThread;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class NavigationController implements Controller {
    protected final NavigationTarget host;
    private final Map<NavigationTarget, Controller> controllerCache = new ConcurrentHashMap<>();
    private Optional<NavigationTarget> selectedChildTarget = Optional.empty();

    public NavigationController(NavigationTarget host) {
        this.host = host;
    }

    void processNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        onNavigate(navigationTarget, data);
        Optional<NavigationTarget> candidate = Optional.of(navigationTarget);
        while (candidate.isPresent()) {
            if (selectedChildTarget.isPresent() && selectedChildTarget.get() == candidate.get()) {
                // We as host controller have already selected the child target in question.
                // We exit the loop here.
                break;
            }
            Optional<Controller> childController = findController(candidate.get(), data);
            if (childController.isPresent()) {
                // We as host handle that target and found the controller. 
                getModel().setNavigationTarget(candidate.get());
                getModel().setView(childController.get().getView());
                selectedChildTarget = candidate;
                Navigation.persistNavigationTarget(navigationTarget);
                break;
            } else {
                // If we as host do not handle that child target we go down one parent to see we have handle 
                // any other target in the path. 
                // At NavigationTarget.ROOT we don't have a parent and candidate is not present, exiting the while loop
                candidate = candidate.get().getParent();
            }
        }
    }

    public void resetSelectedChildTarget() {
        selectedChildTarget = Optional.empty();
        getModel().setView(null);
    }

    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
    }

    @Override
    public void onActivateInternal() {
        Navigation.addNavigationController(host, this);

        // Apply child target for our host from the persisted NavigationTarget
        NavigationModel model = getModel();
        if (model.getNavigationTarget() == null) {
            Navigation.getPersistedNavigationTarget().ifPresent(persisted -> {
                Optional<NavigationTarget> hostCandidate = persisted.getParent();
                NavigationTarget childCandidate = persisted;
                while (hostCandidate.isPresent() && hostCandidate.get() != host) {
                    childCandidate = hostCandidate.get();
                    hostCandidate = childCandidate.getParent();
                }
                NavigationTarget finalChildCandidate = childCandidate;
                hostCandidate.ifPresent(e -> model.setNavigationTarget(finalChildCandidate));
            });
        }
        // If we did not have a persisted target we apply the default
        if (model.getNavigationTarget() == null) {
            model.setNavigationTarget(model.getDefaultNavigationTarget());
        }

        UIThread.runOnNextRenderFrame(() -> processNavigationTarget(model.getNavigationTarget(), Optional.empty()));

        onActivate();
    }

    @Override
    public void onDeactivateInternal() {
        Navigation.removeNavigationController(host, this);
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
                        //todo
                        if (controller instanceof InitWithDataController initWithDataController) {
                            data.ifPresent(initWithDataController::initWithObject);
                        }
                        if (controller.useCaching()) {
                            controllerCache.put(navigationTarget, controller);
                        }
                        return controller;
                    });
        }
    }

    protected abstract Optional<? extends Controller> createController(NavigationTarget navigationTarget);

    protected abstract NavigationModel getModel();
}
