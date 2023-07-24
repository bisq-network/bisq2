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
import bisq.update.Verification;
import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bisq.update.Utils.*;


/**
 * We ship the binary with the current version of the DesktopApp and with the JRE.
 * If there is a jar file found for the given version at the data directory we use that jar file to start a new
 * java process with the new jar file. Otherwise, we use the provided DesktopApp.
 * The `java.home` system property is pointing to the provided JRE from the binary.
 */
@Slf4j
public class DesktopAppLauncher {
    static final String VERSION = "2.0.0";
    static final String APP_NAME = "Bisq2";
    private static final String BIN_PATH = "bin/java";

    private static String[] args;
    private static List<String> jvmArgs;
    private static String version, directory, jarPath;

    public static void main(String[] args) {
        DesktopAppLauncher.args = args;
        try {
            jvmArgs = Options.getBisqJvmArgs();
            String appName = Options.getAppName(args, jvmArgs);
            String userDataDir = OsUtils.getUserDataDir().getAbsolutePath();
            String dataDir = userDataDir + File.separator + appName;

            LogSetup.setup(Paths.get(dataDir, "bisq").toString());
            LogSetup.setLevel(Level.INFO);

            version = Options.getVersion(args, jvmArgs, userDataDir);
            directory = dataDir + File.separator + DESTINATION_DIR + File.separator + version;
            jarPath = directory + File.separator + FILE_NAME;
            if (new File(jarPath).exists()) {
                launchNewProcess();
            } else {
                log.info("No jar file found. Run default Bisq application with version Ï" + VERSION);
                DesktopApp.main(args);
            }
        } catch (Exception e) {
            log.error("Error at launch: {}", ExceptionUtil.print(e));
        }
    }

    private static void launchNewProcess() throws IOException, InterruptedException {
        boolean ignoreSignatureVerification = Options.getOptionValueAsBoolean(args, jvmArgs, "ignoreSignatureVerification", "false");
        boolean ignoreSigningKeyInResourcesCheck = Options.getOptionValueAsBoolean(args, jvmArgs, "ignoreSigningKeyInResourcesCheck", "false");
        if (!ignoreSignatureVerification) {
            // If keyIds are provided as: --keyIds=keyId1,keyId2
            String keyIds = Options.getOptionValue(args, jvmArgs, "keyIds", null);
            if (keyIds != null) {
                List<String> keyList = List.of(keyIds.split(","));
                Verification.verifyDownloadedFile(directory, keyList, ignoreSigningKeyInResourcesCheck);
            } else {
                Verification.verifyDownloadedFile(directory, List.of(KEY_4A133008, KEY_E222AA02), ignoreSigningKeyInResourcesCheck);
            }
        }

        String javaHome = System.getProperty("java.home");
        log.debug("javaHome {}", javaHome);
        log.debug("Jar file found at {}", jarPath);
        log.info("Update found. Start Bisq v{} in a new process.", version);
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
        log.info("Exited launcher process with code: {}", exitCode);
    }
}