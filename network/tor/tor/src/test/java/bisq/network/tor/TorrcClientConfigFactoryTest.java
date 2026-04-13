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
import java.util.HashMap;
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

    @Test
    void baseConfigLinesAppearsWhenNoOverrides() {
        when(mockGenerator.generate()).thenReturn(new HashMap<>(Map.of(
                "SocksPort", "auto",
                "DataDirectory", "/tmp/tor"
        )));

        List<String> lines = factory.torrcClientConfigLines(Map.of());

        assertThat(lines).contains("SocksPort auto", "DataDirectory /tmp/tor");
    }

    @Test
    void disableNetworkIsAlwaysAdded() {
        when(mockGenerator.generate()).thenReturn(new HashMap<>(Map.of("SocksPort", "auto")));

        List<String> lines = factory.torrcClientConfigLines(Map.of());

        assertThat(lines).contains(DISABLE_NETWORK + " 1");
    }

    @Test
    void overrideReplacesBaseConfigKeyWithSameName() {
        when(mockGenerator.generate()).thenReturn(new HashMap<>(Map.of(
                "SocksPort", "auto",
                "DataDirectory", "/tmp/tor"
        )));

        List<String> lines = factory.torrcClientConfigLines(Map.of("SocksPort", List.of("9999")));

        assertThat(lines).contains("SocksPort 9999");
        assertThat(lines).doesNotContain("SocksPort auto");
        // unrelated base config key is still present
        assertThat(lines).contains("DataDirectory /tmp/tor");
    }

    @Test
    void overrideKeyNotInBaseIsAddedToOutput() {
        when(mockGenerator.generate()).thenReturn(new HashMap<>(Map.of("SocksPort", "auto")));

        List<String> lines = factory.torrcClientConfigLines(
                Map.of("UseBridges", List.of("1")));

        assertThat(lines)
                .contains("SocksPort auto")
                .contains("UseBridges 1");
    }

    @Test
    void multipleBridgeOverrideValuesAllAppearInOutput() {
        when(mockGenerator.generate()).thenReturn(new HashMap<>(Map.of("SocksPort", "auto")));

        List<String> lines = factory.torrcClientConfigLines(Map.of(
                "UseBridges", List.of("1"),
                "Bridge", List.of("obfs4 192.0.2.1:1234 FP1", "obfs4 192.0.2.2:5678 FP2")
        ));

        assertThat(lines)
                .contains("Bridge obfs4 192.0.2.1:1234 FP1")
                .contains("Bridge obfs4 192.0.2.2:5678 FP2")
                .contains("UseBridges 1");
        assertThat(lines.stream().filter(l -> l.startsWith("Bridge "))).hasSize(2);
        assertThat(lines.indexOf("Bridge obfs4 192.0.2.1:1234 FP1"))
                .isLessThan(lines.indexOf("Bridge obfs4 192.0.2.2:5678 FP2"));
    }

    @Test
    void overrideReplacesDisableNetworkWhenExplicitlySet() {
        when(mockGenerator.generate()).thenReturn(new HashMap<>(Map.of("SocksPort", "auto")));

        // DisableNetwork is put into base config by the factory (value "1") — an override can replace it
        List<String> lines = factory.torrcClientConfigLines(
                Map.of(DISABLE_NETWORK, List.of("0")));

        assertThat(lines).contains(DISABLE_NETWORK + " 0");
        assertThat(lines.stream().filter(l -> l.startsWith(DISABLE_NETWORK + " "))).hasSize(1);
    }

    @Test
    void createTorrcConfigFileFlowWritesMergedConfigToTorrc() throws IOException {
        when(mockGenerator.generate()).thenReturn(new HashMap<>(Map.of(
                "SocksPort", "auto",
                "DataDirectory", "/tmp/tor"
        )));

        List<String> lines = factory.torrcClientConfigLines(Map.of(
                "SocksPort", List.of("9999"),
                "Bridge", List.of("obfs4 192.0.2.1:1234 FP1", "obfs4 192.0.2.2:5678 FP2")
        ));

        Path torrcPath = tempDir.resolve("torrc");
        new TorrcFileGenerator(torrcPath, lines, Set.of()).generate();

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
    void overrideWithEmptyListRemovesKeyFromOutput() {
        when(mockGenerator.generate()).thenReturn(new HashMap<>(Map.of(
                "SocksPort", "auto",
                "DataDirectory", "/tmp/tor"
        )));
        List<String> lines = factory.torrcClientConfigLines(Map.of("SocksPort", List.of())); // empty list
        assertThat(lines).doesNotContain("SocksPort auto");
        assertThat(lines.stream().filter(l -> l.startsWith("SocksPort "))).isEmpty();
        assertThat(lines).contains("DataDirectory /tmp/tor");
    }
}