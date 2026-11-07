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

package bisq.desktop.main.content.wallet.send;

import bisq.common.application.DevMode;
import bisq.common.monetary.Coin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.wallet.WalletSendErrors;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.wallet.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class WalletSendController implements Controller {
    @Getter
    private final WalletSendView view;
    @Getter
    private final WalletSendModel model;
    private final WalletService walletService;
    private boolean activated;

    public WalletSendController(ServiceProvider serviceProvider) {
        walletService = serviceProvider.getWalletService().orElseThrow();
        model = new WalletSendModel();
        view = new WalletSendView(model, this);
    }

    @Override
    public void onActivate() {
        activated = true;
        walletService.isWalletEncrypted()
                .thenAccept(isWalletEncrypted -> UIThread.run(() -> model.getIsPasswordVisible().set(isWalletEncrypted)));
    }

    // The overlay keeps the controller, so a reopened screen starts empty and no password stays in memory.
    @Override
    public void onDeactivate() {
        activated = false;
        model.getAddress().set("");
        model.getAmount().set("");
        model.getPassword().set("");
        model.getSentTxId().set(null);
    }

    void onSend() {
        if (model.getSendInProgress().get() || model.getSentTxId().get() != null) {
            return;
        }
        boolean addressValid = model.getAddressValidator().validateAndGet();
        boolean amountValid = model.getAmountValidator().validateAndGet();
        if (!addressValid || !amountValid) {
            return;
        }
        Coin amount = WalletSendAmountValidator.parse(model.getAmount().get()).orElseThrow();
        String address = WalletSendAddressValidator.canonical(model.getAddress().get(), DevMode.isDevMode()).orElseThrow();
        model.getSendInProgress().set(true);
        walletService.sendToAddress(Optional.ofNullable(model.getPassword().get()), address, amount.getValue())
                .whenComplete((txId, throwable) -> UIThread.run(() -> handleSendResult(txId, throwable)));
    }

    void onCopyTxId() {
        ClipboardUtil.copyToClipboard(model.getSentTxId().get());
    }

    void onClose() {
        OverlayController.hide();
    }

    // The user may have left the screen while the daemon worked; the result must still reach them.
    private void handleSendResult(String txId, Throwable throwable) {
        model.getSendInProgress().set(false);
        if (throwable != null) {
            show(new Popup().warning(WalletSendErrors.toMessage(throwable)));
        } else if (activated) {
            model.getAddress().set("");
            model.getAmount().set("");
            model.getPassword().set("");
            model.getSentTxId().set(txId);
        } else {
            show(new Popup().feedback(Res.get("wallet.send.sent.popup", txId)));
        }
    }

    private void show(Popup popup) {
        if (activated) {
            popup.owner(view.getRoot());
        }
        popup.show();
    }
}
