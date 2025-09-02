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
import bisq.common.application.DevMode;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class NetworkSettingsController implements Controller {
    @Getter
    private final NetworkSettingsView view;
    private final NetworkSettingsModel model;
    private final ShutDownHandler shutDownHandler;
    private final Path baseDir;
    private final ApplicationService.Config appConfig;
    private final Set<Subscription> pins = new HashSet<>();

    public NetworkSettingsController(ServiceProvider serviceProvider) {
        appConfig = serviceProvider.getConfig();
        baseDir = appConfig.getBaseDir();
        shutDownHandler = serviceProvider.getShutDownHandler();
        model = new NetworkSettingsModel();
        view = new NetworkSettingsView(model, this);
    }

    @Override
    public void onActivate() {
        // The rootConfig is already merged with custom config if present at app startup,
        // but not updated when custom config would change.

        Config rootConfig = appConfig.getRootConfig();
        Config currentCustomConfig = TypesafeConfigUtils.resolveCustomConfig(baseDir).orElse(ConfigFactory.empty());
        Config updatedRootConfig = currentCustomConfig
                .withFallback(rootConfig)
                .resolve();

        Config networkConfig = updatedRootConfig.getConfig("application.network");
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
        model.getI2cpAddress().set(new Address(i2pConfig.getString("i2cpHost"), i2pConfig.getInt("i2cpPort")));
        model.getBi2pGrpcAddress().set(new Address(i2pConfig.getString("bi2pGrpcHost"), i2pConfig.getInt("bi2pGrpcPort")));

        pins.add(EasyBind.subscribe(model.getI2cpAddress(), e -> onDataChanged()));
        pins.add(EasyBind.subscribe(model.getBi2pGrpcAddress(), e -> onDataChanged()));

        model.getShutdownButtonVisible().set(false);
        model.getClearOnlyVisible().set(DevMode.isDevMode());
    }


    @Override
    public void onDeactivate() {
        pins.forEach(Subscription::unsubscribe);
        pins.clear();
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

    void onApplyAndShutdown() {
        shutDownHandler.shutdown();
        writeCustomConfig();
    }

    private void onDataChanged() {
        model.getShutdownButtonVisible().set(true);
    }

    private void writeCustomConfig() {
        List<String> supportedTransportTypesOverride = model.getSelectedTransportOption().get().getTransportTypes().stream()
                .map(Enum::name)
                .toList();
        boolean useEmbeddedI2PRouter = model.getUseEmbeddedI2PRouter().get();
        Address i2cpAddress = model.getI2cpAddress().get();
        Address bi2pGrpcAddress = model.getBi2pGrpcAddress().get();

        Config newConfig = ConfigFactory.parseMap(Map.of(
                "application.network.supportedTransportTypes", supportedTransportTypesOverride,
                "application.network.configByTransportType.i2p.embeddedRouter", useEmbeddedI2PRouter,
                "application.network.configByTransportType.i2p.i2cpHost", i2cpAddress.getHost(),
                "application.network.configByTransportType.i2p.i2cpPort", i2cpAddress.getPort(),
                "application.network.configByTransportType.i2p.bi2pGrpcHost", bi2pGrpcAddress.getHost(),
                "application.network.configByTransportType.i2p.bi2pGrpcPort", bi2pGrpcAddress.getPort()
        ));

        Config customConfig = TypesafeConfigUtils.resolveCustomConfig(baseDir).orElse(ConfigFactory.empty());
        Config config = newConfig
                .withFallback(customConfig)
                .resolve();

        String rendered = config.root().render(ConfigRenderOptions.defaults()
                .setOriginComments(false)
                .setJson(false)             // keep HOCON instead of JSON
                .setFormatted(true));       // pretty print

        Path file = baseDir.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        try {
            Files.writeString(file, rendered);
        } catch (IOException e) {
            log.error("Could not write config file {}", file.toAbsolutePath(), e);
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
