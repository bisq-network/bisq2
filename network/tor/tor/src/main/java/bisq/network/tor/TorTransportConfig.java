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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
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

    private static Map<String, String> parseTorrcOverrideConfig(com.typesafe.config.Config torrcOverrides) {
        Map<String, String> torrcOverrideConfigMap = new HashMap<>();
        torrcOverrides.entrySet()
                .forEach(entry -> torrcOverrideConfigMap.put(
                        entry.getKey(), (String) entry.getValue().unwrapped()
                ));
        return torrcOverrideConfigMap;
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
    private final Map<String, String> torrcOverrides;
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
                              Map<String, String> torrcOverrides,
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
        this.sendMessageThrottleTime = sendMessageThrottleTime;
        this.receiveMessageThrottleTime = receiveMessageThrottleTime;
        this.useExternalTor = useExternalTor;
    }
}