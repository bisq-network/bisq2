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

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.settings.display.DisplaySettingsController;
import bisq.desktop.main.content.settings.language.LanguageSettingsController;
import bisq.desktop.main.content.settings.network.NetworkSettingsController;
import bisq.desktop.main.content.settings.notifications.NotificationsSettingsController;
import bisq.desktop.main.content.settings.trade.TradeSettingsController;
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
        switch (navigationTarget) {
            case LANGUAGE_SETTINGS: {
                return Optional.of(new LanguageSettingsController(serviceProvider));
            }
            case NOTIFICATION_SETTINGS: {
                return Optional.of(new NotificationsSettingsController(serviceProvider));
            }
            case DISPLAY_SETTINGS: {
                return Optional.of(new DisplaySettingsController(serviceProvider));
            }
            case TRADE_SETTINGS: {
                return Optional.of(new TradeSettingsController(serviceProvider));
            }
            case NETWORK_SETTINGS: {
                return Optional.of(new NetworkSettingsController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
