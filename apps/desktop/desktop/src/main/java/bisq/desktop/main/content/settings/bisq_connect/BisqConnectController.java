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

import bisq.api.ApiService;
import bisq.api.access.pairing.PairingCode;
import bisq.api.access.pairing.PairingService;
import bisq.api.web_socket.WebSocketService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.qr.QrCodeDisplay;
import bisq.desktop.common.threading.UIClock;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.settings.bisq_connect.api_config.ApiConfigController;
import bisq.i18n.Res;
import bisq.settings.DontShowAgainService;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class BisqConnectController implements Controller {
    @Getter
    private final BisqConnectView view;
    private final BisqConnectModel model;
    private final Optional<WebSocketService> optionalWebSocketService;
    private final PairingService pairingService;
    private final Set<Pin> pins = new HashSet<>();
    private final DontShowAgainService dontShowAgainService;
    private final ApiService apiService;
    private Subscription onionAddressSubscription;

    public BisqConnectController(ServiceProvider serviceProvider) {
        apiService = serviceProvider.getApiService();
        optionalWebSocketService = apiService.getWebSocketService();
        pairingService = apiService.getPairingService();
        dontShowAgainService = serviceProvider.getDontShowAgainService();

        ApiConfigController apiConfigController = new ApiConfigController(serviceProvider);
        model = new BisqConnectModel(apiService.getApiConfig().getWebSocketServerUrl(), 220);
        view = new BisqConnectView(model, this, apiConfigController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        optionalWebSocketService.ifPresent(webSocketService -> {
            pins.add(FxBindings.bind(model.getWebSocketServiceState()).to(webSocketService.getState()));

            pins.add(pairingService.getPairingQrCode().addObserver(pairingQrCode ->
                    UIThread.run(() -> applyPairingQrCode(pairingQrCode))));

            pins.add(pairingService.getPairingCode().addObserver(pairingCode ->
                    UIThread.run(() -> applyPairingCode(pairingCode))));

            pins.add(webSocketService.getWebsocketClients().addObserver(() ->
                    UIThread.run(() -> {
                        List<BisqConnectView.ClientListItem> listItems = webSocketService.getWebsocketClients()
                                .stream()
                                .map(webSocketClient -> new BisqConnectView.ClientListItem(webSocketClient, pairingService))
                                .toList();
                        model.getConnectedClients().setAll(listItems);
                    })));
        });

        String dontShowAgainId = "settings.bisqConnect.popup";
        if (dontShowAgainService.showAgain(dontShowAgainId)) {
            new Popup().backgroundInfo(Res.get("settings.bisqConnect.popup"))
                    .dontShowAgainId(dontShowAgainId)
                    .show();
        }
    }

    @Override
    public void onDeactivate() {
        pins.forEach(Pin::unbind);
        pins.clear();
        if (onionAddressSubscription != null) {
            onionAddressSubscription.unsubscribe();
        }
    }

    void onReCreatePairingQrCode() {
        try {
            apiService.createPairingQrCode();

        } catch (Exception e) {
            log.warn("Creating QR code failed", e);
            model.getIsPairingVisible().set(false);
            model.getQrCode().set(null);
            model.getQrCodeImage().set(null);
            new Popup().error(e).show();
        }
    }

    private void applyPairingCode(PairingCode pairingCode) {
        if (pairingCode != null) {
            // TODO We should add a progress bar to see how long code is valid.
           UIClock.addOnSecondTickListener(() ->
                   model.getExpiredPairingQrCodeBoxVisible().set(Instant.now().isAfter(pairingCode.getExpiresAt())));
        }
    }

    private void applyPairingQrCode(String pairingQrCode) {
        if (pairingQrCode != null) {
            model.getIsPairingVisible().set(true);
            model.getQrCode().set(pairingQrCode);
            try {
                Image image = QrCodeDisplay.toImage(pairingQrCode, model.getQrCodeSize());
                model.getQrCodeImage().set(image);
            } catch (RuntimeException e) {
                log.warn("Generating QR code image failed", e);
                model.getIsPairingVisible().set(false);
                model.getQrCode().set(null);
                model.getQrCodeImage().set(null);
                new Popup().error(e).show();
            }
        } else {
            model.getIsPairingVisible().set(false);
            model.getQrCode().set(null);
            model.getQrCodeImage().set(null);
        }
    }
}
