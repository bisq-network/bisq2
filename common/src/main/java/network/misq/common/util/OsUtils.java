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

package network.misq.common.util;

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;

public class OsUtils {
    public static File getUserDataDir() {
        if (isWindows())
            return new File(System.getenv("APPDATA"));

        if (isOSX())
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support").toFile();

        // *nix
        return Paths.get(System.getProperty("user.home"), ".local", "share").toFile();
    }

    public static boolean isWindows() {
        return getOSName().contains("win");
    }

    public static boolean isOSX() {
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
        String osArch = System.getProperty("os.arch");
        if (isWindows()) {
            // See: Like always windows needs extra treatment
            // https://stackoverflow.com/questions/20856694/how-to-find-the-os-bit-type
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            return arch.endsWith("64")
                    || wow64Arch != null && wow64Arch.endsWith("64")
                    ? "64" : "32";
        } else if (osArch.contains("arm")) {
            // armv8 is 64 bit, armv7l is 32 bit
            return osArch.contains("64") || osArch.contains("v8") ? "64" : "32";
        } else if (isLinux()) {
            return osArch.startsWith("i") ? "32" : "64";
        } else {
            return osArch.contains("64") ? "64" : osArch;
        }
    }

    public static String getOSName() {
        return System.getProperty("os.name").toLowerCase(Locale.US);
    }
}
