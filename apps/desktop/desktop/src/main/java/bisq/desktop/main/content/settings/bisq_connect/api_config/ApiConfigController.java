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

import bisq.api.ApiConfig;
import bisq.api.ApiService;
import bisq.api.access.transport.ApiAccessTransportService;
import bisq.api.access.transport.ApiAccessTransportType;
import bisq.application.ApplicationService;
import bisq.application.ShutDownHandler;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.util.NetworkUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        apiConfig = ApiConfig.from(serviceProvider.getConfig().getRootConfig().getConfig("application.api"));
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
        model.getApiTransportTypes().add(ApiAccessTransportType.CLEAR);


        subscriptions.add(EasyBind.subscribe(model.getTlsRequired(), tlsRequired -> {
            model.getProtocol().set(tlsRequired ? "wss" : "ws");
            updateApplyButton();
        }));

        subscriptions.add(EasyBind.subscribe(model.getTlsKeyStorePassword(), password -> {
            updateApplyButton();
        }));

        subscriptions.add(EasyBind.subscribe(
                EasyBind.combine(model.getBindHost(), model.getBindPort(), (host, port) -> {
                    if (host == null || port == null) {
                        return null;
                    }

                    try {
                        return model.getProtocol().get() + "://" + Address.from(host, port.intValue()).getFullAddress();
                    } catch (Exception ignore) {
                        return null;
                    }
                }),
                url -> {
                    if (url != null) {
                        apiAccessTransportService.setBindPort(model.getBindPort().get());
                        model.getServerUrl().set(url);
                    }
                    updateApplyButton();
                }
        ));

        subscriptions.add(EasyBind.subscribe(
                EasyBind.combine(model.getWebsocketEnabled(), model.getApiAccessTransportType(),
                        this::handleEnableOrTransportChange),
                future -> {
                    future.whenComplete((wasApplied, throwable) -> {
                        if (throwable != null) {
                            log.error("Error handling enable/transport change", throwable);
                        } else {
                            log.info("wasApplied {}", wasApplied);
                        }
                    });
                }
        ));

        subscriptions.add(EasyBind.subscribe(
                EasyBind.combine(model.getDetectedLanHost(), model.getBindHost(),
                        (detectedLanHost, bindHost) -> detectedLanHost != null && detectedLanHost.equals(bindHost)),
                isEquals -> {
                    model.getDetectedLanHostEqualToHost().set(isEquals);
                }
        ));

        model.getTlsKeyStorePassword().set(apiConfig.getTlsKeyStorePassword());
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        model.getTlsKeyStorePassword().set("");
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
        if (detectedLanHost != null && model.getApiAccessTransportType().get() == ApiAccessTransportType.CLEAR) {
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

                model.getTlsRequired().set(false);

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
                            })
                            .exceptionally(throwable -> {
                                log.error("Failed to publish Tor address", throwable);
                                UIThread.run(() -> {
                                    model.getOnionServiceUrlPrompt().set(Res.get("settings.bisqConnect.apiConfig.onionService.url.prompt.na"));
                                    new Popup().error(throwable).show();
                                });
                                future.complete(false);
                                return null;
                            });
                } else {
                    future.complete(false);
                }
            }
            case CLEAR -> {
                model.getTlsRequired().set(true);

                if (model.getDetectedLanHost().get() == null) {
                    Optional<String> lanHostAddress = NetworkUtils.findLANHostAddress(Optional.empty());
                    lanHostAddress.ifPresent(value -> UIThread.run(() -> model.getDetectedLanHost().set(value)));
                    future.complete(lanHostAddress.isPresent());
                } else {
                    future.complete(false);
                }
            }
            case I2P -> future.completeExceptionally(new UnsupportedOperationException("I2P not supported yet"));
        }
        return future;
    }


    /* --------------------------------------------------------------------- */
    // Config
    /* --------------------------------------------------------------------- */

    private boolean writeConfig() {
        boolean tlsRequired = model.getTlsRequired().get();
        String tlsKeyStorePassword = tlsRequired ? model.getTlsKeyStorePassword().get() : "";
        List<String> tlsKeyStoreSan = resolveTlsKeyStoreSan();
        Config newConfig = ConfigFactory.parseMap(Map.of(
                "application.api.accessTransportType", model.getApiAccessTransportType().get().name(),
                "application.api.server.websocketEnabled", model.getWebsocketEnabled().get(),
                "application.api.server.restEnabled", model.getRestEnabled().get(),
                "application.api.server.bind.host", model.getBindHost().get(),
                "application.api.server.bind.port", model.getBindPort().get(),
                "application.api.server.tls.required", tlsRequired,
                "application.api.server.tls.keystore.password", tlsKeyStorePassword,
                "application.api.server.tls.certificate.san", tlsKeyStoreSan
        ));
        try {
            appConfig.writeCustomConfig(newConfig);
            return true;
        } catch (IOException e) {
            Path customConfigFilePath = appConfig.getAppDataDirPath().resolve(ApplicationService.CUSTOM_CONFIG_FILE_NAME);
            log.error("Could not write config file {}", customConfigFilePath.toAbsolutePath(), e);
            new Popup().error(e).show();
            return false;
        }
    }

    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

    private void updateApplyButton() {
        ApiConfig apiConfig = model.getApiConfig();
        boolean tlsKeyStorePasswordNotChanged = !model.getTlsRequired().get() ||
                Objects.equals(model.getTlsKeyStorePassword().get(), apiConfig.getTlsKeyStorePassword());
        boolean noChange = apiConfig.isWebsocketEnabled() == model.getWebsocketEnabled().get() &&
                apiConfig.isRestEnabled() == model.getRestEnabled().get() &&
                apiConfig.getApiAccessTransportType() == model.getApiAccessTransportType().get() &&
                Objects.equals(apiConfig.getBindHost(), model.getBindHost().get()) &&
                apiConfig.getBindPort() == model.getBindPort().get() &&
                tlsKeyStorePasswordNotChanged;
        model.getApplyButtonDisabled().set(noChange || !isValid());
    }

    private void showApplyAndRestartPopup() {
        new Popup().confirmation(Res.get("settings.bisqConnect.restart.confirm"))
                .actionButtonText(Res.get("action.shutDown"))
                .onAction(() -> {
                    boolean succeeded = writeConfig();
                    if (succeeded) {
                        shutDownHandler.shutdown();
                    }
                })
                .show();
    }

    private boolean isValid() {
        boolean tlsPasswordValid = !model.getTlsRequired().get() ||
                (model.getPwdMinLengthValidator().validateAndGet() &&
                        model.getPwdRequiredFieldValidator().validateAndGet());
        return model.getBindHostValidator().validateAndGet() &&
                model.getBindPortValidator().validateAndGet() &&
                tlsPasswordValid;
    }

    private List<String> resolveTlsKeyStoreSan() {
        if (model.getApiAccessTransportType().get() == ApiAccessTransportType.CLEAR) {
            return List.of(model.getBindHost().get());
        } else if (!apiConfig.getTlsKeyStoreSan().isEmpty()) {
            return apiConfig.getTlsKeyStoreSan();
        } else {
            return List.of("127.0.0.1");
        }
    }
}
