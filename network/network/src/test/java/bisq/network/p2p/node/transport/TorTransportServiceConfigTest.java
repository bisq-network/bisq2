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

package bisq.network.p2p.node.transport;

import bisq.network.tor.BridgeConfig;
import bisq.network.tor.TorTransportConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The block of the Tor transport in the configuration of an application (for example desktop.conf) carries the
 * sandbox, the bridges and the torrc overrides to the Tor library.
 */
class TorTransportServiceConfigTest {
    private static final String TOR_CONFIG = """
            socketTimeout=120
            sendMessageThrottleTime=200
            receiveMessageThrottleTime=200
            bootstrapTimeout=240
            hsUploadTimeout=120
            testNetwork=false
            directoryAuthorities=[]
            torrcOverrides {}
            useExternalTor=false
            """;
    private static final String BRIDGE_LINE =
            "obfs4 192.0.2.1:443 0123456789ABCDEF0123456789ABCDEF01234567 cert=abc iat-mode=0";

    @Test
    void builtInBridgesAndTheSandboxWithTransportsReachTheTorTransport() {
        TorTransportConfig torConfig = torTransportConfig("""
                builtInBridges="obfs4"
                sandbox {
                    enabled=true
                    allowPluggableTransports=true
                    onionServicePortPoolSize=8
                }
                """);

        assertThat(torConfig.getBridgeConfig().getBuiltInBridges()).contains("obfs4");
        assertThat(torConfig.getSandboxConfig().isEnabled()).isTrue();
        assertThat(torConfig.getSandboxConfig().isAllowPluggableTransports()).isTrue();
        assertThat(torConfig.getSandboxConfig().getOnionServicePortPoolSize()).isEqualTo(8);
    }

    @Test
    void customBridgesAndADisabledSandboxReachTheTorTransport() {
        TorTransportConfig torConfig = torTransportConfig("""
                bridges=["%s"]
                sandbox {
                    enabled=false
                }
                """.formatted(BRIDGE_LINE));

        assertThat(torConfig.getBridgeConfig().getCustomBridgeLines()).isEqualTo(List.of(BRIDGE_LINE));
        assertThat(torConfig.getSandboxConfig().isEnabled()).isFalse();
        assertThat(torConfig.getSandboxConfig().isAllowPluggableTransports()).isFalse();
    }

    // The values which the configuration of the applications sets: no bridges, the sandbox on, no transports
    @Test
    void withoutBridgesAndSandboxBlockTheSandboxIsOnWithoutTransports() {
        TorTransportConfig torConfig = torTransportConfig("""
                builtInBridges=""
                bridges=[]
                """);

        assertThat(torConfig.getBridgeConfig()).isEqualTo(BridgeConfig.NONE);
        assertThat(torConfig.getSandboxConfig().isEnabled()).isTrue();
        assertThat(torConfig.getSandboxConfig().isAllowPluggableTransports()).isFalse();
    }

    // ApplicationService reads program arguments and JVM options with ConfigFactory.parseMap and bisq.conf with
    // ConfigFactory.parseFile, and the program arguments win. The Tor library refuses the overrides of bisq.conf, which
    // Bisq 2 also writes itself, if they make Tor start a program or load a library.
    @Test
    void theTorrcOverridesOfBisqConfAreTheOverridesOfAConfigurationFile(@TempDir Path dataDir) throws IOException {
        Path bisqConf = Files.writeString(dataDir.resolve("bisq.conf"),
                "torrcOverrides { AccelName = \"engine\", ClientTransportPlugin = \"obfs4 exec /tmp/program\" }");
        Config programArguments = ConfigFactory.parseMap(Map.of("torrcOverrides.ClientTransportPlugin",
                "obfs4 exec /usr/bin/lyrebird"));
        Config config = programArguments
                .withFallback(ConfigFactory.parseFile(bisqConf.toFile()))
                .withFallback(ConfigFactory.parseString(TOR_CONFIG))
                .resolve();

        TorTransportConfig torConfig = TorTransportService.Config.from(Path.of("tor"), config).getTorTransportConfig();

        assertThat(torConfig.getTorrcOverrides())
                .containsEntry("AccelName", "engine")
                .containsEntry("ClientTransportPlugin", "obfs4 exec /usr/bin/lyrebird");
        assertThat(torConfig.getConfigFileTorrcOverrides().getKeys()).containsExactly("AccelName");
        assertThat(torConfig.getConfigFileTorrcOverrides().getHint()).contains("bisq.conf");
    }

    @Test
    void builtInAndCustomBridgesTogetherAreRefused() {
        assertThatThrownBy(() -> torTransportConfig("""
                builtInBridges="obfs4"
                bridges=["%s"]
                """.formatted(BRIDGE_LINE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TorTransportConfig torTransportConfig(String bridgesAndSandbox) {
        Config config = ConfigFactory.parseString(TOR_CONFIG + bridgesAndSandbox);
        return TorTransportService.Config.from(Path.of("tor"), config).getTorTransportConfig();
    }
}
