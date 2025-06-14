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

package bisq.network.i2p.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;

public class PreventSleepService {
    /**
     * Windows EXECUTION_STATE flags:
     */
    interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        int ES_CONTINUOUS = 0x80000000;
        int ES_SYSTEM_REQUIRED = 0x00000001;
        int ES_DISPLAY_REQUIRED = 0x00000002;
        int ES_AWAYMODE_REQUIRED = 0x00000040;

        int SetThreadExecutionState(int flags);
    }

    private Process inhibitorProcess;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void initialize() throws IOException {
        if (Platform.isWindows()) {
            initWindows();
        } else if (Platform.isMac()) {
            initMac();
        } else if (Platform.isLinux()) {
            initLinux();
        } else {
            System.out.println("Unsupported OS, falling back to audio ping.");
            startAudioPing();
        }
    }

    private void initWindows() {
        int flags = Kernel32.ES_CONTINUOUS | Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_DISPLAY_REQUIRED | Kernel32.ES_AWAYMODE_REQUIRED;
        // Initial assertion
        Kernel32.INSTANCE.SetThreadExecutionState(flags);
        System.out.println("Windows sleep/display prevention initialized.");
        // Re-assert periodically
        scheduler.scheduleAtFixedRate(() -> Kernel32.INSTANCE.SetThreadExecutionState(flags), 1, 1, TimeUnit.MINUTES);
    }

    private void initMac() throws IOException {
        inhibitorProcess = new ProcessBuilder("caffeinate", "-d", "-i").start();
        System.out.println("macOS sleep/display prevention (caffeinate) started.");
    }

    private void initLinux() throws IOException {
        startLinuxInhibitor();
        // Monitor and restart if needed
        scheduler.scheduleAtFixedRate(() -> {
            if (inhibitorProcess == null || !inhibitorProcess.isAlive()) {
                try {
                    startLinuxInhibitor();
                } catch (IOException e) {
                    System.err.println("Failed to restart systemd-inhibit: " + e.getMessage());
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void startLinuxInhibitor() throws IOException {
        inhibitorProcess = new ProcessBuilder("systemd-inhibit", "--mode=block", "--what=idle:sleep:handle-lid-switch:handle-power-key", "/bin/bash", "-c", "while true; do sleep 60; done").start();
        System.out.println("Linux sleep prevention (systemd-inhibit) started.");
    }

    public void shutdown() {
        if (Platform.isWindows()) {
            Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
            System.out.println("Windows sleep/display prevention cleared.");
        }
        if (inhibitorProcess != null) {
            inhibitorProcess.destroy();
            System.out.println("Inhibitor process stopped.");
        }
        scheduler.shutdownNow();
    }

    private void startAudioPing() {
        scheduler.scheduleAtFixedRate(() -> System.out.println("Audio ping: stay awake."), 0, 1, TimeUnit.MINUTES);
    }
}
