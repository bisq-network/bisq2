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
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Provide standardized settings for cross platform system development.
 *
 * - System Application Base Directory (e.g. /usr/share): directory for all user installed shared multi-user applications
 *      - Linux: /opt   Mac: /Applications  Windows: C:\\Program Files
 * - System Application Directory (e.g. /usr/share/bisq2/proxy):
 * - User Home Directory (e.g. /home/username):
 * - User Data Directory (e.g. /home/username/.local/share):
 * - User Config Directory (e.g. /home/username/.config):
 * - User Cache Directory (e.g. /home/username/.cache):
 * - User App Data Directory (e.g. /home/username/.local/share/bisq2/proxy):
 * - User App Config Directory (e.g. /home/username/.config/bisq2/proxy):
 * - User App Cache Directory (.e.g /home/username/.cache/bisq2/proxy):
 *
 */
public class SystemSettings {
    public static String osName = System.getProperty("os.name").toLowerCase();

    public static File getSystemApplicationBaseDir() {
        File sysAppBaseDir = null;
        if (osName.contains("linux")) {
            sysAppBaseDir = new File("/usr/share");
        } else if (osName.contains("mac")) {
            sysAppBaseDir = new File("/Applications");
        } else if (osName.contains("windows")) {
            sysAppBaseDir = new File("C:\\\\Program Files");
        }
        return sysAppBaseDir;
    }

    public static File getSystemApplicationDir(String groupName, String appName, boolean create) {
        if (getSystemApplicationBaseDir() == null) {
            return null;
        }
        File sysAppBaseDir = getSystemApplicationBaseDir();
        File groupDir = new File(sysAppBaseDir.getAbsolutePath() + "/" + groupName);
        if (groupDir.exists() || (create && groupDir.mkdirs())) {
            File appDir = new File(groupDir.getAbsolutePath() + "/" + appName);
            if (appDir.exists() || (create && appDir.mkdirs())) {
                return appDir;
            }
        }
        return null;
    }

