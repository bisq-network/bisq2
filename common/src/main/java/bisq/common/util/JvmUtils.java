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

package bisq.common.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class JvmUtils { // Make program arguments which starts with -Dapplication via System.setProperty accessible as jvm arguments.
    // Useful to pass jvm args as program arguments to a binary (cannot pass jvm options if it's not started by java process).
    public static Set<String> getJvmOptionsFromArgs(String[] args, String prefix) {
        return Stream.of(args)
                .filter(e -> e.startsWith("-D" + prefix))
                .collect(Collectors.toSet());
    }

    public static void addToSystemProperties(Set<String> systemProperties) {
        systemProperties.stream()
                .filter(e -> e.startsWith("-D"))
                .forEach(e -> {
                    try {
                        String[] pair = e.split("=");
                        String key = pair[0].replace("-D", "");
                        System.setProperty(key, pair[1]);
                    } catch (Exception exception) {
                        log.error("error at parsing argument {}", e);
                    }
                });
    }


    public static Set<String> getJvmOptions(String prefix) {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(e -> e.startsWith("-D" + prefix))
                .collect(Collectors.toSet());

    }
}