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

package bisq.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class OsUtils {
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;

    public static File getUserDataDir() {
        if (isWindows()) {
            return new File(System.getenv("APPDATA"));
        }

        if (isMac()) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support").toFile();
        }

        // *nix
        return Paths.get(System.getProperty("user.home"), ".local", "share").toFile();
    }

    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static boolean isWindows() {
        return getOSName().contains("win");
    }

    public static boolean isMac() {
        return getOSName().contains("mac") || getOSName().contains("darwin");
    }

    public static boolean isLinux() {
        return getOSName().contains("linux");
    }

    public static boolean isLinux32() {
        return getOSName().contains("linux") && getOSArchitecture().equals("32");
    }

    public static boolean isLinux64() {
        return getOSName().contains("linux") && getOSArchitecture().equals("64");
    }

    public static String getOSArchitecture() {
        if (isWindows()) {
            // See: Like always windows needs extra treatment
            // https://stackoverflow.com/questions/20856694/how-to-find-the-os-bit-type
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            return arch.endsWith("64")
                    || wow64Arch != null && wow64Arch.endsWith("64")
                    ? "64" : "32";
        }

        String osArch = System.getProperty("os.arch");
        // armv8 is 64 bit, armv7l is 32 bit
        if (osArch.contains("arm")) {
            return osArch.contains("64") || osArch.contains("v8") ? "64" : "32";
        }

        if (isLinux()) {
            return osArch.startsWith("i") ? "32" : "64";
        }

        return osArch.contains("64") ? "64" : osArch;
    }

    public static String getOSName() {
        return System.getProperty("os.name").toLowerCase(Locale.US);
    }

    public static OperatingSystem getOperatingSystem() {
        if (isLinux()) {
            return OperatingSystem.LINUX;
        } else if (isMac()) {
            return OperatingSystem.MAC;
        } else if (isWindows()) {
            return OperatingSystem.WIN;
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + OsUtils.getOSName());
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

    public static boolean open(File file) {
        return open(file.getPath());
    }

    public static boolean open(String fileName) {
        if (isLinux()) {
            if (runCommand("kde-open", "%s", fileName)) return true;
            if (runCommand("gnome-open", "%s", fileName)) return true;
            if (runCommand("xdg-open", "%s", fileName)) return true;
        }

        if (isMac()) {
            if (runCommand("open", "%s", fileName)) return true;
        }

        if (isWindows()) {
            return runCommand("explorer", "%s", "\"" + fileName + "\"");
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
        return isWindows() ? System.getenv("USERPROFILE") : System.getProperty("user.home");
    }
}
