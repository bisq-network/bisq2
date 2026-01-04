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

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.settings.bisq_connect.BisqConnectController;
import bisq.desktop.main.content.settings.language.LanguageSettingsController;
import bisq.desktop.main.content.settings.misc.MiscSettingsController;
import bisq.desktop.main.content.settings.network.NetworkSettingsController;
import bisq.desktop.main.content.settings.notifications.NotificationsSettingsController;
import bisq.desktop.main.content.settings.trade.TradeSettingsController;
import bisq.desktop.navigation.NavigationTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SettingsController extends ContentTabController<SettingsModel> {
    @Getter
    private final SettingsView view;

    public SettingsController(ServiceProvider serviceProvider) {
        super(new SettingsModel(), NavigationTarget.SETTINGS, serviceProvider);

        view = new SettingsView(model, this);
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case LANGUAGE_SETTINGS -> Optional.of(new LanguageSettingsController(serviceProvider));
            case NOTIFICATION_SETTINGS -> Optional.of(new NotificationsSettingsController(serviceProvider));
            case TRADE_SETTINGS -> Optional.of(new TradeSettingsController(serviceProvider));
            case NETWORK_SETTINGS -> Optional.of(new NetworkSettingsController(serviceProvider));
            case BISQ_CONNECT_SETTINGS -> Optional.of(new BisqConnectController(serviceProvider));
            case MISC_SETTINGS -> Optional.of(new MiscSettingsController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
