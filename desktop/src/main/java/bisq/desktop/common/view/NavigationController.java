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

    public NavigationController(NavigationTarget host) {
        this.host = host;
    }

    void processNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        onStartProcessNavigationTarget(navigationTarget, data);
        NavigationModel model = getModel();
        Optional<NavigationTarget> optionalCandidateTarget = Optional.of(navigationTarget);
        while (optionalCandidateTarget.isPresent()) {
            NavigationTarget candidateTarget = optionalCandidateTarget.get();
            if (model.getResolvedTarget().isPresent() && model.getResolvedTarget().get() == candidateTarget) {
                // We as host controller have already selected the child target in question.
                // We exit the loop here.
                break;
            }
            Optional<Controller> optionalChildController = findController(candidateTarget, data);
            if (optionalChildController.isPresent()) {
                // We as host have found the controller and handle that target. 
                Controller childController = optionalChildController.get();
                model.setNavigationTarget(candidateTarget);
                model.setResolvedTarget(optionalCandidateTarget);
                model.setView(childController.getView());
                Navigation.persistNavigationTarget(navigationTarget);
                onNavigationTargetApplied(navigationTarget, data);
                break;
            } else {
                // If we as host do not handle that child target we go down to our parent to see if we handle 
                // any other target on the path. 
                // At NavigationTarget.ROOT we don't have a parent and candidate is not present, exiting the while loop
                optionalCandidateTarget = candidateTarget.getParent();
                if (optionalCandidateTarget.isEmpty()) {
                    onNoNavigationTargetApplied(navigationTarget, data);
                }
            }
        }
    }

    // Called before the navigation target gets processed
    protected void onStartProcessNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
    }

    // Gets called after we have successfully applied a child target 
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
    }

    // Gets called after we have reached the root without handling the target
    protected void onNoNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
    }

    public void resetSelectedChildTarget() {
        getModel().setNavigationTarget(getModel().getDefaultNavigationTarget());
        getModel().setResolvedTarget(Optional.empty());
        getModel().setView(null);
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

        if (model.getNavigationTarget() != NavigationTarget.NONE) {
            UIThread.runOnNextRenderFrame(() -> processNavigationTarget(model.getNavigationTarget(), Optional.empty()));
        }

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
            if (controller instanceof InitWithDataController) {
                InitWithDataController<?> initWithDataController = (InitWithDataController<?>) controller;
                data.ifPresent(initWithDataController::initWithObject);
            }
            return Optional.of(controller);
        } else {
            return createController(navigationTarget)
                    .map(controller -> {
                        if (controller instanceof InitWithDataController) {
                            InitWithDataController<?> initWithDataController = (InitWithDataController<?>) controller;
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
