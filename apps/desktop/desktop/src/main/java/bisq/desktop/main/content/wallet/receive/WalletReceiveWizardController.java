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

package bisq.desktop.main.content.wallet.receive;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.View;
import bisq.desktop.main.content.wallet.receive.address.WalletReceiveAddressController;
import bisq.desktop.main.content.wallet.receive.qrcode.WalletAddressQrCodeController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class WalletReceiveWizardController extends NavigationController {
    private final OverlayController overlayController;
    @Getter
    private final WalletReceiveWizardModel model;
    private final WalletReceiveWizardView view;
    private final WalletReceiveAddressController walletReceiveAddressController;
    private final WalletAddressQrCodeController walletAddressQrCodeController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private Subscription selectedAddressPin;

    public WalletReceiveWizardController(ServiceProvider serviceProvider) {
        super(NavigationTarget.WALLET_RECEIVE);

        overlayController = OverlayController.getInstance();

        model = new WalletReceiveWizardModel();
        view = new WalletReceiveWizardView(model, this);

        walletReceiveAddressController = new WalletReceiveAddressController(serviceProvider);
        walletAddressQrCodeController = new WalletAddressQrCodeController(serviceProvider);
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public View<? extends Parent, ? extends Model, ? extends Controller> getView() {
        return view;
    }

    @Override
    public void onActivate() {
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getCurrentIndex().set(0);
        model.getSelectedChildTarget().set(model.getChildTargets().getFirst());

        selectedAddressPin = EasyBind.subscribe(walletReceiveAddressController.getReceiveAddress(), receiveAddress -> {
            if (receiveAddress != null) {
                walletAddressQrCodeController.setReceiveAddress(receiveAddress);
            }
        });
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        selectedAddressPin.unsubscribe();
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        if (navigationTarget == NavigationTarget.WALLET_RECEIVE_ADDRESS) {
            model.getBackButtonVisible().set(false);
            model.getNextButtonText().set(Res.get("wallet.receive.generateQRCode"));
        } else if (navigationTarget == NavigationTarget.WALLET_ADDRESS_QR_CODE) {
            model.getBackButtonVisible().set(true);
            model.getNextButtonText().set(Res.get("action.close"));
        }
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case WALLET_RECEIVE_ADDRESS -> Optional.of(walletReceiveAddressController);
            case WALLET_ADDRESS_QR_CODE -> Optional.of(walletAddressQrCodeController);
            default -> Optional.empty();
        };
    }

    void onNext() {
        if (model.getCurrentIndex().get() == 0
                && walletReceiveAddressController.getReceiveAddress().get() == null) {
            // Do nothing, as there's no valid receive address yet to generate the QR code
            return;
        }

        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            model.setAnimateRightOut(false);
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
        } else {
            onClose();
        }
    }

    void onBack() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            model.setAnimateRightOut(true);
            model.getCurrentIndex().set(prevIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
        }
    }

    void onClose() {
        OverlayController.hide();
        reset();
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEventWithTextInputFocusCheck(keyEvent, getView().getRoot(), this::onNext);
    }

    private void reset() {
        resetSelectedChildTarget();

        walletReceiveAddressController.reset();
        walletAddressQrCodeController.reset();

        model.reset();
    }
}
