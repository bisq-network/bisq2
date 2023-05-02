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

package bisq.desktop.primary.main.left;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@Slf4j
public class LeftNavController implements Controller {
    private final LeftNavModel model;
    @Getter
    private final LeftNavView view;

    public LeftNavController(DefaultApplicationService applicationService) {
        model = new LeftNavModel(applicationService);
        view = new LeftNavView(model, this);
    }

    public void setNavigationTarget(NavigationTarget navigationTarget) {
        NavigationTarget supportedNavigationTarget;
        Set<NavigationTarget> navigationTargets = model.getNavigationTargets();
        if (navigationTargets.contains(navigationTarget)) {
            supportedNavigationTarget = navigationTarget;
        } else {
            // We get NavigationTarget.CONTENT sometimes due to some timing issues at startup. 
            // If that happens we use the persisted target if present or the default NavigationTarget 
            // otherwise.
            supportedNavigationTarget = navigationTarget.getPath().stream()
                    .filter(navigationTargets::contains)
                    .findAny()
                    .orElse(Navigation.getPersistedNavigationTarget()
                            .orElse(NavigationTarget.DASHBOARD));
        }

        findNavButton(supportedNavigationTarget)
                .ifPresent(leftNavButton -> model.getSelectedNavigationButton().set(leftNavButton));
        model.getSelectedNavigationTarget().set(supportedNavigationTarget);

        switch (supportedNavigationTarget) {
            case BISQ_ACADEMY:
            case BITCOIN_ACADEMY:
            case SECURITY_ACADEMY:
            case PRIVACY_ACADEMY:
            case WALLETS_ACADEMY:
            case FOSS_ACADEMY:
                onLearSubMenuExpanded(true);
                break;
            case BISQ_EASY:
            case LIQUID_SWAP:
            case BISQ_MULTISIG:
            case MONERO_SWAP:
            case BSQ_SWAP:
            case LIGHTNING_X:
                onTradeAppsSubMenuExpanded(true);
                break;
        }
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNavigationTargetSelected(NavigationTarget navigationTarget) {
        findNavButton(navigationTarget).ifPresent(leftNavButton -> model.getSelectedNavigationButton().set(leftNavButton));
        model.getSelectedNavigationTarget().set(navigationTarget);
        Navigation.navigateTo(navigationTarget);
    }

    void onToggleHorizontalExpandState() {
        model.getMenuHorizontalExpanded().set(!model.getMenuHorizontalExpanded().get());
    }

    void onNavigationButtonCreated(LeftNavButton leftNavButton) {
        model.getLeftNavButtons().add(leftNavButton);
        model.getNavigationTargets().add(leftNavButton.getNavigationTarget());
    }

    void onTradeAppsSubMenuExpanded(boolean value) {
        model.getTradeAppsSubMenuExpanded().set(value);
    }

    void onLearSubMenuExpanded(boolean value) {
        model.getLearnsSubMenuExpanded().set(value);
    }

    Optional<LeftNavButton> findNavButton(NavigationTarget navigationTarget) {
        return model.getLeftNavButtons().stream()
                .filter(leftNavButton -> navigationTarget == leftNavButton.getNavigationTarget())
                .findAny();
    }
}
