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

package bisq.desktop_app_launcher;

import bisq.common.logging.LogSetup;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.OsUtils;
import bisq.desktop_app.DesktopApp;
import bisq.updater.DownloadedFilesVerification;
import bisq.updater.UpdaterUtils;
import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static bisq.updater.UpdaterUtils.UPDATES_DIR;
import static bisq.updater.UpdaterUtils.readVersionFromVersionFile;

/**
 * We ship the binary with the current version of the DesktopApp and with the JRE.
 * If there is a version file found in the data dir we read it and look up for the jar file for that version.
 * If it exists we start a new java process with that jar file. Otherwise, we call the main method on the provided DesktopApp.
 * <p>
 * The `java.home` system property is pointing to the provided JRE from the binary.
 * <p>
 * We do not use the typesafe config framework for the DesktopAppLauncher to keep
 * complexity and dependencies at a minimum. There are only 3 options which are used by the DesktopAppLauncher itself.
 * <p>
 * All JVM options and program arguments are forwarded to the Desktop application. As we cannot pass JVM arguments
 * to an executable, we add all program arguments starting with `-Dapplication` with `System.setProperty` as JVM options.
 * <p>
 * DesktopAppLauncher specific options can be set as JVM option or as program arguments:
 * <p>
 * --ignoreSignatureVerification=true|false (default false) OR -Dapplication.ignoreSignatureVerification
 * --ignoreSigningKeyInResourcesCheck=true|false (default false) OR -Dapplication.ignoreSigningKeyInResourcesCheck
 * --keyIds=keyId1,keyId2 (comma separated list of keyIds; default null)OR -Dapplication.keyIds
 */
@Slf4j
public class DesktopAppLauncher {
    private static final String VERSION = "2.0.2";
    private static final String APP_NAME = "Bisq2";
    private static final String FINGER_PRINT_ALEJANDRO_GARCIA = "E222AA02"; // B493 3191 06CC 3D1F 252E  19CB F806 F422 E222 AA02
    private static final String FINGER_PRINT_HENRIK_JANNSEN = "387C8307"; // B8A5 D214 ADFA A387 A14C  8BCF 02AA 2BAE 387C 8307
    private static final List<String> KEY_IDS = List.of(FINGER_PRINT_ALEJANDRO_GARCIA, FINGER_PRINT_HENRIK_JANNSEN);

    private final Options options;

    public static void main(String[] args) {
        try {
            new DesktopAppLauncher(args);
        } catch (Exception e) {
            log.error("Error at launch", e);
            System.err.println("Error at launch: " + ExceptionUtil.getStackTraceAsString(e));
        }
    }

    private final String updatesDir, jarPath, jarFileName;

    private DesktopAppLauncher(String[] args) throws Exception {
        options = new Options(args);
        String appName = options.getAppName().orElse(DesktopAppLauncher.APP_NAME);
        String appDataDir = OsUtils.getUserDataDir().resolve(appName).toAbsolutePath().toString();
        LogSetup.setup(Paths.get(appDataDir, "bisq").toString());
        LogSetup.setLevel(Level.INFO);
        String version = UpdaterUtils.readVersionFromVersionFile(appDataDir)
                .or(options::getVersion)
                .orElse(DesktopAppLauncher.VERSION);
        updatesDir = appDataDir + File.separator + UPDATES_DIR + File.separator + version;
        jarFileName = UpdaterUtils.getJarFileName(version);
        jarPath = updatesDir + File.separator + jarFileName;

        if (new File(jarPath).exists()) {
            boolean ignoreSignatureVerification = options.getValueAsBoolean("ignoreSignatureVerification").orElse(false);
            if (ignoreSignatureVerification) {
                log.warn("Signature verification is disabled by the provided program argument. This is not recommended and should be done only if the user can ensure that the jar file is trusted.");
            } else {
                verifyJarFile();
            }
            invokeJar();
        } else {
            Optional<String> fromVersionFile = readVersionFromVersionFile(appDataDir);
            if (fromVersionFile.isPresent() && !fromVersionFile.get().equals(VERSION)) {
                Optional<String> versionFromArgs = options.getValue("version");
                log.warn("We found a version file with version {} but it does not match our version. versionFromArgs={}; DesktopAppLauncher.VERSION={}; ",
                        fromVersionFile.get(), versionFromArgs, VERSION);
            }
            log.info("No jar file found. Run default Bisq application with version " + VERSION);
            DesktopApp.main(args);
        }
    }

    private void verifyJarFile() throws IOException {
        boolean ignoreSigningKeyInResourcesCheck = options.getValueAsBoolean("ignoreSigningKeyInResourcesCheck").orElse(false);
        List<String> keyList = options.getValue("keyIds")
                .map(keyIds -> List.of(keyIds.split(",")))
                .orElse(KEY_IDS);
        DownloadedFilesVerification.verify(updatesDir, jarFileName, keyList, ignoreSigningKeyInResourcesCheck);
    }

    private void invokeJar() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        URL url = Path.of(jarPath).toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, DesktopAppLauncher.class.getClassLoader().getParent());
        // Need to set contextClassLoader to load class from jar file
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> clazz = classLoader.loadClass(DesktopApp.class.getCanonicalName());
        Method mainMethod = clazz.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object) options.getArgs());
    }
}