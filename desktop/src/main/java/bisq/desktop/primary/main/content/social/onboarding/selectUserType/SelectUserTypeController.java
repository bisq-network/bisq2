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

package bisq.desktop.primary.main.content.social.onboarding.selectUserType;

import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectUserTypeController implements Controller {
    private final SelectUserTypeModel model;
    @Getter
    private final SelectUserTypeView view;

    public SelectUserTypeController() {
        model = new SelectUserTypeModel();
        view = new SelectUserTypeView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void onNewTrader() {
        Navigation.navigateTo(NavigationTarget.ONBOARD_NEWBIE);
    }

    public void onProTrader() {
        Navigation.navigateTo(NavigationTarget.ONBOARD_PRO_TRADER);
    }

    public void onSkip() {
        Navigation.navigateTo(NavigationTarget.CHAT);
    }
}
