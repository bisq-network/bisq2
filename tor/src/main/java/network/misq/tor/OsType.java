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

package network.misq.tor;

import network.misq.common.util.OsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.misq.common.util.FileUtils.FILE_SEP;
import static network.misq.tor.Constants.*;

public enum OsType {
    WIN,
    LINUX_32,
    LINUX_64,
    OSX;

    private static final Logger log = LoggerFactory.getLogger(OsType.class);

    public static OsType getOsType() {
        if (OsUtils.isWindows()) {
            return WIN;
        } else if (OsUtils.isOSX()) {
            return OSX;
        } else if (OsUtils.isLinux32()) {
            return LINUX_32;
        } else if (OsUtils.isLinux64()) {
            return LINUX_64;
        } else {
            throw new RuntimeException("Not supported OS: " + OsUtils.getOSName() + " / " + OsUtils.getOSArchitecture());
        }
    }

    public String getArchiveName() {
        return FILE_SEP + NATIVE_DIR + FILE_SEP + getOsDir() + FILE_SEP + TOR_ARCHIVE;
    }

    public String getTorrcNative() {
        return FILE_SEP + getOsDir(false) + FILE_SEP + TORRC_NATIVE;
    }

    public String getBinaryName() {
        switch (this) {
            case WIN:
                return "tor.exe";
            case LINUX_32:
            case LINUX_64:
                return "tor";
            case OSX:
                return "tor.real";
            default:
                throw new RuntimeException("Not supported OS " + this);
        }
    }

    private String getOsDir() {
        return getOsDir(true);
    }

    private String getOsDir(boolean distinctArch) {
        switch (this) {
            case WIN:
                return WIN_DIR;
            case LINUX_32:
                return distinctArch ? LINUX32_DIR : LINUX_DIR;
            case LINUX_64:
                return distinctArch ? LINUX64_DIR : LINUX_DIR;
            case OSX:
                return distinctArch ? OSX_DIR_64 : OSX_DIR;
            default:
                throw new RuntimeException("Not supported OS " + this);
        }
    }
}
