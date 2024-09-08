package bisq.wallets.regtest;

import lombok.Getter;

import java.util.Locale;

public enum Os {
    LINUX("linux"),
    MAC_OS("macos"),
    WINDOWS("win");

    @Getter
    private final String canonicalName;

    Os(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public static bisq.common.platform.OS getOS() {
        String osName = getOsName();
        if (isLinux(osName)) {
            return bisq.common.platform.OS.LINUX;
        } else if (isMacOs(osName)) {
            return bisq.common.platform.OS.MAC_OS;
        } else if (isWindows(osName)) {
            return bisq.common.platform.OS.WINDOWS;
        }
        throw new IllegalStateException("Running on unsupported OS: " + osName);
    }

    public static boolean isLinux() {
        return isLinux(getOsName());
    }

    public static boolean isLinux(String osName) {
        return osName.contains("linux");
    }

    public static boolean isMacOs() {
        return isMacOs(getOsName());
    }

    public static boolean isMacOs(String osName) {
        return osName.contains("mac") || osName.contains("darwin");
    }

    public static boolean isWindows() {
        return isWindows(getOsName());
    }

    public static boolean isWindows(String osName) {
        return osName.contains("win");
    }

    public static String getOsName() {
        return System.getProperty("os.name").toLowerCase(Locale.US);
    }

    public static String getOsVersion() {
        return System.getProperty("os.version");
    }
}