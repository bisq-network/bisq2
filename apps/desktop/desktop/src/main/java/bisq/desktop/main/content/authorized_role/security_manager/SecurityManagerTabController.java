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

package bisq.desktop.main.content.authorized_role.security_manager;

import bisq.bonded_roles.release.AppType;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.authorized_role.security_manager.tabs.SecurityManagerController;
import bisq.desktop.navigation.NavigationTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SecurityManagerTabController extends TabController<SecurityManagerTabModel> {
    @Getter
    protected final SecurityManagerTabView view;
    private final ServiceProvider serviceProvider;

    public SecurityManagerTabController(ServiceProvider serviceProvider) {
        super(new SecurityManagerTabModel(), NavigationTarget.SECURITY_MANAGER);

        this.serviceProvider = serviceProvider;

        view = new SecurityManagerTabView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case DESKTOP_SECURITY_MANAGER -> Optional.of(new SecurityManagerController(serviceProvider, AppType.DESKTOP));
            case MOBILE_NODE_SECURITY_MANAGER ->
                    Optional.of(new SecurityManagerController(serviceProvider, AppType.MOBILE_NODE));
            case MOBILE_CLIENT_SECURITY_MANAGER ->
                    Optional.of(new SecurityManagerController(serviceProvider, AppType.MOBILE_CLIENT));
            default -> Optional.empty();
        };
    }
}
