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

package bisq.bi2p.common.standby;

import bisq.common.observable.Pin;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.platform.OS;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

/**
 * On OSX and Windows we play a silent sound and repeat it after a delay. This prevents the OS to activate standby mode.
 * Other options would be to use the Robot and create some key or mouse events, but that seems more risky.
 * The solution with playing the sound was already used in Bisq 1 and seems to have worked reliably.
 */
@Slf4j
public class PreventStandbyModeService {
    private final PreventStandbyMode preventStandbyMode;
    private final ReadOnlyObservable<Boolean> prevent;
    @Nullable
    private Pin preventStandbyModePin;

    public PreventStandbyModeService(String baseDir, ReadOnlyObservable<Boolean> prevent) {
        this.prevent = prevent;
        this.preventStandbyMode = OS.isLinux()
                ? Inhibitor.findExecutableInhibitor().orElseGet(() -> new SoundPlayer(baseDir))
                : new SoundPlayer(baseDir);
    }

    public void initialize() {
        if (OS.isAndroid()) {
            return;
        }

        preventStandbyModePin = prevent.addObserver(preventStandbyMode -> {
            if (preventStandbyMode) {
                this.preventStandbyMode.initialize();
            } else {
                this.preventStandbyMode.shutdown();
            }
        });
    }

    public void shutdown() {
        if (preventStandbyModePin != null) {
            preventStandbyModePin.unbind();
            preventStandbyModePin = null;
        }
        preventStandbyMode.shutdown();
    }
}
