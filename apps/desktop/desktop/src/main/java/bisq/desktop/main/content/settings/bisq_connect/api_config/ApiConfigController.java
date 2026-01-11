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

package bisq.desktop.main.content.settings.bisq_connect.api_config;

import bisq.application.ApplicationService;
import bisq.application.ShutDownHandler;
import bisq.application.TypesafeConfigUtils;
import bisq.common.file.FileMutatorUtils;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.util.NetworkUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.api.ApiConfig;
import bisq.api.ApiService;
import bisq.api.access.transport.ApiAccessTransportService;
import bisq.api.access.transport.ApiAccessTransportType;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ApiConfigController implements Controller {
    @Getter
    private final ApiConfigView view;
    private final ApiConfigModel model;
    private final NetworkService networkService;
    private final ApiAccessTransportService apiAccessTransportService;
    private final ApplicationService.Config appConfig;
    private final ShutDownHandler shutDownHandler;
    @Getter
    private final ApiConfig apiConfig;

    private final Set<Subscription> subscriptions = new HashSet<>();


    public ApiConfigController(ServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        ApiService apiService = serviceProvider.getApiService();
        apiAccessTransportService = apiService.getApiAccessTransportService();
        appConfig = serviceProvider.getConfig();
        shutDownHandler = serviceProvider.getShutDownHandler();

        Config config = readConfig();
        apiConfig = ApiConfig.from(config.getConfig("application.api"));
        model = new ApiConfigModel(apiConfig);
        view = new ApiConfigView(model, this);
    }

    @Override
    public void onActivate() {
        model.getOnionServiceUrlPrompt().set(Res.get("settings.bisqConnect.apiConfig.onionService.url.prompt.na"));
        model.getOnionServiceUrlHelp().set(Res.get("settings.bisqConnect.apiConfig.onionService.url.help"));
        model.getApplyButtonDisabled().set(true);

        // Provide apiAccessTransportTypes
        if (networkService.getSupportedTransportTypes().contains(TransportType.TOR)) {
            model.getApiTransportTypes().add(ApiAccessTransportType.TOR);
        }
        model.getApiTransportTypes().add(ApiAccessTransportType.CLEARNET);


        subscriptions.add(EasyBind.subscribe(model.getTlsRequired(), tlsRequired -> {
            model.getProtocol().set(tlsRequired ? "wss" : "ws");
            updateApplyButton();
        }));

        subscriptions.add(EasyBind.subscribe(
                EasyBind.combine(model.getBindHost(), model.getBindPort(), (host, port) -> {
                    int bindPort = port.intValue();
                    apiAccessTransportService.setBindPort(bindPort);

                    try {
                        return model.getProtocol().get() + "://" + Address.from(host, bindPort).getFullAddress();
                    } catch (Exception ignore) {
                        updateApplyButton();
                        return null;
                    }
                }),
                url -> {
                    if (url != null) {
                        model.getServerUrl().set(url);
                        updateApplyButton();
                    }
                }
        ));

        subscriptions.add(EasyBind.subscribe(
                EasyBind.combine(model.getWebsocketEnabled(), model.getApiAccessTransportType(),
                        this::handleEnableOrTransportChange),
                future -> {
                    future.whenComplete((wasApplied, throwable) -> {
                        log.info("wasApplied {}", wasApplied);
                    });
                }
        ));

        subscriptions.add(EasyBind.subscribe(
                EasyBind.combine(model.getDetectedLanHost(), model.getBindHost(),
                        (detectedLanHost, bindPort) -> detectedLanHost != null && detectedLanHost.equals(bindPort)),
                isEquals -> {
                    model.getDetectedLanHostApplied().set(isEquals);
                }
        ));
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public ReadOnlyObjectProperty<Address> getOnionServiceAddress() {
        return model.getOnionServiceAddress();
    }


    /* --------------------------------------------------------------------- */
    // UI - handler
    /* --------------------------------------------------------------------- */

    void onToggleEnabled(boolean enabled) {
        model.getWebsocketEnabled().set(enabled);
        model.getRestEnabled().set(enabled);
        updateApplyButton();
    }

    void onApiTransportType(ApiAccessTransportType type) {
        if (type != model.getApiAccessTransportType().get()) {
            model.getApiAccessTransportType().set(type);
            updateApplyButton();
        }
    }

    void onApplyDetectedLanHost() {
        String detectedLanHost = model.getDetectedLanHost().get();
        if (detectedLanHost != null && model.getApiAccessTransportType().get() == ApiAccessTransportType.CLEARNET) {
            model.getBindHost().set(detectedLanHost);
        }
    }

    void onApply() {
        if (isValid()) {
            showApplyAndRestartPopup();
        }
    }


    /* --------------------------------------------------------------------- */
    // Enable state, ApiTransportType
    /* --------------------------------------------------------------------- */

    private CompletableFuture<Boolean> handleEnableOrTransportChange(boolean enabled, ApiAccessTransportType type) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        apiAccessTransportService.setApiAccessTransportType(type);

        switch (type) {
            case TOR -> {
                if (!enabled) {
                    return CompletableFuture.completedFuture(false);
                }

                if (model.getOnionServiceUrl().get() == null) {
                    model.getOnionServiceUrlPrompt().set(Res.get("settings.bisqConnect.apiConfig.onionService.url.prompt.publishing"));
                    apiAccessTransportService.publishAndGetTorAddress()
                            .thenAccept(address -> {
                                UIThread.run(() -> {
                                    model.getOnionServiceAddress().set(address);
                                    model.getOnionServiceUrl().set(model.getProtocol().get() + "://" + address.getFullAddress());
                                    model.getOnionServiceUrlPrompt().set(null);
                                    model.getOnionServiceUrlHelp().set(null);
                                    future.complete(true);
                                });
                            });
                } else {
                    future.complete(false);
                }
            }
            case CLEARNET -> {
                if (model.getDetectedLanHost().get() == null) {
                    NetworkUtils.findLANHostAddress(Optional.empty())
                            .ifPresent(value -> UIThread.run(() -> model.getDetectedLanHost().set(value)));
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            }
        }
        return future;
    }


    /* --------------------------------------------------------------------- */
    // Config
    /* --------------------------------------------------------------------- */

    private Config readConfig() {
        return TypesafeConfigUtils.resolveCustomConfig(appConfig.getAppDataDirPath())
                .orElse(ConfigFactory.empty())
                .withFallback(appConfig.getRootConfig())
                .resolve();
    }

    private void writeConfig() {
        Path appDataDirPath = appConfig.getAppDataDirPath();
        Config newConfig = ConfigFactory.parseMap(Map.of(
                "application.api.accessTransportType", model.getApiAccessTransportType().get().name(),
                "application.api.server.websocketEnabled", model.getWebsocketEnabled().get(),
                "application.api.server.restEnabled", model.getRestEnabled().get(),
                "application.api.server.bind.host", model.getBindHost().get(),
                "application.api.server.bind.port", model.getBindPort().get()
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
            new Popup().error(e).show();
        }
    }


    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

    private void updateApplyButton() {
        ApiConfig apiConfig = model.getApiConfig();
        boolean noChange = apiConfig.isWebsocketEnabled() == model.getWebsocketEnabled().get() &&
                apiConfig.isRestEnabled() == model.getRestEnabled().get() &&
                apiConfig.getBindHost().equals(model.getBindHost().get()) &&
                apiConfig.getBindPort() == model.getBindPort().get();
        model.getApplyButtonDisabled().set(noChange || !isValid());
    }

    private void showApplyAndRestartPopup() {
        new Popup().confirmation(Res.get("settings.bisqConnect.restart.confirm"))
                .actionButtonText(Res.get("action.shutDown"))
                .onAction(() -> {
                    writeConfig();
                    shutDownHandler.shutdown();
                })
                .show();
    }

    private boolean isValid() {
        return model.getBindHostValidator().validateAndGet() && model.getBindPortValidator().validateAndGet();
    }
}
