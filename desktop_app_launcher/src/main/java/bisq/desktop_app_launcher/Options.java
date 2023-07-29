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

import bisq.common.util.JvmUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class Options {
    @Getter
    private final String[] args;
    private final Set<String> jvmOptions;

    public Options(String[] args) {
        this.args = args;
        jvmOptions = JvmUtils.getJvmOptions("application");
        Set<String> jvmOptionsFromArgs = JvmUtils.getJvmOptionsFromArgs(args, "application");
        JvmUtils.addToSystemProperties(jvmOptionsFromArgs);
        jvmOptions.addAll(jvmOptionsFromArgs);
    }

    Optional<String> getValue(String key) {
        String jvmOptionString = "-Dapplication." + key + "=";
        String argsOptionString = "--" + key + "=";
        return jvmOptions.stream()
                .filter(e -> e.startsWith(jvmOptionString))
                .map(e -> e.replace(jvmOptionString, ""))
                .findAny()
                .or(() -> Arrays.stream(args)
                        .filter(e -> e.startsWith(argsOptionString))
                        .map(e -> e.replace(argsOptionString, ""))
                        .findAny());
    }

    Optional<String> getAppName() {
        return getValue("appName");
    }

    Optional<String> getVersion() {
        return getValue("version");
    }

    Optional<Boolean> getValueAsBoolean(String key) {
        return getValue(key).map("true"::equalsIgnoreCase);
    }
}