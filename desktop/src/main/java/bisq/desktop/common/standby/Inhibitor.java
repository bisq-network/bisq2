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

import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Slf4j
class Inhibitor implements PreventStandbyMode {
    // On linux, preferred inhibitor is gnome-session-inhibit, then fall back to systemd-inhibit if it is installed.
    private static final List<String> INHIBIT_EXECUTABLE_CANDIDATES = List.of(
            "/usr/bin/gnome-session-inhibit",
            "/bin/gnome-session-inhibit",
            "/usr/bin/systemd-inhibit",
            "/bin/systemd-inhibit");
    private ExecutorService executor;

    static Optional<PreventStandbyMode> findExecutableInhibitor() {
        return findInhibitExecutable().map(file -> new Inhibitor(file.getPath()));
    }

    static Optional<File> findInhibitExecutable() {
        return INHIBIT_EXECUTABLE_CANDIDATES.stream()
                .map(File::new)
                .filter(File::canExecute)
                .filter(File::exists)
                .findAny();
    }

    private final String inhibitExecutablePath;
    private volatile boolean isPlaying;
    private Optional<Process> process = Optional.empty();

    Inhibitor(String inhibitExecutablePath) {
        this.inhibitExecutablePath = inhibitExecutablePath;
    }

    @Override
    public void initialize() {
        if (isPlaying) {
            return;
        }
        isPlaying = true;

        executor = ExecutorFactory.newSingleThreadExecutor("PreventStandbyMode");
        executor.submit(() -> {
            try {
                String[] commands = inhibitExecutablePath.contains("gnome-session-inhibit")
                        ? new String[]{inhibitExecutablePath, "--app-id", "Bisq", "--inhibit", "suspend", "--reason", "Avoid Standby", "--inhibit-only"}
                        : new String[]{inhibitExecutablePath, "--who", "Bisq", "--what", "sleep", "--why", "Avoid Standby", "--mode", "block", "tail", "-f", "/dev/null"};
                ProcessBuilder processBuilder = new ProcessBuilder(commands);
                process = Optional.of(processBuilder.start());
                log.info("Started -- disabled power management via {}", String.join(" ", commands));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void shutdown() {
        isPlaying = false;
        process.ifPresent(process -> {
            log.info("Stopping process {} isAlive={}", process.toHandle().info(), process.isAlive());
            if (process.isAlive()) {
                process.destroy();
            }
        });
        process = Optional.empty();
        if (executor != null) {
            ExecutorFactory.shutdownAndAwaitTermination(executor, 10);
        }
    }
}