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

package bisq.desktop.common.standby;

import bisq.common.observable.Pin;
import bisq.common.platform.OS;
import bisq.desktop.ServiceProvider;
import bisq.settings.SettingsService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PreventStandbyModeService keeps the system awake by platform-specific inhibitors.
 * Uses systemd-inhibit on Linux, caffeinate on macOS, and SetThreadExecutionState via JNA on Windows.
 * Falls back to an audio ping on other OSs.
 */
@Slf4j
public class PreventStandbyModeService {
    private final SettingsService settingsService;
    private final SleepInhibitor inhibitor;
    @Nullable
    private Pin preventStandbyModePin;

    public PreventStandbyModeService(ServiceProvider serviceProvider) {
        this.settingsService = serviceProvider.getSettingsService();
        this.inhibitor = new SleepInhibitor();
    }

    public void initialize() {
        if (OS.isAndroid()) {
            return;
        }

        preventStandbyModePin = settingsService.getPreventStandbyMode().addObserver(enabled -> {
            if (enabled) {
                try {
                    inhibitor.initialize();
                } catch (IOException e) {
                    log.error("Failed to initialize sleep inhibitor", e);
                }
            } else {
                inhibitor.shutdown();
            }
        });
    }

    public void shutdown() {
        if (preventStandbyModePin != null) {
            preventStandbyModePin.unbind();
            preventStandbyModePin = null;
        }
        inhibitor.shutdown();
    }

    private static class SleepInhibitor {
        private Process inhibitorProcess;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private boolean initialized = false;

        public void initialize() throws IOException {
            if (initialized) {
                return;
            }
            initialized = true;
            if (OS.isWindows()) {
                initWindows();
            } else if (OS.isMacOs()) {
                initMac();
            } else if (OS.isLinux()) {
                initLinux();
            } else {
                log.info("Unsupported OS {}, falling back to audio ping.", OS.getOsName());
                startAudioPing();
            }
        }

        private void initWindows() {
            int flags = Kernel32.ES_CONTINUOUS | Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_DISPLAY_REQUIRED;
            Kernel32.INSTANCE.SetThreadExecutionState(flags);
            log.info("Windows sleep/display prevention initialized.");
            scheduler.scheduleAtFixedRate(() -> Kernel32.INSTANCE.SetThreadExecutionState(flags), 1, 1, TimeUnit.MINUTES);
        }

        private void initMac() throws IOException {
            inhibitorProcess = new ProcessBuilder("caffeinate", "-d", "-i").start();
            log.info("macOS sleep/display prevention (caffeinate) started.");
        }

        private void initLinux() throws IOException {
            startLinuxInhibitor();
            scheduler.scheduleAtFixedRate(() -> {
                if (inhibitorProcess == null || !inhibitorProcess.isAlive()) {
                    try {
                        startLinuxInhibitor();
                    } catch (IOException e) {
                        log.error("Failed to restart systemd-inhibit", e);
                    }
                }
            }, 1, 1, TimeUnit.MINUTES);
        }

        private void startLinuxInhibitor() throws IOException {
            inhibitorProcess = new ProcessBuilder(
                    "systemd-inhibit",
                    "--mode=block",
                    "--what=idle:sleep:handle-lid-switch:handle-power-key",
                    "/bin/bash",
                    "-c",
                    "while true; do sleep 60; done"
            ).start();
            log.info("Linux sleep prevention (systemd-inhibit) started.");
        }

        public void shutdown() {
            initialized = false;
            if (OS.isWindows()) {
                Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
                log.info("Windows sleep/display prevention cleared.");
            }
            if (inhibitorProcess != null) {
                inhibitorProcess.destroy();
                log.info("Inhibitor process stopped.");
            }
            scheduler.shutdownNow();
        }

        private void startAudioPing() {
            scheduler.scheduleAtFixedRate(() -> log.debug("Audio ping: stay awake."), 0, 1, TimeUnit.MINUTES);
        }

        private interface Kernel32 extends com.sun.jna.Library {
            Kernel32 INSTANCE = com.sun.jna.Native.load("kernel32", Kernel32.class);
            int ES_CONTINUOUS = 0x80000000;
            int ES_SYSTEM_REQUIRED = 0x00000001;
            int ES_DISPLAY_REQUIRED = 0x00000002;

            int SetThreadExecutionState(int flags);
        }
    }
}
