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
        if(getSystemApplicationBaseDir()==null) {
            return null;
        }
        File sysAppBaseDir = getSystemApplicationBaseDir();
        File groupDir = new File(sysAppBaseDir.getAbsolutePath()+"/"+groupName);
        if(groupDir.exists() || (create && groupDir.mkdir())) {
            File appDir = new File(groupDir.getAbsolutePath()+"/"+appName);
            if(appDir.exists() || (create && appDir.mkdir())) {
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
        if(userHome!=null) {
            return new File(userHome);
        }
        File userHomeDir = null;
        String username = System.getProperty("user.name");
        if(username!=null) {
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
        if(getUserHomeDir()==null) {
            return null;
        }
        File userDataHome = null;
        if(System.getProperty("XDG_DATA_HOME")!=null) {
            userDataHome = new File(System.getProperty("Bisq2_DATA_HOME"));
        } else {
            File local = new File(getUserHomeDir().getAbsolutePath()+"/.local");
            if(local.exists() || (create && local.mkdir())) {
                userDataHome = new File(local.getAbsolutePath() + "/share");
            }
        }
        if(userDataHome!=null && (userDataHome.exists() || (create && userDataHome.mkdir()))) {
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
        if(getUserHomeDir()==null) {
            return null;
        }
        File userConfigHome = null;
        if(System.getProperty("XDG_CONFIG_HOME")!=null) {
            userConfigHome = new File(System.getProperty("Bisq2_CONFIG_HOME"));
        } else {
            userConfigHome = new File(getUserHomeDir().getAbsolutePath()+"/.config");
        }
        if(userConfigHome.exists() || (create && userConfigHome.mkdir())) {
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
        if(getUserHomeDir()==null) {
            return null;
        }
        File userConfigHome = null;
        if(System.getProperty("XDG_CACHE_HOME")!=null) {
            userConfigHome = new File(System.getProperty("Bisq2_CACHE_HOME"));
        } else {
            userConfigHome = new File(getUserHomeDir().getAbsolutePath()+"/.cache");
        }
        if(userConfigHome.exists() || (create && userConfigHome.mkdir())) {
            return userConfigHome;
        }
        return null;
    }

    public static File getUserAppHomeDir() throws IOException {
        if(getUserHomeDir()==null) {
            return null;
        }
        File userHomeDir = getUserHomeDir();
        File userAppHomeDir = null;
        if(osName.contains("linux")) {
            userAppHomeDir = userHomeDir;
            if(!userAppHomeDir.exists()) {
                throw new FileNotFoundException("User App Home Directory for Linux not found: "+userHomeDir.getAbsolutePath());
            }
        } else if(osName.contains("mac")) {
            userAppHomeDir = new File(userHomeDir, "Applications");
            if(!userAppHomeDir.exists()) {
                throw new FileNotFoundException("User App Home Directory for Mac not found: "+userHomeDir.getAbsolutePath()+"/Applications");
            }
        } else if(osName.contains("windows")) {
            userAppHomeDir = new File(userHomeDir.getAbsolutePath()+"\\AppData\\Local\\Programs");
            if(!userAppHomeDir.exists()) {
                throw new FileNotFoundException("User App Home Directory for Windows not found: "+userHomeDir.getAbsolutePath()+"\\AppData\\Local\\Programs");
            }
        }
        return userAppHomeDir;
    }

    public static File getUserAppHomeDir(String groupName, String appName, boolean create) throws IOException {
        if (getUserAppHomeDir() == null) {
            return null;
        }
        File userAppHomeDir = getUserAppHomeDir();
        File groupDir = new File(userAppHomeDir, groupName);
        if (groupDir.exists() || (create && groupDir.mkdir())) {
            File appDir = new File(groupDir, appName);
            if (appDir.exists() || (create && appDir.mkdir())) {
                return appDir;
            }
        }
        return null;
    }

    public static File getUserAppDataDir(String groupName, String appName, boolean create) throws IOException {
        if(getUserAppHomeDir()==null) {
            return null;
        }
        File userAppHomeDir = getUserAppHomeDir();
        File groupDir = new File(userAppHomeDir, groupName);
        if(groupDir.exists() || (create && groupDir.mkdir())) {
            File appDir = new File(groupDir, appName);
            if(appDir.exists() || (create && appDir.mkdir())) {
                return appDir;
            }
        }
        return null;
    }

    public static File getUserAppConfigDir(String groupName, String appName, boolean create) throws IOException {
        if(getUserAppHomeDir()==null) {
            return null;
        }
        File userAppHomeDir = getUserAppHomeDir();
        File groupDir = new File(userAppHomeDir, groupName);
        if(groupDir.exists() || (create && groupDir.mkdir())) {
            File appDir = new File(groupDir, appName);
            if(appDir.exists() || (create && appDir.mkdir())) {
                return appDir;
            }
        }
        return null;
    }

    public static File getUserAppCacheDir(String groupName, String appName, boolean create) throws IOException {
        if(getUserAppHomeDir()==null) {
            return null;
        }
        File userAppHomeDir = getUserAppHomeDir();
        File groupDir = new File(userAppHomeDir, groupName);
        if(groupDir.exists() || (create && groupDir.mkdir())) {
            File appDir = new File(groupDir, appName);
            if(appDir.exists() || (create && appDir.mkdir())) {
                return appDir;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String groupName = "1m5";
        String sysAppName = "core";
        String appName = "proxy";
        // * - System Application Base Directory (e.g. /usr/share): directory for all user installed shared multi-user applications
        print("System App Base", getSystemApplicationBaseDir());
        // * - System Application Directory (e.g. /usr/share/1m5/core):
        print("System App Dir", getSystemApplicationDir( groupName, sysAppName, true));
        // * - User Home Directory (e.g. /home/objectorange):
        print("User Home", getUserHomeDir());
        // * - User Data Directory (e.g. /home/objectorange/.local/share):
        print("User Data", getUserDataDir(true));
        // * - User Config Directory (e.g. /home/objectorange/.config):
        print("User Config", getUserConfigDir(true));
        // * - User Cache Directory (e.g. /home/objectorange/.cache):
        print("User Cache", getUserCacheDir(true));
        // * - User App Home Directory
        try {
            print("User App Home", getUserAppHomeDir());
            // * - User App Data Directory (e.g. /home/objectorange/.local/share/1m5/proxy):
            print("User App Data", getUserAppDataDir(groupName, appName, true));
            // * - User App Config Directory (e.g. /home/objectorange/.config/1m5/proxy):
            print("User App Config", getUserAppConfigDir(groupName, appName, true));
            // * - User App Cache Directory (.e.g /home/objectorange/.cache/1m5/proxy):
            print("User App Cache", getUserAppCacheDir(groupName, appName, true));
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private static void print(String message, File path) {
        if(path==null) {
            System.out.println(message + ": null");
        } else {
            System.out.println(message + ": " + path.getAbsolutePath());
        }
    }

}