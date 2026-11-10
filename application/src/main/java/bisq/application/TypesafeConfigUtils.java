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

import bisq.common.file.FileMutatorUtils;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValueFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
                Config config = parseCustomConfigFile(customConfigFilePath);
                log.info("Using custom config file: {}", customConfigFilePath.toAbsolutePath());
                return Optional.of(config);
            } catch (Exception e) {
                log.error("Could not load custom config file {}", customConfigFilePath.toAbsolutePath(), e);
            }
        }
        return Optional.empty();
    }

    /**
     * Parses a file as the start parses bisq.conf. The start ignores a bisq.conf for which this method throws, so
     * every check of whether the start takes a bisq.conf uses this method. The check of validity also throws for an
     * unresolved substitution.
     */
    private static Config parseCustomConfigFile(Path filePath) {
        Config config = ConfigFactory.parseFile(filePath.toFile());
        config.withFallback(ConfigFactory.defaultReference())
                .checkValid(ConfigFactory.defaultReference(), "application");
        return config;
    }

    /**
     * Appends one line {@code path=value} per entry to bisq.conf, where it overrides an earlier value of the path.
     * Unlike a rewrite of the file, it keeps the comments and includes of the user, and the user undoes it by
     * removing the lines. Bisq writes the new content into a temporary file next to bisq.conf, parses it as the start
     * parses bisq.conf, and only then moves it over bisq.conf, so that a failure leaves bisq.conf as it was.
     *
     * @param entries values which typesafe config takes, for example a boolean, a string or a list
     * @throws IOException if the file cannot be read or written, or if the start would not take bisq.conf with the
     *                     lines or would not get the values from it, for example because of a syntax error or a
     *                     substitution, so that the application would ignore the file and the lines with it
     */
    public static void appendToCustomConfig(Path appDataDirPath, Map<String, ?> entries) throws IOException {
        Path customConfigFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        // A bisq.conf which links to another file stays a link, and the other file gets the lines
        Path targetPath = Files.exists(customConfigFilePath) ? customConfigFilePath.toRealPath() : customConfigFilePath;
        String content = Files.exists(targetPath) ? Files.readString(targetPath, StandardCharsets.UTF_8) : "";
        String separator = content.isEmpty() || content.endsWith("\n") ? "" : "\n";
        String newContent = content + separator + String.join("\n", toConfigLines(entries)) + "\n";
        Path tempFilePath = Files.createTempFile(targetPath.toAbsolutePath().getParent(),
                ApplicationService.CUSTOM_CONFIG_FILE_NAME + ".", ".tmp");
        try {
            FileMutatorUtils.writeToPath(newContent, tempFilePath);
            // In the directory of bisq.conf, so that a relative include resolves as at the start
            Config newConfig = parseCustomConfigFile(tempFilePath);
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object expected = ConfigValueFactory.fromAnyRef(entry.getValue()).unwrapped();
                if (!expected.equals(newConfig.getValue(entry.getKey()).unwrapped())) {
                    throw new IOException(customConfigFilePath + " does not take the line " +
                            toConfigLines(Map.of(entry.getKey(), entry.getValue())).get(0));
                }
            }
            FileMutatorUtils.replaceFile(tempFilePath, targetPath);
        } catch (ConfigException e) {
            throw new IOException(customConfigFilePath + " is not valid: " + e.getMessage(), e);
        } finally {
            Files.deleteIfExists(tempFilePath);
        }
    }

    /**
     * @return the lines {@code path=value} which {@link #appendToCustomConfig} appends, sorted by path, for example
     * {@code a.b=false}, {@code a.c=""} and {@code a.d=[]}
     */
    public static List<String> toConfigLines(Map<String, ?> entries) {
        return new TreeMap<>(entries).entrySet().stream()
                .map(entry -> entry.getKey() + "=" +
                        ConfigValueFactory.fromAnyRef(entry.getValue()).render(ConfigRenderOptions.concise()))
                .toList();
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
