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

package bisq.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TypesafeConfigUtils {
    public static Config resolveFilteredJvmOptions() {
        Map<String, String> filteredJvmProps = new HashMap<>();
        System.getProperties().forEach((key, value) -> {
            if (key instanceof String keyAsString && keyAsString.startsWith("application.")) {
                filteredJvmProps.put(keyAsString, String.valueOf(value));
            }
        });
        return ConfigFactory.parseMap(filteredJvmProps);
    }

    public static Config parseArgsToConfig(String[] args) {
        Map<String, Object> map = Arrays.stream(args)
                .filter(arg -> arg.startsWith("--application.") && arg.contains("="))
                .map(arg -> arg.substring(2).split("=", 2))
                .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1], (a, b) -> b)); // last one wins if duplicates
        map.putAll(mapCustomArgsToTypesafeEntries(args));
        return ConfigFactory.parseMap(map);
    }

    private static Map<String, Object> mapCustomArgsToTypesafeEntries(String[] args) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            int valueIndex = i + 1;
            switch (args[i]) {
                case "--app-name":
                    if (valueIndex < args.length) {
                        map.put("application.appName", args[valueIndex]);
                        i++; // skip the value
                    }
                    break;
                case "--data-dir":
                    if (valueIndex < args.length) {
                        map.put("application.dataDir", args[valueIndex]);
                        i++;
                    }
                    break;
            }
        }
        return map;
    }

    public static Optional<Config> resolveCustomConfig(Path appDataDir) {
        // J8 compatible to avoid issues on mobile Samsung devices
        String configName = ApplicationService.CUSTOM_CONFIG_FILE_NAME;
        File customConfigFile = Paths.get(appDataDir.toString(), configName).toFile();
        if (customConfigFile.exists()) {
            try {
                com.typesafe.config.Config config = ConfigFactory.parseFile(customConfigFile);
                config.checkValid(ConfigFactory.defaultReference(), "application");
                log.info("Using custom config file: {}", customConfigFile.getAbsolutePath());
                return Optional.of(config);
            } catch (Exception e) {
                log.error("Could not load custom config file {}", customConfigFile.getAbsolutePath(), e);
            }
        }
        return Optional.empty();
    }
}
