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

package bisq.common.platform;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PlatformUtils {
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;

    public static Path getUserDataDir() {
        if (OS.isWindows()) {
            return Paths.get(System.getenv("APPDATA"));
        }

        if (OS.isMacOs()) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support");
        }

        if (OS.isAndroid()) {
            throw new RuntimeException("getUserDataDir is not supported for Android platform. " +
                    "Provide the user data dir from the Android activity instead.");
        }

        // *nix
        return Paths.get(System.getProperty("user.home"), ".local", "share");
    }

    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static String getInstallerExtension() {
        if (OS.isMacOs()) {
            return ".dmg";
        } else if (OS.isWindows()) {
            return ".exe";
        } else if (LinuxDistribution.isDebian()) {
            return ".deb";
        } else if (LinuxDistribution.isRedHat()) {
            return ".rpm";
        } else {
            throw new RuntimeException("No suitable install package available for your OS.");
        }
    }

    public static void makeBinaryExecutable(Path binaryPath) {
        boolean isSuccess = binaryPath.toFile().setExecutable(true);
        if (!isSuccess) {
            throw new IllegalStateException(
                    String.format("Couldn't make `%s` executable.", binaryPath)
            );
        }
    }

    public static boolean browse(URI uri) {
        return open(uri.toString());
    }

    public static boolean browse(String url) {
        return open(url);
    }

    public static boolean open(File file) {
        return open(file.getPath());
    }

    public static boolean open(String target) {
        if (OS.isLinux()) {
            if (runCommand("kde-open", "%s", target)) return true;
            if (runCommand("gnome-open", "%s", target)) return true;
            if (runCommand("xdg-open", "%s", target)) return true;
        }

        if (OS.isMacOs()) {
            if (runCommand("open", "%s", target)) return true;
        }

        if (OS.isWindows()) {
            return runCommand("explorer", "%s", "\"" + target + "\"");
        }

        return false;
    }

    private static boolean runCommand(String command, String args, String fileName) {
        log.info("Trying to exec: cmd = {} args = {} file = {}", command, args, fileName);
        String[] parts = prepareCommand(command, args, fileName);
        try {
            Process p = Runtime.getRuntime().exec(parts);
            if (p == null) return false;

            try {
                int value = p.exitValue();
                if (value == 0) {
                    log.warn("Process ended immediately.");
                } else {
                    log.warn("Process crashed.");
                }
                return false;
            } catch (IllegalThreadStateException e) {
                log.info("Process is running.");
                return true;
            }
        } catch (IOException e) {
            log.warn("Error running command. {}", e.toString());
            return false;
        }
    }

    private static String[] prepareCommand(String command, String args, String fileName) {
        List<String> parts = new ArrayList<>();
        parts.add(command);
        if (args != null) {
            for (String s : args.split(" ")) {
                s = String.format(s, fileName);
                parts.add(s.trim());
            }
        }
        return parts.toArray(new String[0]);
    }

    public static String getDownloadOfHomeDir() {
        File file = new File(getHomeDirectory() + "/Downloads");
        if (file.exists()) {
            return file.getAbsolutePath();
        } else {
            return getHomeDirectory();
        }
    }

    public static String getHomeDirectory() {
        return OS.isWindows() ? System.getenv("USERPROFILE") : System.getProperty("user.home");
    }
}
