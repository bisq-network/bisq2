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

import lombok.Getter;

import java.util.Locale;

public enum Architecture {
    X86_64("x86_64"),
    ARM_64("arm64");

    @Getter
    private final String canonicalName;

    Architecture(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public static Architecture getArchitecture() {
        String architectureName = getOsArch();
        if (isX86_64(architectureName)) {
            return Architecture.X86_64;
        } else if (isArm64(architectureName)) {
            return Architecture.ARM_64;
        }
        throw new IllegalStateException("Running on unsupported Architecture: " + architectureName);
    }

    public static boolean isX86_64() {
        return isX86_64(getOsArch());
    }

    public static boolean isX86_64(String architectureName) {
        return is64Bit(architectureName) && (architectureName.contains("x86") || architectureName.contains("amd"));
    }

    public static boolean isArm64() {
        return isArm64(getOsArch());
    }

    public static boolean isArm64(String architectureName) {
        return is64Bit(architectureName) && (architectureName.contains("aarch") || architectureName.contains("arm"));
    }


    public static String getBit() {
        return getBit(getOsArch());
    }

    public static String getBit(String architectureName) {
        return is64Bit(architectureName) ? "64" : "32";
    }

    public static boolean is64Bit() {
        return is64Bit(getOsArch());
    }

    public static boolean is64Bit(String architectureName) {
        return architectureName.contains("64") || architectureName.contains("armv8");
    }

    public static String getOsArch() {
        return System.getProperty("os.arch").toLowerCase(Locale.US);
    }
}