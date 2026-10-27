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

import bisq.common.network.TransportType;
import bisq.network.NetworkServiceConfig;
import bisq.network.tor.TorSandboxConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When the Tor library refuses to start Tor without its sandbox, its message tells the user of Bisq 2 to turn the
 * sandbox off with a JVM option. The library names the path of the option itself, so a rename of the configuration
 * of Bisq 2 or of the path in the library has to fail this test.
 */
class TorSandboxOptOutHintTest {
    // The JVM option of the hint, for example -Dapplication.network.configByTransportType.tor.sandbox.enabled=false
    private static final Pattern JVM_OPTION = Pattern.compile("-D((?:\\w+\\.)+\\w+)=false");
    private static final String TOR_CONFIG_PATH = "application.network.configByTransportType.tor";

    @Test
    void optOutHintNamesTheJvmOptionWhichTurnsTheSandboxOffInEveryApplication() throws IOException {
        List<Path> applicationConfigPaths = findApplicationConfigPaths();
        assertThat(applicationConfigPaths)
                .anyMatch(path -> path.endsWith(Path.of("desktop-app", "src", "main", "resources", "desktop.conf")));

        for (Path applicationConfigPath : applicationConfigPaths) {
            Config resourceConfig = ConfigFactory.parseFile(applicationConfigPath.toFile()).resolve();
            TorSandboxConfig sandboxConfig = readSandboxConfig(resourceConfig);
            assertThat(sandboxConfig.isEnabled()).as(applicationConfigPath.toString()).isTrue();

            Matcher matcher = JVM_OPTION.matcher(sandboxConfig.getOptOutHint());
            assertThat(matcher.find()).as(sandboxConfig.getOptOutHint()).isTrue();
            String optionPath = matcher.group(1);
            // Bisq 2 takes only the JVM options which start with "application." into its configuration
            // (TypesafeConfigUtils.resolveFilteredJvmOptions), and they take precedence over the resource configuration.
            assertThat(optionPath).startsWith("application.");
            Config configWithOption = ConfigFactory.parseMap(Map.of(optionPath, false))
                    .withFallback(resourceConfig)
                    .resolve();

            assertThat(readSandboxConfig(configWithOption).isEnabled()).as(applicationConfigPath.toString()).isFalse();
        }
    }

    // Reads the sandbox as the applications do: the network block of the application block, through
    // NetworkServiceConfig
    private static TorSandboxConfig readSandboxConfig(Config rootConfig) {
        Config networkConfig = rootConfig.getConfig("application").getConfig("network");
        NetworkServiceConfig networkServiceConfig = NetworkServiceConfig.from(Path.of("data"), networkConfig);
        TorTransportService.Config torConfig =
                (TorTransportService.Config) networkServiceConfig.getConfigByTransportType().get(TransportType.TOR);
        return torConfig.getTorTransportConfig().getSandboxConfig();
    }

    // The configurations of the applications which configure the Tor transport, in apps/<app>/src/main/resources or
    // apps/desktop/<app>/src/main/resources. The depth keeps the walk out of the build output.
    private static List<Path> findApplicationConfigPaths() throws IOException {
        Path appsDirPath = findRepositoryRoot().resolve("apps");
        try (Stream<Path> paths = Files.walk(appsDirPath, 6)) {
            return paths.filter(path -> path.toString().endsWith(".conf"))
                    .filter(path -> path.getParent().endsWith(Path.of("src", "main", "resources")))
                    .filter(path -> ConfigFactory.parseFile(path.toFile()).hasPath(TOR_CONFIG_PATH))
                    .sorted()
                    .toList();
        }
    }

    private static Path findRepositoryRoot() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null && !Files.isDirectory(path.resolve("apps").resolve("desktop"))) {
            path = path.getParent();
        }
        assertThat(path).as("The repository root above the working directory of the test").isNotNull();
        return path;
    }
}
