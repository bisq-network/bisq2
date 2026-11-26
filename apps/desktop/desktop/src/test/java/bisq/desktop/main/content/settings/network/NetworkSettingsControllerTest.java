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

package bisq.desktop.main.content.settings.network;

import bisq.application.ApplicationService;
import bisq.application.ShutDownHandler;
import bisq.common.observable.Observable;
import bisq.common.platform.OS;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The switch which allows pluggable transports in the Tor sandbox, and the transport and I2P fields. Tor reads its
 * sandbox configuration when it starts, so the switch writes only its own entry into bisq.conf and shuts Bisq down.
 * The other fields write only the entries which the user changed.
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class NetworkSettingsControllerTest {
    private static final String SANDBOX_PATH = "application.network.configByTransportType.tor.sandbox";
    private static final String ALLOW_PLUGGABLE_TRANSPORTS = SANDBOX_PATH + ".allowPluggableTransports";
    private static final String I2P_PATH = "application.network.configByTransportType.i2p";
    private static final String I2CP_PORT = I2P_PATH + ".i2cpPort";

    @TempDir
    Path appDataDirPath;
    private final ShutDownHandler shutDownHandler = mock(ShutDownHandler.class);

    // The text fields of the view ask Transitions whether to animate
    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
    }

    @AfterEach
    void tearDown() {
        Transitions.setSettingsService(null);
    }

    @Test
    void switchShowsTheConfiguredValueAndOffersToApplyOnlyAChange() {
        NetworkSettingsController controller = createController(true, false);
        NetworkSettingsModel model = controller.getModel();
        activateController(controller, false);

        assertThat(model.getAllowPluggableTransports().get()).isFalse();
        assertThat(model.getAllowPluggableTransportsUnavailableText().get()).isEmpty();
        assertThat(model.getTorShutdownButtonVisible().get()).isFalse();

        JavaFxToolkitExtension.runOnFxThread(() -> controller.onToggleAllowPluggableTransports(true));
        assertThat(model.getAllowPluggableTransports().get()).isTrue();
        assertThat(model.getTorShutdownButtonVisible().get()).isTrue();

        JavaFxToolkitExtension.runOnFxThread(() -> controller.onToggleAllowPluggableTransports(false));
        assertThat(model.getTorShutdownButtonVisible().get()).isFalse();
        verify(shutDownHandler, never()).shutdown();
    }

    // The configuration of the running application does not follow a later change of bisq.conf
    @Test
    void switchShowsTheValueOfBisqConf() throws IOException {
        Files.writeString(appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME),
                ALLOW_PLUGGABLE_TRANSPORTS + "=true\n");
        NetworkSettingsController controller = createController(true, false);

        activateController(controller, false);

        assertThat(controller.getModel().getAllowPluggableTransports().get()).isTrue();
    }

    // The other entries of bisq.conf stay, and the defaults of the application are not copied, so that they keep
    // following the defaults of new releases
    @Test
    void applyWritesOnlyTheSwitchIntoBisqConfAndShutsDown() throws IOException {
        Path bisqConfPath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        Files.writeString(bisqConfPath, "application.network.configByTransportType.tor.builtInBridges=\"obfs4\"\n");
        NetworkSettingsController controller = createController(true, false);
        activateController(controller, false);

        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.onToggleAllowPluggableTransports(true);
            controller.onApplyTorSettingsAndShutdown();
        });

        Config bisqConf = ConfigFactory.parseFile(bisqConfPath.toFile());
        assertThat(bisqConf.getBoolean(ALLOW_PLUGGABLE_TRANSPORTS)).isTrue();
        assertThat(bisqConf.getString("application.network.configByTransportType.tor.builtInBridges")).isEqualTo("obfs4");
        assertThat(bisqConf.hasPath(SANDBOX_PATH + ".enabled")).isFalse();
        assertThat(bisqConf.hasPath("application.network.supportedTransportTypes")).isFalse();
        verify(shutDownHandler).shutdown();
    }

    @Test
    void switchIsUnavailableWhileTheSandboxIsOff() {
        NetworkSettingsController controller = createController(false, false);

        activateController(controller, false);

        assertThat(controller.getModel().getAllowPluggableTransportsUnavailableText().get())
                .isEqualTo(Res.get("settings.network.tor.allowPluggableTransports.sandboxDisabled"));
    }

    // The Tor library refuses pluggable transports in the sandbox on Windows.
    @Test
    void switchIsUnavailableOnWindows() {
        NetworkSettingsController controller = createController(true, false);

        activateController(controller, true);

        assertThat(controller.getModel().getAllowPluggableTransportsUnavailableText().get())
                .isEqualTo(Res.get("settings.network.tor.allowPluggableTransports.windows"));
    }

    // A program argument or JVM option overrides bisq.conf, which the switch writes, so the switch could not change it
    @Test
    void switchIsUnavailableWhileTheCommandLineSetsIt() throws IOException {
        Files.writeString(appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME),
                ALLOW_PLUGGABLE_TRANSPORTS + "=false\n");
        NetworkSettingsController controller = createController(true, true,
                ConfigFactory.parseMap(Map.of(ALLOW_PLUGGABLE_TRANSPORTS, true)));

        activateController(controller, false);

        NetworkSettingsModel model = controller.getModel();
        assertThat(model.getAllowPluggableTransports().get()).isTrue();
        assertThat(model.getAllowPluggableTransportsUnavailableText().get())
                .isEqualTo(Res.get("settings.network.tor.allowPluggableTransports.commandLine",
                        ALLOW_PLUGGABLE_TRANSPORTS, true));
    }

    // The page shows what the next start uses, and there the command line wins over bisq.conf
    @Test
    void theCommandLineWinsOverBisqConf() throws IOException {
        Files.writeString(appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME),
                SANDBOX_PATH + ".enabled=false\n");
        NetworkSettingsController controller = createController(true, false,
                ConfigFactory.parseMap(Map.of(SANDBOX_PATH + ".enabled", true)));

        activateController(controller, false);

        assertThat(controller.getModel().getAllowPluggableTransportsUnavailableText().get()).isEmpty();
    }

    // A value of the command line must not end up in bisq.conf, where it would stay after the option is gone
    @Test
    void applyingTheNetworkSettingsKeepsTheValuesOfBisqConfOverTheCommandLine() throws IOException {
        Path bisqConfPath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        Files.writeString(bisqConfPath, I2CP_PORT + "=7655\n");
        NetworkSettingsController controller = createController(true, false,
                ConfigFactory.parseMap(Map.of(I2CP_PORT, 9000)));
        activateController(controller, false);

        assertThat(controller.getModel().getI2cpAddress().get().getPort()).isEqualTo(9000);

        JavaFxToolkitExtension.runOnFxThread(controller::onApplyAndShutdown);

        assertThat(ConfigFactory.parseFile(bisqConfPath.toFile()).getInt(I2CP_PORT)).isEqualTo(7655);
    }

    // An unchanged value can come from the command line or from the defaults of the release. Neither belongs into
    // bisq.conf.
    @Test
    void applyingTheNetworkSettingsWritesOnlyTheChangedEntries() throws IOException {
        NetworkSettingsController controller = createController(true, false,
                ConfigFactory.parseMap(Map.of(I2CP_PORT, 9000)));
        activateController(controller, false);

        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.onToggleUseEmbedded(false);
            controller.onApplyAndShutdown();
        });

        Config bisqConf = ConfigFactory.parseFile(appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME).toFile());
        assertThat(bisqConf.getBoolean(I2P_PATH + ".embeddedRouter")).isFalse();
        assertThat(bisqConf.hasPath(I2CP_PORT)).isFalse();
        assertThat(bisqConf.hasPath(I2P_PATH + ".i2cpHost")).isFalse();
        assertThat(bisqConf.hasPath("application.network.supportedTransportTypes")).isFalse();
        verify(shutDownHandler).shutdown();
    }

    @Test
    void applyingUnchangedNetworkSettingsCreatesNoBisqConf() {
        NetworkSettingsController controller = createController(true, false,
                ConfigFactory.parseMap(Map.of(I2CP_PORT, 9000)));
        activateController(controller, false);

        JavaFxToolkitExtension.runOnFxThread(controller::onApplyAndShutdown);

        assertThat(appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME)).doesNotExist();
        verify(shutDownHandler).shutdown();
    }

    private void activateController(NetworkSettingsController controller, boolean windows) {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            // Static mocks are thread-local, so install the platform input where activation runs.
            try (MockedStatic<OS> os = mockStatic(OS.class, CALLS_REAL_METHODS)) {
                os.when(OS::isWindows).thenReturn(windows);
                controller.onActivate();
            }
        });
    }

    private NetworkSettingsController createController(boolean sandboxEnabled, boolean allowPluggableTransports) {
        return createController(sandboxEnabled, allowPluggableTransports, ConfigFactory.empty());
    }

    private NetworkSettingsController createController(boolean sandboxEnabled,
                                                       boolean allowPluggableTransports,
                                                       Config commandLineConfig) {
        Config rootConfig = ConfigFactory.parseString("""
                application.network {
                    supportedTransportTypes = ["TOR"]
                    configByTransportType {
                        i2p {
                            embeddedRouter = true
                            i2cpHost = "127.0.0.1"
                            i2cpPort = 7654
                            bi2pGrpcHost = "127.0.0.1"
                            bi2pGrpcPort = 6159
                        }
                        tor.sandbox {
                            enabled = %s
                            allowPluggableTransports = %s
                        }
                    }
                }
                """.formatted(sandboxEnabled, allowPluggableTransports));
        ApplicationService.Config config = new ApplicationService.Config(rootConfig, commandLineConfig,
                appDataDirPath, "Bisq2", false, 0, false, "", false, false, 0, false, false);
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getConfig()).thenReturn(config);
        when(serviceProvider.getShutDownHandler()).thenReturn(shutDownHandler);

        return WaitForAsyncUtils.waitForAsyncFx(10_000, () -> new NetworkSettingsController(serviceProvider));
    }
}
