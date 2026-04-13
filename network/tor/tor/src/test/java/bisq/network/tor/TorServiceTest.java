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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TorServiceTest {

    @Test
    void readExternalTorConfigMapUsesTorrcOverrideFilePathWhenSet(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("external_tor.config"),
                "UseExternalTor 0\n" +
                        "ControlPort 127.0.0.1:9051\n");
        Files.writeString(tempDir.resolve("custom_external_tor.config"),
                "UseExternalTor 1\n" +
                        "ControlPort 127.0.0.1:1111\n");

        TorService torService = new TorService(createConfig(tempDir, "custom_external_tor.config"));
        invokeReadExternalTorConfigMap(torService);

        Map<String, String> externalTorConfigMap = getExternalTorConfigMap(torService);
        assertThat(externalTorConfigMap.get("UseExternalTor")).isEqualTo("1");
        assertThat(externalTorConfigMap.get("ControlPort")).isEqualTo("127.0.0.1:1111");
    }

    @Test
    void readExternalTorConfigMapFallsBackToExternalTorConfigWhenOverridePathMissing(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("external_tor.config"),
                "UseExternalTor 1\n" +
                        "ControlPort 127.0.0.1:2222\n");

        TorService torService = new TorService(createConfig(tempDir, "missing_external_tor.config"));
        invokeReadExternalTorConfigMap(torService);

        Map<String, String> externalTorConfigMap = getExternalTorConfigMap(torService);
        assertThat(externalTorConfigMap.get("UseExternalTor")).isEqualTo("1");
        assertThat(externalTorConfigMap.get("ControlPort")).isEqualTo("127.0.0.1:2222");
    }

    @Test
    void readExternalTorConfigMapIncludesControlSocketWhenConfigured(@TempDir Path tempDir) throws Exception {
        Path controlSocketPath = tempDir.resolve("tor-control.sock");
        Files.writeString(tempDir.resolve("external_tor.config"),
                "UseExternalTor 1\n" +
                        "ControlSocket " + controlSocketPath + "\n" +
                        "ControlPort 127.0.0.1:9051\n");

        TorService torService = new TorService(createConfig(tempDir, ""));
        invokeReadExternalTorConfigMap(torService);

        Map<String, String> externalTorConfigMap = getExternalTorConfigMap(torService);
        assertThat(externalTorConfigMap.get("ControlSocket")).isEqualTo(controlSocketPath.toString());
        assertThat(externalTorConfigMap.get("ControlPort")).isEqualTo("127.0.0.1:9051");
    }

    @Test
    void getControlEndpointUnquotesConfiguredControlSocket(@TempDir Path tempDir) throws Exception {
        Path controlSocketPath = tempDir.resolve("tor-control.sock");
        Files.writeString(tempDir.resolve("external_tor.config"),
                "UseExternalTor 1\n" +
                        "ControlSocket \"" + controlSocketPath + "\"\n" +
                        "CookieAuthFile '" + tempDir.resolve("control.authcookie") + "'\n");

        TorService torService = new TorService(createConfig(tempDir, ""));
        invokeReadExternalTorConfigMap(torService);

        Map<String, String> externalTorConfigMap = getExternalTorConfigMap(torService);
        Method getControlEndpoint = TorService.class.getDeclaredMethod("getControlEndpoint", Map.class);
        getControlEndpoint.setAccessible(true);
        Object controlEndpoint = getControlEndpoint.invoke(torService, externalTorConfigMap);
        assertThat(controlEndpoint.getClass().getSimpleName()).isEqualTo("UnixSocketControlEndpoint");
        Method controlSocketPathAccessor = controlEndpoint.getClass().getDeclaredMethod("controlSocketPath");
        controlSocketPathAccessor.setAccessible(true);

        assertThat(controlSocketPathAccessor.invoke(controlEndpoint)).isEqualTo(controlSocketPath);
        assertThat(externalTorConfigMap.get("CookieAuthFile"))
                .isEqualTo("'" + tempDir.resolve("control.authcookie") + "'");
    }

    @Test
    void initializeFailsWhenExternalTorIsEnabledButControlServerIsUnreachable(@TempDir Path tempDir) throws Exception {
        // Keep this test deterministic by using an invalid local control port.
        Files.writeString(tempDir.resolve("external_tor.config"),
                "UseExternalTor 1\n" +
                        "ControlPort 127.0.0.1:1\n" +
                        "CookieAuthentication 0\n");

        // TorService decides from external_tor.config, not from TorTransportConfig.useExternalTor.
        TorTransportConfig config = new TorTransportConfig(
                tempDir,
                -1,
                500,
                500,
                500,
                false,
                Set.of(),
                Map.of(),
                "",
                200,
                200,
                false
        );

        TorService torService = new TorService(config);

        assertThatThrownBy(torService::initialize)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("could not connect to the external Tor control server");
    }

    @Test
    void initializeFailsWhenExternalTorControlSocketIsUnavailable(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("external_tor.config"),
                "UseExternalTor 1\n" +
                        "ControlSocket " + tempDir.resolve("missing-control.sock") + "\n" +
                        "CookieAuthentication 0\n");

        TorService torService = new TorService(createConfig(tempDir, ""));

        assertThatThrownBy(torService::initialize)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("could not connect to the external Tor control server");
    }

    @Test
    void initializeFailureMentionsCustomExternalTorConfigPath(@TempDir Path tempDir) throws Exception {
        Path customConfigPath = tempDir.resolve("custom_external_tor.config");
        Files.writeString(customConfigPath,
                "UseExternalTor 1\n" +
                        "ControlPort 127.0.0.1:1\n" +
                        "CookieAuthentication 0\n");

        TorService torService = new TorService(createConfig(tempDir, "custom_external_tor.config"));

        assertThatThrownBy(torService::initialize)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(customConfigPath.toString());
    }

    @Test
    void getControlEndpointFailureMentionsCustomExternalTorConfigPath(@TempDir Path tempDir) throws Exception {
        Path customConfigPath = tempDir.resolve("custom_external_tor.config");
        Files.writeString(customConfigPath,
                "UseExternalTor 1\n" +
                        "CookieAuthentication 0\n");

        TorService torService = new TorService(createConfig(tempDir, "custom_external_tor.config"));
        invokeReadExternalTorConfigMap(torService);

        assertThatThrownBy(() -> invokeGetControlEndpoint(torService, getExternalTorConfigMap(torService)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(customConfigPath.toString())
                .hasMessageContaining("Either ControlSocket or ControlPort must be set");
    }

    private static TorTransportConfig createConfig(Path dataDirPath, String torrcOverrideFilePath) {
        return new TorTransportConfig(
                dataDirPath,
                -1,
                500,
                500,
                500,
                false,
                Set.of(),
                Map.of(),
                torrcOverrideFilePath,
                200,
                200,
                false
        );
    }

    private static void invokeReadExternalTorConfigMap(TorService torService) throws Exception {
        Method readExternalTorConfigMap = TorService.class.getDeclaredMethod("readExternalTorConfigMap");
        readExternalTorConfigMap.setAccessible(true);
        readExternalTorConfigMap.invoke(torService);
    }

    private static Object invokeGetControlEndpoint(TorService torService,
                                                   Map<String, String> externalTorConfigMap) throws Throwable {
        Method getControlEndpoint = TorService.class.getDeclaredMethod("getControlEndpoint", Map.class);
        getControlEndpoint.setAccessible(true);
        try {
            return getControlEndpoint.invoke(torService, externalTorConfigMap);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getExternalTorConfigMap(TorService torService) throws Exception {
        Field externalTorConfigMapField = TorService.class.getDeclaredField("externalTorConfigMap");
        externalTorConfigMapField.setAccessible(true);
        return (Map<String, String>) externalTorConfigMapField.get(torService);
    }
}