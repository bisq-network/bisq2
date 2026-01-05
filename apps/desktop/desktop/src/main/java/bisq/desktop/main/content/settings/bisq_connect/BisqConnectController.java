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

package bisq.desktop.main.content.settings.bisq_connect;

import bisq.application.ApplicationService;
import bisq.application.TypesafeConfigUtils;
import bisq.common.file.FileMutatorUtils;
import bisq.common.network.Address;
import bisq.common.network.ClearnetAddress;
import bisq.common.observable.Pin;
import bisq.common.util.NetworkUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.qr.QrCodeDisplay;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class BisqConnectController implements Controller {


    @Getter
    private final BisqConnectView view;
    private final BisqConnectModel model;
    private final ServiceProvider serviceProvider;

    private boolean initialEnabled;
    private BisqConnectExposureMode initialExposureMode = BisqConnectExposureMode.LAN;
    private boolean savedEnabled;
    private BisqConnectExposureMode savedMode = BisqConnectExposureMode.LAN;
    private String savedPassword;

    private Pin websocketInitPin;
    private Pin websocketErrorPin;
    private Pin clientsPin;
    private Pin websocketAddressPin;

    public BisqConnectController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;

        model = new BisqConnectModel(220);
        view = new BisqConnectView(model, this);
    }

    @Override
    public void onActivate() {
        loadFromConfig();
        serviceProvider.getHttpApiService().getWebSocketService().ifPresent(ws -> {
            initialEnabled = true;
            initialExposureMode = ws.isPublishOnionService() ? BisqConnectExposureMode.TOR : BisqConnectExposureMode.LAN;
            websocketInitPin = ws.addInitObserver(up -> UIThread.run(() -> {
                model.getWebsocketRunning().set(up);
                updatePreview();
            }));
            websocketErrorPin = ws.addErrorObserver(msg -> UIThread.run(() -> {
                model.getWebsocketInitError().set(msg + "\n" + Res.get("settings.bisqConnect.connectionDetails.unexpectedErrorHint"));
                updatePreview();
            }));
            websocketAddressPin = ws.addAddressObserver(address -> UIThread.run(() -> {
                model.getWebsocketAddress().set(address);
                updatePreview();
            }));
            clientsPin = ws.getWebsocketClients().addObserver(() ->
                    UIThread.run(() -> model.getConnectedClients().setAll(ws.getWebsocketClients().getUnmodifiableSet())));
        });
        UIThread.run(this::updatePreview);
    }

    @Override
    public void onDeactivate() {
        try {
            if (websocketInitPin != null) {
                websocketInitPin.unbind();
                websocketErrorPin.unbind();
                websocketAddressPin.unbind();
                clientsPin.unbind();
                websocketInitPin = null;
                websocketErrorPin = null;
                websocketAddressPin = null;
                clientsPin = null;
            }
        } catch (Exception t) {
            log.debug("Error removing websocket listeners", t);
        }
    }

    void onToggleEnabled(boolean enabled) {
        model.getEnabled().set(enabled);
        updateChangeDetectedFlag();
    }

    void onSelectMode(BisqConnectExposureMode mode) {
        model.getSelectedMode().set(mode);
        updateChangeDetectedFlag();
    }

    void onPasswordChanged(String password) {
        String safePassword = password == null ? "" : password;
        model.getPassword().set(safePassword);
        updateChangeDetectedFlag();
    }

    void onSaveChanges() {
        if (model.getPasswordIsValid().get()) {
            writeCustomConfig();
            savedEnabled = model.getEnabled().get();
            savedMode = model.getSelectedMode().get();
            savedPassword = model.getPassword().get();
            model.getIsChangeDetected().set(false);
            new Popup()
                    .feedback(Res.get("settings.bisqConnect.restart.confirm"))
                    .useShutDownButton()
                    .show();
        }
        updatePreview();
    }

    private void loadFromConfig() {
        ApplicationService.Config appConfig = serviceProvider.getConfig();
        Path appDataDirPath = appConfig.getAppDataDirPath();
        Config currentOverrideConfig = TypesafeConfigUtils.resolveCustomConfig(appDataDirPath)
                .orElse(ConfigFactory.empty())
                .withFallback(appConfig.getRootConfig())
                .resolve();

        Config websocketConfig = currentOverrideConfig.getConfig("application.websocket");
        Config websocketServerConfig = websocketConfig.getConfig("server");

        boolean enabled = websocketConfig.getBoolean("enabled");
        boolean publishOnionService = websocketConfig.getBoolean("publishOnionService");
        int port = websocketServerConfig.getInt("port");
        String password = websocketConfig.getString("password");

        BisqConnectExposureMode mode = publishOnionService ? BisqConnectExposureMode.TOR : BisqConnectExposureMode.LAN;

        savedEnabled = enabled;
        savedMode = mode;
        savedPassword = password;

        model.getEnabled().set(enabled);
        model.getSelectedMode().set(mode);
        model.getPort().set(port);
        model.getPassword().set(password);
        model.getIsChangeDetected().set(false);
    }

    private void updatePreview() {
        Optional<Address> address = model.getWebsocketAddress().get();

        try {
            if (address.isPresent()) {
                if (address.get().getHost().equals("0.0.0.0")) {
                    address = Optional.of(new ClearnetAddress(NetworkUtils.findLANHostAddress(Optional.empty()).orElseThrow(), address.get().getPort()));
                }
                String url = "http://" + address.get().getFullAddress();
                applyUrlToModel(url);
            } else {
                model.getApiUrl().set("");
                model.getQrCodeImage().set(null);

                if (model.getEnabled().get() && !initialEnabled) {
                    model.getQrPlaceholder().set(Res.get("settings.bisqConnect.connectionDetails.placeholderRestartRequired"));
                } else if (!initialEnabled) {
                    model.getQrPlaceholder().set(Res.get("settings.bisqConnect.connectionDetails.placeholderDisabled"));
                } else {
                    if (initialExposureMode == BisqConnectExposureMode.LAN) {
                        model.getQrPlaceholder().set(Res.get("settings.bisqConnect.connectionDetails.placeholderLan"));
                    } else {
                        model.getQrPlaceholder().set(Res.get("settings.bisqConnect.connectionDetails.placeholderTor"));
                    }
                }
            }
        } catch (Exception e) {
            model.getApiUrl().set("");
            model.getQrCodeImage().set(null);
            model.getQrPlaceholder().set(Res.get("settings.bisqConnect.connectionDetails.placeholderLan.error"));
        }
    }

    private void applyUrlToModel(String url) {
        model.getApiUrl().set(url);
        model.getQrPlaceholder().set("");
        try {
            model.getQrCodeImage().set(QrCodeDisplay.toImage(url, model.getQrCodeSize().get()));
        } catch (RuntimeException e) {
            log.warn("QR code generation failed", e);
            model.getApiUrl().set("");
            model.getQrCodeImage().set(null);
            model.getQrPlaceholder().set(Res.get("settings.bisqConnect.connectionDetails.placeholderError"));
        }
    }

    private void writeCustomConfig() {
        ApplicationService.Config appConfig = serviceProvider.getConfig();
        Path appDataDirPath = appConfig.getAppDataDirPath();

        boolean torMode = model.getSelectedMode().get().isTor();
        String password = Optional.ofNullable(model.getPassword().get()).orElse("");

        Config newConfig = ConfigFactory.parseMap(Map.of(
                "application.websocket.enabled", model.getEnabled().get(),
                "application.websocket.localhostOnly", torMode,
                "application.websocket.publishOnionService", torMode,
                "application.websocket.server.host", torMode ? "localhost" : "0.0.0.0",
                "application.websocket.password", password
        ));

        Config customConfig = TypesafeConfigUtils.resolveCustomConfig(appDataDirPath).orElse(ConfigFactory.empty());
        Config config = newConfig.withFallback(customConfig).resolve();

        String rendered = config.root().render(ConfigRenderOptions.defaults()
                .setOriginComments(false)
                .setJson(false)
                .setFormatted(true));

        Path customConfigFilePath = appDataDirPath.resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
        try {
            FileMutatorUtils.writeToPath(rendered, customConfigFilePath);
        } catch (IOException e) {
            log.error("Could not write config file {}", customConfigFilePath.toAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    private void updateChangeDetectedFlag() {
        boolean changeDetected = model.getEnabled().get() != savedEnabled
                || model.getSelectedMode().get() != savedMode
                || !Objects.equals(model.getPassword().get(), savedPassword);
        model.getIsChangeDetected().set(changeDetected);
    }
}
