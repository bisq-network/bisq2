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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Getter
public class Options {
    private final String[] args;
    private final List<String> jvmArgs;

    public Options(String[] args) {
        this.args = args;
        jvmArgs = getJvmArgs();
    }

    List<String> getJvmArgs() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(e -> e.startsWith("-Dapplication"))
                .collect(Collectors.toList());

        // If we start from a binary we pass the JVM args as program arguments and add it as system properties, to make 
        // them available for typesafe config. The jvmArgs from the ManagementFactory.getRuntimeMXBean().getInputArguments()
        // call would be empty in that case.
        Stream.of(args)
                .filter(e -> e.startsWith("-Dapplication"))
                .forEach(e -> {
                    try {
                        String[] pair = e.split("=");
                        String key = pair[0].replace("-D", "");
                        System.setProperty(key, pair[1]);
                        jvmArgs.add(e);
                    } catch (Exception exception) {
                        log.error("error at parsing argument {}", e);
                    }
                });
        return jvmArgs;
    }

    Optional<String> getValue(String key) {
        String jvmOptionString = "-Dapplication." + key + "=";
        String argsOptionString = "--" + key + "=";
        return jvmArgs.stream()
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