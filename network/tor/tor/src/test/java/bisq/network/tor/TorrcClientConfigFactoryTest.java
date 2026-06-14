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

import bisq.network.tor.common.torrc.TorrcConfigGenerator;
import bisq.network.tor.common.torrc.TorrcFileGenerator;
import net.freehaven.tor.control.PasswordDigest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static bisq.network.tor.common.torrc.Torrc.Keys.DISABLE_NETWORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class TorrcClientConfigFactoryTest {

    @TempDir
    Path tempDir;

    private TorrcConfigGenerator mockGenerator;
    private PasswordDigest mockPasswordDigest;
    private TorrcClientConfigFactory factory;

    @BeforeEach
    void setUp() {
        mockGenerator = Mockito.mock(TorrcConfigGenerator.class);
        mockPasswordDigest = Mockito.mock(PasswordDigest.class);

        // Use doReturn().when() to avoid calling the real method during stub setup.
        TorrcClientConfigFactory base = new TorrcClientConfigFactory(false, tempDir, mockPasswordDigest);
        factory = Mockito.spy(base);
        doReturn(mockGenerator).when(factory).clientTorrcGenerator();
    }

    private static Map<String, List<String>> mutableBaseConfig(Map<String, List<String>> entries) {
        return new LinkedHashMap<>(entries);
    }

    @Test
    void baseConfigAppearsWhenNoOverrides() {
        when(mockGenerator.generate()).thenReturn(mutableBaseConfig(Map.of(
                "SocksPort", List.of("auto"),
                "DataDirectory", List.of("/tmp/tor")
        )));

        Map<String, List<String>> config = factory.torrcClientConfigMap(Map.of());

        assertThat(config.get("SocksPort")).containsExactly("auto");
        assertThat(config.get("DataDirectory")).containsExactly("/tmp/tor");
    }

    @Test
    void disableNetworkIsAlwaysAdded() {
        when(mockGenerator.generate()).thenReturn(mutableBaseConfig(Map.of("SocksPort", List.of("auto"))));

        Map<String, List<String>> config = factory.torrcClientConfigMap(Map.of());

        assertThat(config.get(DISABLE_NETWORK)).containsExactly("1");
    }

    @Test
    void overrideReplacesBaseConfigKeyWithSameName() {
        when(mockGenerator.generate()).thenReturn(mutableBaseConfig(Map.of(
                "SocksPort", List.of("auto"),
                "DataDirectory", List.of("/tmp/tor")
        )));

        Map<String, List<String>> config = factory.torrcClientConfigMap(Map.of("SocksPort", List.of("9999")));

        assertThat(config.get("SocksPort")).containsExactly("9999");
        // unrelated base config key is still present
        assertThat(config.get("DataDirectory")).containsExactly("/tmp/tor");
    }

    @Test
    void overrideKeyNotInBaseIsAddedToOutput() {
        when(mockGenerator.generate()).thenReturn(mutableBaseConfig(Map.of("SocksPort", List.of("auto"))));

        Map<String, List<String>> config = factory.torrcClientConfigMap(Map.of("UseBridges", List.of("1")));

        assertThat(config.get("SocksPort")).containsExactly("auto");
        assertThat(config.get("UseBridges")).containsExactly("1");
    }

    @Test
    void multipleBridgeOverrideValuesAllAppearInOutput() {
        when(mockGenerator.generate()).thenReturn(mutableBaseConfig(Map.of("SocksPort", List.of("auto"))));

        Map<String, List<String>> config = factory.torrcClientConfigMap(Map.of(
                "UseBridges", List.of("1"),
                "Bridge", List.of("obfs4 192.0.2.1:1234 FP1", "obfs4 192.0.2.2:5678 FP2")
        ));

        assertThat(config.get("Bridge")).containsExactly(
                "obfs4 192.0.2.1:1234 FP1",
                "obfs4 192.0.2.2:5678 FP2");
        assertThat(config.get("UseBridges")).containsExactly("1");
    }

    @Test
    void overrideReplacesDisableNetworkWhenExplicitlySet() {
        when(mockGenerator.generate()).thenReturn(mutableBaseConfig(Map.of("SocksPort", List.of("auto"))));

        // DisableNetwork is put into base config by the factory (value "1") — an override can replace it
        Map<String, List<String>> config = factory.torrcClientConfigMap(Map.of(DISABLE_NETWORK, List.of("0")));

        assertThat(config.get(DISABLE_NETWORK)).containsExactly("0");
    }

    @Test
    void createTorrcConfigFileFlowWritesMergedConfigToTorrc() throws IOException {
        when(mockGenerator.generate()).thenReturn(mutableBaseConfig(Map.of(
                "SocksPort", List.of("auto"),
                "DataDirectory", List.of("/tmp/tor")
        )));

        Map<String, List<String>> config = factory.torrcClientConfigMap(Map.of(
                "SocksPort", List.of("9999"),
                "Bridge", List.of("obfs4 192.0.2.1:1234 FP1", "obfs4 192.0.2.2:5678 FP2")
        ));

        Path torrcPath = tempDir.resolve("torrc");
        new TorrcFileGenerator(torrcPath, config, Set.of()).generate();

        String torrcContent = Files.readString(torrcPath);
        assertThat(torrcContent)
                .contains("DataDirectory /tmp/tor")
                .contains("SocksPort 9999")
                .contains("Bridge obfs4 192.0.2.1:1234 FP1")
                .contains("Bridge obfs4 192.0.2.2:5678 FP2")
                .contains(DISABLE_NETWORK + " 1")
                .doesNotContain("SocksPort auto");
        assertThat(torrcContent.lines().filter(l -> l.startsWith("SocksPort "))).hasSize(1);
    }

    @Test
    void overrideWithEmptyListRemovesKeyFromOutput() throws IOException {
        when(mockGenerator.generate()).thenReturn(mutableBaseConfig(Map.of(
                "SocksPort", List.of("auto"),
                "DataDirectory", List.of("/tmp/tor")
        )));

        Map<String, List<String>> config = factory.torrcClientConfigMap(Map.of("SocksPort", List.of())); // empty list

        assertThat(config.get("SocksPort")).isEmpty();
        assertThat(config.get("DataDirectory")).containsExactly("/tmp/tor");

        Path torrcPath = tempDir.resolve("torrc");
        new TorrcFileGenerator(torrcPath, config, Set.of()).generate();
        assertThat(Files.readString(torrcPath)).doesNotContain("SocksPort ");
    }
}
