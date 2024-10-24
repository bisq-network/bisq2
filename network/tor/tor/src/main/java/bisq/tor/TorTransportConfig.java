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

package bisq.tor;

import bisq.common.network.TransportConfig;
import bisq.network.tor.common.torrc.DirectoryAuthority;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Getter
@ToString
@EqualsAndHashCode
public class TorTransportConfig implements TransportConfig {

    public static TorTransportConfig from(Path dataDir, com.typesafe.config.Config config) {
        return new TorTransportConfig(
                dataDir,
                config.hasPath("defaultNodePort") ? config.getInt("defaultNodePort") : -1,
                (int) TimeUnit.SECONDS.toMillis(config.getInt("bootstrapTimeout")),
                (int) TimeUnit.SECONDS.toMillis(config.getInt("hsUploadTimeout")),
                (int) TimeUnit.SECONDS.toMillis(config.getInt("defaultNodeSocketTimeout")),
                (int) TimeUnit.SECONDS.toMillis(config.getInt("userNodeSocketTimeout")),
                config.getBoolean("testNetwork"),
                parseDirectoryAuthorities(config.getList("directoryAuthorities")),
                parseTorrcOverrideConfig(config.getConfig("torrcOverrides")),
                config.getInt("sendMessageThrottleTime"),
                config.getInt("receiveMessageThrottleTime")
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

    private final Path dataDir;
    private final int defaultNodePort;
    private final int bootstrapTimeout; // in ms
    private final int hsUploadTimeout; // in ms
    private final int defaultNodeSocketTimeout; // in ms
    private final int userNodeSocketTimeout; // in ms
    private final boolean isTestNetwork;
    private final Set<DirectoryAuthority> directoryAuthorities;
    private final Map<String, String> torrcOverrides;
    private final int sendMessageThrottleTime;
    private final int receiveMessageThrottleTime;

    public TorTransportConfig(Path dataDir,
                              int defaultNodePort,
                              int bootstrapTimeout,
                              int hsUploadTimeout,
                              int defaultNodeSocketTimeout,
                              int userNodeSocketTimeout,
                              boolean isTestNetwork,
                              Set<DirectoryAuthority> directoryAuthorities,
                              Map<String, String> torrcOverrides,
                              int sendMessageThrottleTime,
                              int receiveMessageThrottleTime) {
        this.dataDir = dataDir;
        this.defaultNodePort = defaultNodePort;
        this.bootstrapTimeout = bootstrapTimeout;
        this.hsUploadTimeout = hsUploadTimeout;
        this.defaultNodeSocketTimeout = defaultNodeSocketTimeout;
        this.userNodeSocketTimeout = userNodeSocketTimeout;
        this.isTestNetwork = isTestNetwork;
        this.directoryAuthorities = directoryAuthorities;
        this.torrcOverrides = torrcOverrides;
        this.sendMessageThrottleTime = sendMessageThrottleTime;
        this.receiveMessageThrottleTime = receiveMessageThrottleTime;
    }
}