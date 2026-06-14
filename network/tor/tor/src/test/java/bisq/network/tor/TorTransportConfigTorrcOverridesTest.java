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

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TorTransportConfigTorrcOverridesTest {

    private static final String MINIMAL_CONFIG_TEMPLATE =
            "bootstrapTimeout=240\n" +
                    "hsUploadTimeout=120\n" +
                    "socketTimeout=120\n" +
                    "testNetwork=false\n" +
                    "directoryAuthorities=[]\n" +
                    "sendMessageThrottleTime=200\n" +
                    "receiveMessageThrottleTime=200\n" +
                    "useExternalTor=false\n";

    @Test
    void oldFormatSingleKeyValueIsSupported(@TempDir Path tempDir) {
        // Old format: torrcOverrides { SomeKey = someValue }
        // This is what all existing production configs use
        String configStr = MINIMAL_CONFIG_TEMPLATE +
                "torrcOverrides { SocksPort = 9999 }\n";

        var config = ConfigFactory.parseString(configStr);
        TorTransportConfig torTransportConfig = TorTransportConfig.from(tempDir, config);

        Map<String, List<String>> overrides = torTransportConfig.getTorrcOverrides();
        assertThat(overrides).hasSize(1);
        assertThat(overrides.get("SocksPort")).containsExactly("9999");
    }

    @Test
    void newFormatListValueAllowsMultipleEntriesForSameKey(@TempDir Path tempDir) {
        // New format: torrcOverrides { Bridge = ["value1", "value2"] }
        // Needed for directives like Bridge that can legitimately appear multiple times
        String configStr = MINIMAL_CONFIG_TEMPLATE +
                "torrcOverrides {\n" +
                "  UseBridges = 1\n" +
                "  Bridge = [\"obfs4 192.0.2.1:1234 FP1\", \"obfs4 192.0.2.2:5678 FP2\"]\n" +
                "}\n";

        var config = ConfigFactory.parseString(configStr);
        TorTransportConfig torTransportConfig = TorTransportConfig.from(tempDir, config);

        Map<String, List<String>> overrides = torTransportConfig.getTorrcOverrides();
        assertThat(overrides.get("Bridge")).containsExactly(
                "obfs4 192.0.2.1:1234 FP1",
                "obfs4 192.0.2.2:5678 FP2"
        );
        assertThat(overrides.get("UseBridges")).containsExactly("1");
    }

    @Test
    void oldFormatSingleStringBridgeIsSupported(@TempDir Path tempDir) {
        // A single Bridge string value (non-list) still works
        String configStr = MINIMAL_CONFIG_TEMPLATE +
                "torrcOverrides { Bridge = \"obfs4 192.0.2.1:1234 FP1\" }\n";

        var config = ConfigFactory.parseString(configStr);
        TorTransportConfig torTransportConfig = TorTransportConfig.from(tempDir, config);

        assertThat(torTransportConfig.getTorrcOverrides().get("Bridge"))
                .containsExactly("obfs4 192.0.2.1:1234 FP1");
    }

    @Test
    void oldFormatDuplicateBridgeKeysLastValueWins(@TempDir Path tempDir) {
        // HOCON does not support duplicate keys — the second assignment silently replaces the first.
        // Users who need multiple Bridge lines must use the list format instead.
        String configStr = MINIMAL_CONFIG_TEMPLATE +
                "torrcOverrides {\n" +
                "  Bridge = \"obfs4 192.0.2.1:1234 FP1\"\n" +
                "  Bridge = \"obfs4 192.0.2.2:5678 FP2\"\n" +
                "}\n";

        var config = ConfigFactory.parseString(configStr);
        TorTransportConfig torTransportConfig = TorTransportConfig.from(tempDir, config);

        // Only the last value survives due to HOCON semantics
        assertThat(torTransportConfig.getTorrcOverrides().get("Bridge"))
                .containsExactly("obfs4 192.0.2.2:5678 FP2");
    }

    @Test
    void emptyOverridesProducesEmptyMap(@TempDir Path tempDir) {
        // Production default: torrcOverrides={}
        String configStr = MINIMAL_CONFIG_TEMPLATE +
                "torrcOverrides={}\n";

        var config = ConfigFactory.parseString(configStr);
        TorTransportConfig torTransportConfig = TorTransportConfig.from(tempDir, config);

        assertThat(torTransportConfig.getTorrcOverrides()).isEmpty();
    }

    // ── torrcOverrideFilePath ──────────────────────────────────────────────────

    @Test
    void torrcOverrideFilePathDefaultsToEmpty(@TempDir Path tempDir) {
        // When missing from config, torrcOverrideFilePath should be empty (feature disabled)
        String configStr = MINIMAL_CONFIG_TEMPLATE + "torrcOverrides={}\n";

        var config = ConfigFactory.parseString(configStr);
        TorTransportConfig torTransportConfig = TorTransportConfig.from(tempDir, config);

        assertThat(torTransportConfig.getTorrcOverrideFilePath()).isEmpty();
    }

    @Test
    void torrcOverrideFilePathAbsoluteIsParsedFromConfig(@TempDir Path tempDir) {
        String configStr = MINIMAL_CONFIG_TEMPLATE +
                "torrcOverrides={}\n" +
                "torrcOverrideFilePath=\"/etc/bisq/custom.torrc\"\n";

        var config = ConfigFactory.parseString(configStr);
        TorTransportConfig torTransportConfig = TorTransportConfig.from(tempDir, config);

        assertThat(torTransportConfig.getTorrcOverrideFilePath())
                .contains(Path.of("/etc/bisq/custom.torrc"));
    }

    @Test
    void torrcOverrideFilePathRelativeIsResolvedAgainstDataDir(@TempDir Path tempDir) {
        String configStr = MINIMAL_CONFIG_TEMPLATE +
                "torrcOverrides={}\n" +
                "torrcOverrideFilePath=\"custom.torrc\"\n";

        var config = ConfigFactory.parseString(configStr);
        TorTransportConfig torTransportConfig = TorTransportConfig.from(tempDir, config);

        assertThat(torTransportConfig.getTorrcOverrideFilePath())
                .contains(tempDir.resolve("custom.torrc"));
    }

    @Test
    void parseTorrcOverrideFileParsesSimpleKeyValueLines(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("overrides.torrc");
        Files.writeString(file, "SocksPort 9050\nUseBridges 1\n");

        var result = TorrcFileParser.parseTorrcOverrideFile(file);

        assertThat(result.get("SocksPort")).containsExactly("9050");
        assertThat(result.get("UseBridges")).containsExactly("1");
    }

    @Test
    void parseTorrcOverrideFileAccumulatesRepeatedKeys(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("overrides.torrc");
        Files.writeString(file,
                "UseBridges 1\n" +
                        "Bridge obfs4 1.2.3.4:1234 FP1\n" +
                        "Bridge obfs4 5.6.7.8:5678 FP2\n");

        var result = TorrcFileParser.parseTorrcOverrideFile(file);

        assertThat(result.get("Bridge")).containsExactly(
                "obfs4 1.2.3.4:1234 FP1",
                "obfs4 5.6.7.8:5678 FP2");
        assertThat(result.get("UseBridges")).containsExactly("1");
    }

    @Test
    void parseTorrcOverrideFileSkipsCommentAndBlankLines(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("overrides.torrc");
        Files.writeString(file,
                "# This is a comment\n" +
                        "\n" +
                        "SocksPort 9050\n" +
                        "  \n" +
                        "# another comment\n" +
                        "UseBridges 1\n");

        var result = TorrcFileParser.parseTorrcOverrideFile(file);

        assertThat(result).hasSize(2);
        assertThat(result.get("SocksPort")).containsExactly("9050");
        assertThat(result.get("UseBridges")).containsExactly("1");
    }

    @Test
    void parseTorrcOverrideFileHandlesTabAsKeyValueDelimiter(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("overrides.torrc");
        Files.writeString(file, "SocksPort\t9050\n");

        var result = TorrcFileParser.parseTorrcOverrideFile(file);

        assertThat(result.get("SocksPort")).containsExactly("9050");
    }

    @Test
    void parseTorrcOverrideFileHandlesValuesWithSpaces(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("overrides.torrc");
        // Bridge values contain spaces — everything after the first space is the value
        Files.writeString(file, "Bridge obfs4 1.2.3.4:1234 ABCDEF fingerprint\n");

        var result = TorrcFileParser.parseTorrcOverrideFile(file);

        assertThat(result.get("Bridge")).containsExactly("obfs4 1.2.3.4:1234 ABCDEF fingerprint");
    }
}
