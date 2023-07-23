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
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import bisq.desktop_app.DesktopApp;
import bisq.security.PgPUtils;
import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * We ship the binary with the current version of the DesktopApp and with the JRE.
 * If there is a jar file found for the given version at the data directory we use that jar file to start a new
 * java process with the new jar file. Otherwise, we use the provided DesktopApp.
 * The `java.home` system property is pointing to the provided JRE from the binary.
 */
@Slf4j
public class DesktopAppLauncher {
    private static final String DEFAULT_APP_NAME = "Bisq2";
    private static final String DEFAULT_VERSION = "2.0.0";
    private static final String VERSION_FILE = "version.txt";
    private static final String JAR_DIR = "jar";
    private static final String JAR_FILE_NAME = "desktop.jar";
    private static final String BIN_PATH = "bin/java";


    public static void main(String[] args) {
        try {
            List<String> jvmArgs = getBisqJvmArgs();
            String appName = getAppName(args, jvmArgs);
            String userDataDir = OsUtils.getUserDataDir().getAbsolutePath();
            String dataDir = userDataDir + File.separator + appName;

            LogSetup.setup(Paths.get(dataDir, "bisq").toString());
            LogSetup.setLevel(Level.INFO);

            String version = getVersion(args, jvmArgs, userDataDir);
            String pathToJar = dataDir + File.separator + JAR_DIR + File.separator + version;
            String jarPath = pathToJar + File.separator + JAR_FILE_NAME;
            if (new File(jarPath).exists()) {
                boolean ignoreSignature = getOption(args, jvmArgs, "ignoreSignature", "false").equals("true");
                if (!ignoreSignature) {
                    // If keys are provided as: --keyList=key1,key2
                    String keys = getOption(args, jvmArgs, "keyList", null);
                    if (keys != null) {
                        List<String> keyList = List.of(keys.split(","));
                        PgPUtils.verifyDownloadedFile(pathToJar, JAR_FILE_NAME, keyList);
                    } else {
                        List<String> keyList = List.of(PgPUtils.KEY_4A133008, PgPUtils.KEY_E222AA02);
                        PgPUtils.checkIfKeysMatchesResourceKeys(pathToJar, keyList);
                        PgPUtils.verifyDownloadedFile(pathToJar, JAR_FILE_NAME, keyList);
                    }
                }

                String javaHome = System.getProperty("java.home");
                log.info("javaHome {}", javaHome);
                log.info("Jar file found at {}. Start that application in a new process.", jarPath);
                String javaBinPath = javaHome + File.separator + BIN_PATH;
                List<String> command = new ArrayList<>();
                command.add(javaBinPath);
                command.addAll(jvmArgs);
                command.add("-jar");
                command.add(jarPath);
                command.addAll(Arrays.asList(args));
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.environment().put("JAVA_HOME", javaHome);
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                log.info("Exited with code: {}", exitCode);
            } else {
                log.info("No jar file found at {}. Run default application.", jarPath);
                DesktopApp.main(args);
            }
        } catch (Exception e) {
            log.error("Error at launch: {}", ExceptionUtil.print(e));
        }
    }


    private static String getVersion(String[] args, List<String> jvmArgs, String userDataDir) {
        String versionFilePath = userDataDir + File.separator + VERSION_FILE;
        return FileUtils.readFromFileIfPresent(new File(versionFilePath))
                .orElse(getOption(args, jvmArgs, "version", DEFAULT_VERSION));
    }

    private static String getAppName(String[] args, List<String> jvmArgs) {
        return getOption(args, jvmArgs, "appName", DEFAULT_APP_NAME);
    }

    private static String getOption(String[] args, List<String> jvmArgs, String option, String defaultValue) {
        String jvmOptionString = "-Dapplication." + option + "=";
        String argsOptionString = "--" + option + "=";
        return jvmArgs.stream()
                .filter(e -> e.startsWith(jvmOptionString))
                .map(e -> e.replace(jvmOptionString, ""))
                .findAny()
                .or(() -> Arrays.stream(args)
                        .filter(e -> e.startsWith(argsOptionString))
                        .map(e -> e.replace(argsOptionString, ""))
                        .findAny())
                .orElse(defaultValue);
    }


    private static List<String> getBisqJvmArgs() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(e -> e.startsWith("-Dapplication"))
                .collect(Collectors.toList());
    }
}