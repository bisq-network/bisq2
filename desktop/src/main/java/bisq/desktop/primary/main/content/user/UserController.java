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

package bisq.desktop.primary.main.content.user;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.user.accounts.PaymentAccountsController;
import bisq.desktop.primary.main.content.user.password.PasswordController;
import bisq.desktop.primary.main.content.user.reputation.ReputationController;
import bisq.desktop.primary.main.content.user.roles.RolesController;
import bisq.desktop.primary.main.content.user.userProfile.UserProfileController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class UserController extends TabController<UserModel> {
    private final DefaultApplicationService applicationService;
    @Getter
    private final UserView view;

    public UserController(DefaultApplicationService applicationService) {
        super(new UserModel(), NavigationTarget.USER);

        this.applicationService = applicationService;

        view = new UserView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case USER_PROFILE: {
                return Optional.of(new UserProfileController(applicationService));
            }
            case PASSWORD: {
                return Optional.of(new PasswordController(applicationService));
            }
            case BISQ_EASY_PAYMENT_ACCOUNTS: {
                return Optional.of(new PaymentAccountsController(applicationService));
            }
            case REPUTATION: {
                return Optional.of(new ReputationController(applicationService));
            }
            case ROLES: {
                return Optional.of(new RolesController(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
