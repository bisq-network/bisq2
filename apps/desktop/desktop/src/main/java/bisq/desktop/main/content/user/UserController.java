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

package bisq.desktop.main.content.user;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.user.accounts.PaymentAccountsController;
import bisq.desktop.main.content.user.password.PasswordController;
import bisq.desktop.main.content.user.user_profile.UserProfileController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class UserController extends ContentTabController<UserModel> {
    @Getter
    private final UserView view;

    public UserController(ServiceProvider serviceProvider) {
        super(new UserModel(), NavigationTarget.USER, serviceProvider);

        view = new UserView(model, this);
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case USER_PROFILE -> Optional.of(new UserProfileController(serviceProvider));
            case PASSWORD -> Optional.of(new PasswordController(serviceProvider));
            case PAYMENT_ACCOUNTS -> Optional.of(new PaymentAccountsController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
