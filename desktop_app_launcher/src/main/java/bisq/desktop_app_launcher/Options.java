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

import bisq.common.util.FileUtils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static bisq.update.Utils.VERSION_FILE_NAME;

public class Options {
    static String getVersion(String[] args, List<String> jvmArgs, String userDataDir) {
        String versionFilePath = userDataDir + File.separator + VERSION_FILE_NAME;
        return FileUtils.readFromFileIfPresent(new File(versionFilePath))
                .orElse(getOptionValue(args, jvmArgs, "version", DesktopAppLauncher.VERSION));
    }

    static String getAppName(String[] args, List<String> jvmArgs) {
        return getOptionValue(args, jvmArgs, "appName", DesktopAppLauncher.APP_NAME);
    }

    static List<String> getBisqJvmArgs() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(e -> e.startsWith("-Dapplication"))
                .collect(Collectors.toList());
    }

    static String getOptionValue(String[] args, List<String> jvmArgs, String option, String defaultValue) {
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

    static boolean getOptionValueAsBoolean(String[] args, List<String> jvmArgs, String option, String defaultValue) {
        return "true".equalsIgnoreCase(getOptionValue(args, jvmArgs, option, defaultValue));
    }
}