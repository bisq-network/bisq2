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

package bisq.desktop.main.content.wallet.receive.address;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.wallet.WalletService;
import javafx.beans.property.ReadOnlyStringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class WalletReceiveAddressController implements Controller {
    @Getter
    private final WalletReceiveAddressView view;
    private final WalletReceiveAddressModel model;
    private final WalletService walletService;
    private final Runnable onNextHandler;

    public WalletReceiveAddressController(ServiceProvider serviceProvider,
                                          Runnable onNextHandler) {
        this.onNextHandler = onNextHandler;
        model = new WalletReceiveAddressModel();
        view = new WalletReceiveAddressView(model, this);
        walletService = serviceProvider.getWalletService().orElseThrow();

        walletService.getUnusedAddress().
                thenAccept(receiveAddress -> {
                    UIThread.run(() -> {
                        model.getReceiveAddress().set(receiveAddress);
                        walletService.findReceiveAddressEntry(receiveAddress).ifPresent(entry -> {
                            model.getReceiveAddressEntry().set(entry);
                            model.getReceiveAddressName().set(entry.getName().orElse(null));
                        });
                        updateIsNewAddress(false);
                    });
                });
    }

    public ReadOnlyStringProperty getReceiveAddress() {
        return model.getReceiveAddress();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onGenerateQrCode() {
        onNextHandler.run();
    }

    void onCopyToClipboard() {
        ClipboardUtil.copyToClipboard(model.getReceiveAddress().get());
    }

    void onCreateNewReceiveAddress() {
        walletService.createReceiveAddress()
                .thenAccept(receiveAddressEntry -> {
                    UIThread.run(() -> {
                        model.getReceiveAddressEntry().set(receiveAddressEntry);
                        model.getReceiveAddress().set(receiveAddressEntry.getAddress());
                        model.getReceiveAddressName().set(null);
                        updateIsNewAddress(true);
                    });
                });
    }

    void onSaveAddressName() {
        UIThread.run(() -> {
            if (model.getReceiveAddressEntry().get() != null
                    && model.getReceiveAddressName().get() != null) {
                String addressName = model.getReceiveAddressName().get().trim();
                boolean wasUpdated = walletService.updateReceiveAddress(model.getReceiveAddressEntry().get(), Optional.of(addressName));
                if (wasUpdated) {
                    model.getIsAddressNameEditable().set(false);
                }
            }
        });
    }

    void onDeleteAddressName() {
        UIThread.run(() -> {
            model.getReceiveAddressName().set("");
        });
    }

    void onAddAddressNote() {
        UIThread.run(() -> {
            boolean shouldShowAddressNote = model.getShouldShowAddressNote().get();
            model.getShouldShowAddressNote().set(!shouldShowAddressNote);
        });
    }

    private void updateIsNewAddress(boolean isNewAddress) {
        model.getIsNewAddress().set(isNewAddress);
        model.getAddressTextFieldDescription().set(isNewAddress
                ? Res.get("wallet.receive.newUnusedAddress")
                : Res.get("wallet.receive.unusedAddress"));
        boolean hasName = model.getReceiveAddressName().get() != null;
        model.getIsAddressNameEditable().set(isNewAddress && !hasName);
        model.getShouldShowAddressNote().set(hasName);
    }
}
