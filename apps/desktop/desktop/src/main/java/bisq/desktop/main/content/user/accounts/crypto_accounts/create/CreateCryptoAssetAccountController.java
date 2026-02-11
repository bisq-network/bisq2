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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.address.AddressController;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.currency.CryptoAssetSelectionController;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.summary.PaymentSummaryController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class CreateCryptoAssetAccountController extends NavigationController {
    @Getter
    private final CreateCryptoAssetAccountModel model;
    @Getter
    private final CreateCryptoAssetAccountView view;
    private final OverlayController overlayController;
    private final CryptoAssetSelectionController cryptoAssetSelectionController;
    private final AddressController addressController;
    private final PaymentSummaryController summaryController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private Subscription selectedPaymentMethodPin, accountDataPin;

    public CreateCryptoAssetAccountController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT);

        model = new CreateCryptoAssetAccountModel();
        view = new CreateCryptoAssetAccountView(model, this);

        overlayController = OverlayController.getInstance();

        cryptoAssetSelectionController = new CryptoAssetSelectionController();
        addressController = new AddressController(serviceProvider);
        summaryController = new PaymentSummaryController(serviceProvider);
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void onActivate() {
        model.getNextButtonVisible().set(true);
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        setChildTargets();

        model.getCurrentIndex().set(0);
        model.getSelectedChildTarget().set(model.getChildTargets().get(0));

        selectedPaymentMethodPin = EasyBind.subscribe(cryptoAssetSelectionController.getSelectedPaymentMethod(),
                paymentMethod -> {
                    if (paymentMethod != null) {
                        model.setPaymentMethod(Optional.of(paymentMethod));
                        addressController.setPaymentMethod(paymentMethod);
                        summaryController.setPaymentMethod(paymentMethod);
                        setChildTargets();
                    }
                    model.getNextButtonDisabled().set(paymentMethod == null);
                });
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        if (selectedPaymentMethodPin != null) {
            selectedPaymentMethodPin.unsubscribe();
            selectedPaymentMethodPin = null;
        }
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCreateAccountButtonVisible().set(navigationTarget == NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT_SUMMARY);
        model.getNextButtonVisible().set(navigationTarget != NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT_SUMMARY);
        model.getBackButtonVisible().set(model.getCurrentIndex().get() > 0);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case CREATE_CRYPTO_CURRENCY_ACCOUNT_CURRENCY -> Optional.of(cryptoAssetSelectionController);
            case CREATE_CRYPTO_CURRENCY_ACCOUNT_DATA -> Optional.of(addressController);
            case CREATE_CRYPTO_CURRENCY_ACCOUNT_SUMMARY -> Optional.of(summaryController);
            default -> Optional.empty();
        };
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEventWithTextInputFocusCheck(keyEvent,getView().getRoot(),this::navigateNext);
    }

    void onNext() {
        navigateNext();
    }

    private void navigateNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size() && validate()) {
            model.setAnimateRightOut(false);
            navigateToIndex(nextIndex);
        }
    }

    void onBack() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            model.setAnimateRightOut(true);
            navigateToIndex(prevIndex);
        }
    }

    void onClose() {
        OverlayController.hide();
    }

    void onCreateAccount() {
        summaryController.showAccountNameOverlay();
        model.getNextButtonVisible().set(false);
        model.getCreateAccountButtonVisible().set(false);
        model.getBackButtonVisible().set(false);
    }

    private void navigateToIndex(int index) {
        model.getCurrentIndex().set(index);
        NavigationTarget target = model.getChildTargets().get(index);
        if (target == NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT_SUMMARY) {
            summaryController.setAccountPayload(addressController.getAccountPayload());
        }
        model.getSelectedChildTarget().set(target);
        Navigation.navigateTo(target);
    }

    private void setChildTargets() {
        model.getChildTargets().clear();
        model.getChildTargets().add(NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT_CURRENCY);
        model.getChildTargets().add(NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT_DATA);
        model.getChildTargets().add(NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT_SUMMARY);
    }


    private boolean hasConfigurableOptions(FiatPaymentMethod method) {
        // TODO: Implement logic to determine if the payment method has configurable options
        // For now, return false because no payment methods currently have Options page
        return false;
    }

    private boolean validate() {
        return switch (model.getSelectedChildTarget().get()) {
            case CREATE_CRYPTO_CURRENCY_ACCOUNT_CURRENCY -> cryptoAssetSelectionController.validate();
            case CREATE_CRYPTO_CURRENCY_ACCOUNT_DATA -> addressController.validate();
            default -> true;
        };
    }
}