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

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The block of the Tor transport in the configuration of an application (for example desktop.conf) carries the
 * sandbox and the bridges to the Tor library.
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
