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

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationServiceConfigTest {
    private static final String BROKEN_CONTENT = """
            application.network.configByTransportType.tor.bridges = ["obfs4 192.0.2.1:443 cert=x iat-mode=0"]
            application.network {
            """;

    // Earlier versions read such a file as empty and replaced it with the new settings only, so the bridges were lost
    @Test
    void aRewriteLeavesAFileWhichCannotBeReadAsItIs(@TempDir Path appDataDirPath) throws IOException {
        Path configFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        Files.writeString(configFilePath, BROKEN_CONTENT, StandardCharsets.UTF_8);
        ApplicationService.Config config = createConfig(appDataDirPath);

        assertThrows(IOException.class, () -> config.writeCustomConfig(
                ConfigFactory.parseMap(Map.of("application.api.server.restEnabled", true))));
        assertThrows(IOException.class, () -> config.removeNodeFromCustomConfig("application.api"));

        assertEquals(BROKEN_CONTENT, Files.readString(configFilePath, StandardCharsets.UTF_8));
    }

    private static ApplicationService.Config createConfig(Path appDataDirPath) {
        return new ApplicationService.Config(ConfigFactory.empty(), ConfigFactory.empty(), appDataDirPath, "bisq2_test",
                false, 0, false, "", false, false, 0, false, false);
    }
}
