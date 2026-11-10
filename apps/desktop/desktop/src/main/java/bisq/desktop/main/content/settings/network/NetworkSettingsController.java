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
import bisq.application.TypesafeConfigUtils;
import bisq.bonded_roles.security_manager.difficulty_adjustment.DifficultyAdjustmentService;
import bisq.common.application.DevMode;
import bisq.common.file.FileMutatorUtils;
import bisq.common.network.Address;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.common.observable.Pin;
import bisq.common.platform.OS;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.network.p2p.node.transport.TorTransportService;
import bisq.settings.SettingsService;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class NetworkSettingsController implements Controller {

    @Getter
    private final NetworkSettingsView view;
    private final NetworkSettingsModel model;
    private final SettingsService settingsService;
    private final DifficultyAdjustmentService difficultyAdjustmentService;
    private final ApplicationService.Config appConfig;
    private final Path appDataDirPath;
    private final ShutDownHandler shutDownHandler;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private Pin ignoreDiffAdjustmentFromSecManagerPin,
            mostRecentDifficultyAdjustmentFactorOrDefaultPin, difficultyAdjustmentFactorPin;
    private boolean configuredAllowPluggableTransports;
    private Map<String, Object> configuredNetworkEntries = Map.of();

    public NetworkSettingsController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        difficultyAdjustmentService = serviceProvider.getBondedRolesService().getDifficultyAdjustmentService();
        appConfig = serviceProvider.getConfig();
        appDataDirPath = appConfig.getAppDataDirPath();
        shutDownHandler = serviceProvider.getShutDownHandler();

        model = new NetworkSettingsModel();
        view = new NetworkSettingsView(model, this);
    }

    @Override
    public void onActivate() {
        // The rootConfig is already merged with custom config if present at app startup,
        // but not updated when custom config would change.

        // The page shows the values of the next start, where the program arguments and JVM options win over bisq.conf.
        Config rootConfig = appConfig.getRootConfig();
        Config currentCustomConfig = TypesafeConfigUtils.resolveCustomConfig(appDataDirPath).orElse(ConfigFactory.empty());
        Config nextStartConfig = appConfig.getCommandLineConfig()
                .withFallback(currentCustomConfig)
                .withFallback(rootConfig)
                .resolve();

        Config networkConfig = nextStartConfig.getConfig("application.network");
        List<TransportType> supportedTransportTypes = new ArrayList<>(networkConfig.getEnumList(TransportType.class, "supportedTransportTypes"));
        if (supportedTransportTypes.size() == 1) {
            switch (supportedTransportTypes.get(0)) {
                case TOR -> model.getSelectedTransportOption().set(TransportOption.TOR);
                case I2P -> model.getSelectedTransportOption().set(TransportOption.I2P);
                case CLEAR ->
                        model.getSelectedTransportOption().set(TransportOption.CLEAR); // Clearnet not supported in UI
            }
        } else if (supportedTransportTypes.size() == 2 &&
                supportedTransportTypes.contains(TransportType.TOR) &&
                supportedTransportTypes.contains(TransportType.I2P)) {
            model.getSelectedTransportOption().set(TransportOption.TOR_AND_I2P);
        } else {
            log.warn("Unsupported TransportType combination: {}", supportedTransportTypes);
        }

        Config i2pConfig = networkConfig.getConfig("configByTransportType.i2p");
        model.getUseEmbeddedI2PRouter().set(i2pConfig.getBoolean("embeddedRouter"));
        model.getI2cpAddress().set(new ClearnetAddress(i2pConfig.getString("i2cpHost"), i2pConfig.getInt("i2cpPort")));
        model.getBi2pGrpcAddress().set(new ClearnetAddress(i2pConfig.getString("bi2pGrpcHost"), i2pConfig.getInt("bi2pGrpcPort")));
        configuredNetworkEntries = toNetworkEntries();

        // Tor reads its sandbox configuration when it starts, so a change takes effect after a restart.
        Config torSandboxConfig = nextStartConfig.getConfig(TorTransportService.SANDBOX_CONFIG_PATH);
        configuredAllowPluggableTransports = torSandboxConfig.getBoolean("allowPluggableTransports");
        model.getAllowPluggableTransports().set(configuredAllowPluggableTransports);
        model.getTorShutdownButtonVisible().set(false);
        if (!torSandboxConfig.getBoolean("enabled")) {
            model.getAllowPluggableTransportsUnavailableText()
                    .set(Res.get("settings.network.tor.allowPluggableTransports.sandboxDisabled"));
        } else if (OS.isWindows()) {
            // The Tor library refuses pluggable transports in the sandbox on Windows.
            model.getAllowPluggableTransportsUnavailableText()
                    .set(Res.get("settings.network.tor.allowPluggableTransports.windows"));
        } else if (appConfig.isSetOnCommandLine(TorTransportService.ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH)) {
            // The option overrides what the switch would write into bisq.conf
            model.getAllowPluggableTransportsUnavailableText()
                    .set(Res.get("settings.network.tor.allowPluggableTransports.commandLine",
                            TorTransportService.ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH, configuredAllowPluggableTransports));
        } else {
            model.getAllowPluggableTransportsUnavailableText().set("");
        }

        subscriptions.add(EasyBind.subscribe(model.getI2cpAddress(), e -> onDataChanged()));
        subscriptions.add(EasyBind.subscribe(model.getBi2pGrpcAddress(), e -> onDataChanged()));

        model.getShutdownButtonVisible().set(false);
        model.getClearOnlyVisible().set(DevMode.isDevMode());


        ignoreDiffAdjustmentFromSecManagerPin = FxBindings.bindBiDir(model.getIgnoreDiffAdjustmentFromSecManager())
                .to(settingsService.getIgnoreDiffAdjustmentFromSecManager(), settingsService::setIgnoreDiffAdjustmentFromSecManager);
        model.getDifficultyAdjustmentFactorEditable().bind(model.getIgnoreDiffAdjustmentFromSecManager());
        subscriptions.add(EasyBind.subscribe(model.getIgnoreDiffAdjustmentFromSecManager(),
                value -> {
                    if (value) {
                        model.getDifficultyAdjustmentFactorDescriptionText().set(Res.get("settings.network.difficultyAdjustmentFactor.description.self"));
                        if (mostRecentDifficultyAdjustmentFactorOrDefaultPin != null) {
                            mostRecentDifficultyAdjustmentFactorOrDefaultPin.unbind();
                        }
                        difficultyAdjustmentFactorPin = FxBindings.bindBiDir(model.getDifficultyAdjustmentFactor())
                                .to(settingsService.getDifficultyAdjustmentFactor(), settingsService::setDifficultyAdjustmentFactor);
                    } else {
                        model.getDifficultyAdjustmentFactorDescriptionText().set(Res.get("settings.network.difficultyAdjustmentFactor.description.fromSecManager"));
                        if (difficultyAdjustmentFactorPin != null) {
                            difficultyAdjustmentFactorPin.unbind();
                        }
                        mostRecentDifficultyAdjustmentFactorOrDefaultPin = difficultyAdjustmentService.getMostRecentValueOrDefault()
                                .addObserver(mostRecentValueOrDefault ->
                                        UIThread.run(() -> model.getDifficultyAdjustmentFactor().set(mostRecentValueOrDefault)));
                    }
                }));
    }


    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        ignoreDiffAdjustmentFromSecManagerPin.unbind();
        model.getDifficultyAdjustmentFactorEditable().unbind();
        if (difficultyAdjustmentFactorPin != null) {
            difficultyAdjustmentFactorPin.unbind();
        }
        if (mostRecentDifficultyAdjustmentFactorOrDefaultPin != null) {
            mostRecentDifficultyAdjustmentFactorOrDefaultPin.unbind();
        }
    }

    @VisibleForTesting
    NetworkSettingsModel getModel() {
        return model;
    }

    void onResetToDefaults() {
        applyDefaults();
        onDataChanged();
    }

    void onSetTransport(TransportOption transportOption) {
        model.getSelectedTransportOption().set(transportOption);
        onDataChanged();
    }

    void onToggleUseEmbedded(boolean selected) {
        model.getUseEmbeddedI2PRouter().set(selected);
        onDataChanged();
    }

    void onToggleAllowPluggableTransports(boolean selected) {
        model.getAllowPluggableTransports().set(selected);
        model.getTorShutdownButtonVisible().set(selected != configuredAllowPluggableTransports);
    }

    void onApplyAndShutdown() {
        writeNetworkConfig();
        shutDownHandler.shutdown();
    }

    void onApplyTorSettingsAndShutdown() {
        // Only the changed entry, so that the other entries keep following the defaults of new releases
        writeCustomConfig(Map.of(TorTransportService.ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH,
                model.getAllowPluggableTransports().get()));
        shutDownHandler.shutdown();
    }

    private void onDataChanged() {
        model.getShutdownButtonVisible().set(true);
    }

    // Only the changed entries. An unchanged value can come from the command line or from the defaults of this
    // release. In bisq.conf it would stay after the option is removed or after a new release changes the default.
    private void writeNetworkConfig() {
        Map<String, Object> changedEntries = toNetworkEntries().entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getValue(), configuredNetworkEntries.get(entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!changedEntries.isEmpty()) {
            writeCustomConfig(changedEntries);
        }
    }

    private Map<String, Object> toNetworkEntries() {
        Map<String, Object> entries = new HashMap<>();
        TransportOption transportOption = model.getSelectedTransportOption().get();
        // Null if the configured transport types are a combination which the page does not offer
        if (transportOption != null) {
            entries.put("application.network.supportedTransportTypes", transportOption.getTransportTypes().stream()
                    .map(Enum::name)
                    .toList());
        }
        Address i2cpAddress = model.getI2cpAddress().get();
        Address bi2pGrpcAddress = model.getBi2pGrpcAddress().get();
        entries.put("application.network.configByTransportType.i2p.embeddedRouter", model.getUseEmbeddedI2PRouter().get());
        entries.put("application.network.configByTransportType.i2p.i2cpHost", i2cpAddress.getHost());
        entries.put("application.network.configByTransportType.i2p.i2cpPort", i2cpAddress.getPort());
        entries.put("application.network.configByTransportType.i2p.bi2pGrpcHost", bi2pGrpcAddress.getHost());
        entries.put("application.network.configByTransportType.i2p.bi2pGrpcPort", bi2pGrpcAddress.getPort());
        return entries;
    }

    private void writeCustomConfig(Map<String, Object> entries) {
        Config newConfig = ConfigFactory.parseMap(entries);
        Config customConfig = TypesafeConfigUtils.resolveCustomConfig(appDataDirPath).orElse(ConfigFactory.empty());
        Config config = newConfig
                .withFallback(customConfig)
                .resolve();

        String rendered = config.root().render(ConfigRenderOptions.defaults()
                .setOriginComments(false)
                .setJson(false)             // keep HOCON instead of JSON
                .setFormatted(true));       // pretty print

        Path customConfigFilePath = appConfig.getCustomConfigFilePath();
        try {
            FileMutatorUtils.writeToPath(rendered, customConfigFilePath);
        } catch (IOException e) {
            log.error("Could not write config file {}", customConfigFilePath.toAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    private void applyDefaults() {
        model.getSelectedTransportOption().set(DevMode.isDevMode() ? TransportOption.CLEAR : TransportOption.TOR_AND_I2P);
        model.getUseEmbeddedI2PRouter().set(false);
        model.getI2cpAddress().set(NetworkSettingsModel.DEFAULT_I2CP_ADDRESS);
        model.getBi2pGrpcAddress().set(NetworkSettingsModel.DEFAULT_BI2P_GRPC_ADDRESS);
    }
}
