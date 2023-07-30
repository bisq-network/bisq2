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

package bisq.desktop.common.utils.standby;

import bisq.common.util.OsUtils;
import bisq.desktop.ServiceProvider;
import bisq.settings.SettingsService;
import lombok.extern.slf4j.Slf4j;

/**
 * On OSX and Windows we play a silent sound and repeat it after a delay. This prevents the OS to activate standby mode.
 * Other options would be to use the Robot and create some key or mouse events, but that seems more risky.
 * The solution with playing the sound was already used in Bisq 1 and seems to have worked reliably.
 */
@Slf4j
public class PreventStandbyModeService {
    private final SettingsService settingsService;
    private final PreventStandbyMode preventStandbyMode;

    public PreventStandbyModeService(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        preventStandbyMode = OsUtils.isLinux() ? Inhibitor.findExecutableInhibitor().orElse(new SoundPlayer(serviceProvider)) : new SoundPlayer(serviceProvider);
    }

    public void initialize() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        settingsService.getPreventStandbyMode().addObserver(preventStandbyMode -> {
            if (preventStandbyMode) {
                this.preventStandbyMode.initialize();
            } else {
                this.preventStandbyMode.shutdown();
            }
        });
    }

    public void shutdown() {
        preventStandbyMode.shutdown();
    }
}