    /**
     * User Home Directory
     * Linux: /home/username
     * Mac: /Users/username
     * Windows: c:\\\\Users\\username
     * @return java.io.File user home directory or null if System property user.name does not exist
     */
    public static File getUserHomeDir() {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            return new File(userHome);
        }
        File userHomeDir = null;
        String username = System.getProperty("user.name");
        if (username != null) {
            if (osName.contains("linux")) {
                userHomeDir = new File("/home/" + username);
            } else if (osName.contains("mac")) {
                userHomeDir = new File("/Users/" + username);
            } else if (osName.contains("windows")) {
                userHomeDir = new File("c:\\\\Users\\" + username);
            }
        }
        return userHomeDir;
    }

    /**
     * User data directory from XDG_DATA_HOME environment variable if present otherwise
     * the .local/share directory within the user directory above. If that directory
     * does not exist and create=true, it will attempt to create it.
     * @param create
     * @return
     */
    public static File getUserDataDir(boolean create) {
        if (getUserHomeDir() == null) {
            return null;
        }
        File userDataHome = null;
        if (System.getenv("XDG_DATA_HOME") != null) {
            userDataHome = new File(System.getenv("XDG_DATA_HOME"));
        } else {
            File local = new File(getUserHomeDir().getAbsolutePath() + "/.local");
            if (local.exists() || (create && local.mkdir())) {
                userDataHome = new File(local.getAbsolutePath() + "/share");
            }
        }
        if (userDataHome != null && (userDataHome.exists() || (create && userDataHome.mkdir()))) {
            return userDataHome;
        }
        return null;
    }

    /**
     * User config directory from XDG_CONFIG_HOME environment variable if present otherwise
     * the .config directory within the user directory above. If that directory
     * does not exist and create=true, it will attempt to create it.
     * @param create
     * @return
     */
    public static File getUserConfigDir(boolean create) {
        if (getUserHomeDir() == null) {
            return null;
        }
        File userConfigHome = null;
        if (System.getenv("XDG_CONFIG_HOME") != null) {
            userConfigHome = new File(System.getenv("XDG_CONFIG_HOME"));
        } else {
            userConfigHome = new File(getUserHomeDir().getAbsolutePath() + "/.config");
        }
        if (userConfigHome.exists() || (create && userConfigHome.mkdir())) {
            return userConfigHome;
        }
        return null;
    }

    /**
     * User cache directory from XDG_CACHE_HOME environment variable if present otherwise
     * the .cache directory within the user directory above. If that directory
     * does not exist and create=true, it will attempt to create it.
     * @param create
     * @return
     */
    public static File getUserCacheDir(boolean create) {
        if (getUserHomeDir() == null) {
            return null;
        }
        File userConfigHome = null;
        if (System.getenv("XDG_CACHE_HOME") != null) {
            userConfigHome = new File(System.getenv("XDG_CACHE_HOME"));
        } else {
            userConfigHome = new File(getUserHomeDir().getAbsolutePath() + "/.cache");
        }
        if (userConfigHome.exists() || (create && userConfigHome.mkdir())) {
            return userConfigHome;
        }
        return null;
    }

    public static File getUserAppHomeDir() throws IOException {
        if (getUserHomeDir() == null) {
            return null;
        }
        File userHomeDir = getUserHomeDir();
        File userAppHomeDir = null;
        if (osName.contains("linux")) {
            userAppHomeDir = userHomeDir;
            if (!userAppHomeDir.exists()) {
                throw new FileNotFoundException("User App Home Directory for Linux not found: " + userHomeDir.getAbsolutePath());
            }
        } else if (osName.contains("mac")) {
            userAppHomeDir = new File(userHomeDir, "Applications");
            if (!userAppHomeDir.exists()) {
                throw new FileNotFoundException("User App Home Directory for Mac not found: " + userHomeDir.getAbsolutePath() + "/Applications");
            }
        } else if (osName.contains("windows")) {
            userAppHomeDir = new File(userHomeDir.getAbsolutePath() + "\\AppData\\Local\\Programs");
            if (!userAppHomeDir.exists()) {
                throw new FileNotFoundException("User App Home Directory for Windows not found: " + userHomeDir.getAbsolutePath() + "\\AppData\\Local\\Programs");
            }
        }
        return userAppHomeDir;
    }

    public static File getUserAppDataDir(String groupName, String appName, boolean create) throws IOException {
        if (getUserDataDir(create) == null) {
            return null;
        }
        File userDataDir = getUserDataDir(create);
        File groupDir = new File(userDataDir, groupName);
        if (groupDir.exists() || (create && groupDir.mkdir())) {
            File appDir = new File(groupDir, appName);
            if (appDir.exists() || (create && appDir.mkdir())) {
                return appDir;
            }
        }
        return null;
    }

    public static File getUserAppConfigDir(String groupName, String appName, boolean create) throws IOException {
        if (getUserConfigDir(create) == null) {
            return null;
        }
        File userConfigDir = getUserConfigDir(create);
        File groupDir = new File(userConfigDir, groupName);
        if (groupDir.exists() || (create && groupDir.mkdir())) {
            File appDir = new File(groupDir, appName);
            if (appDir.exists() || (create && appDir.mkdir())) {
                return appDir;
            }
        }
        return null;
    }

    public static File getUserAppCacheDir(String groupName, String appName, boolean create) throws IOException {
        if (getUserCacheDir(create) == null) {
            return null;
        }
        File userCacheDir = getUserCacheDir(create);
        File groupDir = new File(userCacheDir, groupName);
        if (groupDir.exists() || (create && groupDir.mkdir())) {
            File appDir = new File(groupDir, appName);
            if (appDir.exists() || (create && appDir.mkdir())) {
                return appDir;
            }
        }
        return null;
    }
}
