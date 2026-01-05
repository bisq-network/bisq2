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

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TypesafeConfigUtils {
    public static Config resolveFilteredJvmOptions() {
        Map<String, Object> filteredJvmProps = new HashMap<>();
        System.getProperties().forEach((key, value) -> {
            if (key instanceof String keyAsString && keyAsString.startsWith("application.")) {
                filteredJvmProps.put(keyAsString, coerce(String.valueOf(value)));
            }
        });
        return ConfigFactory.parseMap(filteredJvmProps);
    }


    public static Config parseArgsToConfig(String[] args) {
        Map<String, Object> map = Arrays.stream(args)
                .filter(arg -> arg.startsWith("--application.") && arg.contains("="))
                .map(arg -> arg.substring(2).split("=", 2))
                .collect(Collectors.toMap(arr -> arr[0], arr -> coerce(arr[1]), (a, b) -> b)); // last one wins if duplicates
        map.putAll(mapCustomArgsToTypesafeEntries(args));
        return ConfigFactory.parseMap(map);
    }

    public static Optional<Config> resolveCustomConfig(Path appDataDirPath) {
        // J8 compatible to avoid issues on mobile Samsung devices
        String configName = ApplicationService.CUSTOM_CONFIG_FILE_NAME;
        Path customConfigFilePath = appDataDirPath.resolve(configName);
        if (Files.exists(customConfigFilePath)) {
            try {
                com.typesafe.config.Config config = ConfigFactory.parseFile(customConfigFilePath.toFile());
                config.withFallback(ConfigFactory.defaultReference()).checkValid(ConfigFactory.defaultReference(), "application");
                log.info("Using custom config file: {}", customConfigFilePath.toAbsolutePath());
                return Optional.of(config);
            } catch (Exception e) {
                log.error("Could not load custom config file {}", customConfigFilePath.toAbsolutePath(), e);
            }
        }
        return Optional.empty();
    }

    // Accept both formats: "--app-name=bisq" and "--app-name bisq"
    @VisibleForTesting
    static Map<String, Object> mapCustomArgsToTypesafeEntries(String[] args) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            int valueIndex = i + 1;
            String arg = args[i];
            if (arg.startsWith("--app-name=")) {
                map.put("application.appName", arg.substring("--app-name=".length()));
                continue;
            }
            if (arg.startsWith("--data-dir=")) {
                map.put("application.baseDir", arg.substring("--data-dir=".length()));
                continue;
            }
            if (arg.startsWith("--base-dir=")) {
                map.put("application.baseDir", arg.substring("--base-dir=".length()));
                continue;
            }
            switch (arg) {
                case "--app-name":
                    if (valueIndex < args.length && !args[valueIndex].startsWith("--")) {
                        map.put("application.appName", args[valueIndex]);
                        i++;
                    }
                    break;
                case "--data-dir":
                    if (valueIndex < args.length && !args[valueIndex].startsWith("--")) {
                        map.put("application.baseDir", args[valueIndex]);
                        i++;
                    }
                    break;
                case "--base-dir":
                    if (valueIndex < args.length && !args[valueIndex].startsWith("--")) {
                        map.put("application.baseDir", args[valueIndex]);
                        i++;
                    }
                    break;
            }
        }
        return map;
    }

    @VisibleForTesting
    static Object coerce(String raw) {
        String stringValue = raw.trim();
        if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
            return Boolean.parseBoolean(stringValue);
        }
        try {
            if (stringValue.matches("[-+]?\\d+")) {
                long longValue = Long.parseLong(stringValue);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                }
                return longValue;
            }
        } catch (NumberFormatException ignore) {
        }
        try {
            if (stringValue.matches("[-+]?\\d*\\.\\d+([eE][-+]?\\d+)?")) {
                return Double.parseDouble(stringValue);
            }
        } catch (NumberFormatException ignore) {
        }
        return raw;
    }
}
