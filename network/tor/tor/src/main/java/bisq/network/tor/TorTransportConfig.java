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

package bisq.network.tor;

import bisq.common.network.TransportConfig;
import bisq.network.tor.common.torrc.DirectoryAuthority;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class TorTransportConfig implements TransportConfig {
    public static TorTransportConfig from(Path dataDirPath, com.typesafe.config.Config config) {
        return new TorTransportConfig(
                dataDirPath,
                config.hasPath("defaultNodePort") ? config.getInt("defaultNodePort") : -1,
                (int) TimeUnit.SECONDS.toMillis(config.getInt("bootstrapTimeout")),
                (int) TimeUnit.SECONDS.toMillis(config.getInt("hsUploadTimeout")),
                (int) TimeUnit.SECONDS.toMillis(config.getInt("socketTimeout")),
                config.getBoolean("testNetwork"),
                parseDirectoryAuthorities(config.getList("directoryAuthorities")),
                parseTorrcOverrideConfig(config.getConfig("torrcOverrides")),
                config.hasPath("torrcOverrideFilePath") ? config.getString("torrcOverrideFilePath") : "",
                config.getInt("sendMessageThrottleTime"),
                config.getInt("receiveMessageThrottleTime"),
                config.getBoolean("useExternalTor")
        );
    }

    private static Set<DirectoryAuthority> parseDirectoryAuthorities(ConfigList directoryAuthoritiesConfig) {
        Set<DirectoryAuthority> allDirectoryAuthorities = new HashSet<>();
        directoryAuthoritiesConfig.forEach(authConfig -> {
            DirectoryAuthority directoryAuthority = DirectoryAuthority.builder()
                    .nickname(getStringFromConfigValue(authConfig, "nickname"))
                    .orPort(Integer.parseInt(getStringFromConfigValue(authConfig, "orPort")))
                    .v3Ident(getStringFromConfigValue(authConfig, "v3Ident"))
                    .dirPort(Integer.parseInt(getStringFromConfigValue(authConfig, "dirPort")))
                    .relayFingerprint(getStringFromConfigValue(authConfig, "relayFingerprint"))
                    .build();

            allDirectoryAuthorities.add(directoryAuthority);
        });
        return allDirectoryAuthorities;
    }

    private static Map<String, List<String>> parseTorrcOverrideConfig(com.typesafe.config.Config torrcOverrides) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        torrcOverrides.entrySet().forEach(entry -> {
            Object unwrapped = entry.getValue().unwrapped();
            if (unwrapped instanceof List<?> values) {
                List<String> stringValues = new ArrayList<>();
                values.forEach(v -> stringValues.add(String.valueOf(v)));
                result.put(entry.getKey(), stringValues);
            } else {
                result.put(entry.getKey(), List.of(String.valueOf(unwrapped)));
            }
        });
        return result;
    }

    /**
     * Parses a torrc-style override file into a map of key → list of values.
     * Each non-blank, non-comment line is expected to have the form {@code Key Value}.
     * Repeated keys (e.g. multiple {@code Bridge} lines) accumulate into a list so that
     * all entries appear in the generated torrc.
     */
    public static Map<String, List<String>> parseTorrcOverrideFile(Path filePath) throws IOException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(filePath)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int spaceIndex = -1;
            for (int i = 0; i < trimmed.length(); i++) {
                if (Character.isWhitespace(trimmed.charAt(i))) {
                    spaceIndex = i;
                    break;
                }
            }
            if (spaceIndex < 0) {
                continue; // bare key with no value — skip
            }
            String key = trimmed.substring(0, spaceIndex);
            String value = trimmed.substring(spaceIndex + 1).strip();
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return result;
    }

    private static String getStringFromConfigValue(ConfigValue configValue, String key) {
        return (String) configValue
                .atKey(key)
                .getObject(key)
                .get(key)
                .unwrapped();
    }

    private final Path dataDirPath;
    private final int defaultNodePort;
    private final int bootstrapTimeout; // in ms
    private final int hsUploadTimeout; // in ms
    private final int socketTimeout; // in ms
    private final boolean isTestNetwork;
    private final Set<DirectoryAuthority> directoryAuthorities;
    private final Map<String, List<String>> torrcOverrides;
    /**
     * Optional path to a torrc-style file whose entries override {@code torrcOverrides}.
     * Supports both absolute paths and paths relative to the data directory.
     * When non-empty this file takes precedence over the inline {@code torrcOverrides} map.
     * Leave empty (the default) to use {@code torrcOverrides} instead.
     */
    private final String torrcOverrideFilePath;
    private final int sendMessageThrottleTime;
    private final int receiveMessageThrottleTime;
    private final boolean useExternalTor;

    public TorTransportConfig(Path dataDirPath,
                              int defaultNodePort,
                              int bootstrapTimeout,
                              int hsUploadTimeout,
                              int socketTimeout,
                              boolean isTestNetwork,
                              Set<DirectoryAuthority> directoryAuthorities,
                              Map<String, List<String>> torrcOverrides,
                              String torrcOverrideFilePath,
                              int sendMessageThrottleTime,
                              int receiveMessageThrottleTime,
                              boolean useExternalTor) {
        this.dataDirPath = dataDirPath;
        this.defaultNodePort = defaultNodePort;
        this.bootstrapTimeout = bootstrapTimeout;
        this.hsUploadTimeout = hsUploadTimeout;
        this.socketTimeout = socketTimeout;
        this.isTestNetwork = isTestNetwork;
        this.directoryAuthorities = directoryAuthorities;
        this.torrcOverrides = torrcOverrides;
        this.torrcOverrideFilePath = torrcOverrideFilePath;
        this.sendMessageThrottleTime = sendMessageThrottleTime;
        this.receiveMessageThrottleTime = receiveMessageThrottleTime;
        this.useExternalTor = useExternalTor;
    }
}