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

package bisq.desktop.main.content.settings;

import bisq.common.application.DevMode;
import bisq.desktop.main.content.ContentTabView;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;

public class SettingsView extends ContentTabView<SettingsModel, SettingsController> {
    public SettingsView(SettingsModel model, SettingsController controller) {
        super(model, controller);

        addTab(Res.get("settings.language"), NavigationTarget.LANGUAGE_SETTINGS);
        addTab(Res.get("settings.notifications"), NavigationTarget.NOTIFICATION_SETTINGS);
        addTab(Res.get("settings.trade"), NavigationTarget.TRADE_SETTINGS);
        addTab(Res.get("settings.network"), NavigationTarget.NETWORK_SETTINGS);

        // Hide it until pairing is production ready
        if (DevMode.isDevMode()) {
            addTab(Res.get("settings.bisqConnect"), NavigationTarget.BISQ_CONNECT_SETTINGS);
        }
        addTab(Res.get("settings.misc"), NavigationTarget.MISC_SETTINGS);
    }
}
