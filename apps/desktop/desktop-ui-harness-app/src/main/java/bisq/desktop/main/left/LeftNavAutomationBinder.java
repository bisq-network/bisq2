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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.left;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop_ui_harness_app.AbstractDesktopAutomationViewBinder;

import java.util.Map;

public final class LeftNavAutomationBinder extends AbstractDesktopAutomationViewBinder<LeftNavView> {
    private static final Map<NavigationTarget, String> LEFT_NAV_IDS = Map.ofEntries(
            Map.entry(NavigationTarget.DASHBOARD, "dashboard"),
            Map.entry(NavigationTarget.BISQ_EASY, "bisq-easy"),
            Map.entry(NavigationTarget.MU_SIG, "mu-sig"),
            Map.entry(NavigationTarget.REPUTATION, "reputation"),
            Map.entry(NavigationTarget.CONTACTS_LIST, "contacts"),
            Map.entry(NavigationTarget.TRADE_PROTOCOLS, "trade-protocols"),
            Map.entry(NavigationTarget.WALLET, "wallet"),
            Map.entry(NavigationTarget.ACADEMY, "academy"),
            Map.entry(NavigationTarget.CHAT, "chat"),
            Map.entry(NavigationTarget.SUPPORT, "support"),
            Map.entry(NavigationTarget.USER, "user"),
            Map.entry(NavigationTarget.NETWORK, "network"),
            Map.entry(NavigationTarget.SETTINGS, "settings"),
            Map.entry(NavigationTarget.AUTHORIZED_ROLE, "authorized-role")
    );

    @Override
    public Class<LeftNavView> viewType() {
        return LeftNavView.class;
    }

    @Override
    public void bind(LeftNavView view) {
        scope(view.mainNavigationScope(), "left-nav");
        LEFT_NAV_IDS.forEach((target, automationId) ->
                view.navigationAction(target).ifPresent(node -> id(node, automationId)));
    }
}
