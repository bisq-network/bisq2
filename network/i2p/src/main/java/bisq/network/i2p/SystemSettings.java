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

package bisq.network.i2p;

import java.io.File;
import java.io.IOException;

public class SystemSettings {
    private final File i2pDirPath;

    public SystemSettings(String i2pDirPath) {
        this.i2pDirPath = new File(i2pDirPath);
        try {
            if (!this.i2pDirPath.exists() && !this.i2pDirPath.mkdirs()) {
                throw new IOException("Unable to create base directory: " + this.i2pDirPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize SystemSettings base dir", e);
        }
    }

    public File getUserHomeDir() {
        return ensureDir("");
    }

    public File getUserDataDir() {
        return ensureDir("data");
    }

    public File getUserConfigDir() {
        return ensureDir("config");
    }

    public File getUserCacheDir() {
        return ensureDir("cache");
    }

    /**
     * Application-specific Data Directory, inside i2pDirPath/data/<groupName>/<appName>
     */
    public File getUserAppDataDir(String groupName, String appName) {
        File groupDir = new File(getUserDataDir(), groupName);
        File appDir = new File(groupDir, appName);
        return ensureDir(appDir);
    }

    /**
     * Application-specific Config Directory, inside i2pDirPath/config/<groupName>/<appName>
     */
    public File getUserAppConfigDir(String groupName, String appName) {
        File groupDir = new File(getUserConfigDir(), groupName);
        File appDir = new File(groupDir, appName);
        return ensureDir(appDir);
    }

    /**
     * Application-specific Cache Directory, inside i2pDirPath/cache/<groupName>/<appName>
     */
    public File getUserAppCacheDir(String groupName, String appName) {
        File groupDir = new File(getUserCacheDir(), groupName);
        File appDir = new File(groupDir, appName);
        return ensureDir(appDir);
    }

    /**
     * Ensure the named subdirectory of i2pDirPath exists.
     */
    private File ensureDir(String name) {
        File dir = new File(i2pDirPath, name);
        return ensureDir(dir);
    }

    /**
     * Ensure the given File path exists.
     */
    private File ensureDir(File dir) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Unable to create directory: " + dir.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + dir, e);
        }
        return dir;
    }
}
