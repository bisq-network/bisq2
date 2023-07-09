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

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabButton;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class UserView extends TabView<UserModel, UserController> {

    private final TabButton alertSenderTabButton;
    private Subscription alertSenderTabButtonVisiblePin;

    public UserView(UserModel model, UserController controller) {
        super(model, controller);

        addTab(Res.get("user.userProfile"), NavigationTarget.USER_PROFILE);
        addTab(Res.get("user.password"), NavigationTarget.PASSWORD);
        addTab(Res.get("user.paymentAccounts"), NavigationTarget.BISQ_EASY_PAYMENT_ACCOUNTS);
        addTab(Res.get("user.reputation"), NavigationTarget.REPUTATION);
        addTab(Res.get("user.roles"), NavigationTarget.ROLES);
        addTab(Res.get("user.nodes"), NavigationTarget.NODES);
        alertSenderTabButton = addTab(Res.get("user.alertSender"), NavigationTarget.SECURITY_MANAGER);
        headLine.setText(Res.get("user.headline"));
    }

    @Override
    protected void onViewAttached() {
        alertSenderTabButtonVisiblePin = EasyBind.subscribe(model.getAlertSenderTabButtonVisible(),
                value -> {
                    alertSenderTabButton.setVisible(value);
                    alertSenderTabButton.setManaged(value);
                    maybeAnimateMark();
                });
    }

    @Override
    protected void onViewDetached() {
        alertSenderTabButtonVisiblePin.unsubscribe();
    }
}
