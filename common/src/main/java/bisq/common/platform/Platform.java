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

public enum Platform {
    LINUX_X86_64("linux_x86_64"),
    LINUX_ARM_64("linux_arm64"),

    MACOS_X86_64("macos_x86_64"),
    MACOS_ARM_64("macos_arm64"),

    WIN_X86_64("win_x86_64"),
    WIN_ARM_64("win_arm64");

    @Getter
    private final String platformName;

    Platform(String platformName) {
        this.platformName = platformName;
    }

    public static Platform getPlatform() {
        OS os = OS.getOS();
        Architecture architecture = Architecture.getArchitecture();
        switch (os) {
            case LINUX:
                switch (architecture) {
                    case X86_64:
                        return LINUX_X86_64;
                    case ARM_64:
                        return LINUX_ARM_64;
                }
            case MAC_OS:
                switch (architecture) {
                    case X86_64:
                        return MACOS_X86_64;
                    case ARM_64:
                        return MACOS_ARM_64;
                }
            case WINDOWS:
                switch (architecture) {
                    case X86_64:
                        return WIN_X86_64;
                    case ARM_64:
                        return WIN_ARM_64;
                }
        }
        throw new IllegalStateException("Running on unsupported Platform: " + os.getCanonicalName() + "/" + architecture.getCanonicalName());
    }

    public static String getDetails() {
        return OS.getOS().getCanonicalName() + " / " +
                Architecture.getArchitecture().getCanonicalName() + " / " +
                Architecture.getBit() +
                " v." + OS.getOsVersion();
    }
}