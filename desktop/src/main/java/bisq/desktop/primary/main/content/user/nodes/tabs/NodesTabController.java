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

package bisq.desktop.primary.main.content.user.nodes.tabs;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.user.nodes.tabs.registration.NodeRegistrationController;
import bisq.user.node.NodeType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class NodesTabController extends TabController<NodesTabModel> {
    @Getter
    private final NodesTabView view;
    private final DefaultApplicationService applicationService;

    public NodesTabController(DefaultApplicationService applicationService) {
        super(new NodesTabModel(), NavigationTarget.NODES_TABS);

        this.applicationService = applicationService;
        view = new NodesTabView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case REGISTER_SEED_NODE: {
                return Optional.of(new NodeRegistrationController(applicationService, NodeType.SEED_NODE));
            }
            case REGISTER_ORACLE_NODE: {
                return Optional.of(new NodeRegistrationController(applicationService, NodeType.ORACLE_NODE));
            }
            case REGISTER_EXPLORER_NODE: {
                return Optional.of(new NodeRegistrationController(applicationService, NodeType.EXPLORER_NODE));
            }
            case REGISTER_MARKET_PRICE_NODE: {
                return Optional.of(new NodeRegistrationController(applicationService, NodeType.MARKET_PRICE_NODE));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
