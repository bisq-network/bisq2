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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.PaymentDataController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.options.PaymentOptionsController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.payment_method.PaymentMethodSelectionController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.PaymentSummaryController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class CreatePaymentAccountController extends NavigationController {
    private final OverlayController overlayController;
    @Getter
    private final CreatePaymentAccountModel model;
    @Getter
    private final CreatePaymentAccountView view;
    private final PaymentMethodSelectionController paymentMethodController;
    private final PaymentDataController accountDataController;
    private final PaymentOptionsController optionsController;
    private final PaymentSummaryController summaryController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private Subscription selectedPaymentMethodPin, accountDataPin, showOverlayPin;

    public CreatePaymentAccountController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CREATE_PAYMENT_ACCOUNT);

        overlayController = OverlayController.getInstance();

        model = new CreatePaymentAccountModel();
        view = new CreatePaymentAccountView(model, this);

        paymentMethodController = new PaymentMethodSelectionController();
        accountDataController = new PaymentDataController(serviceProvider);
        optionsController = new PaymentOptionsController(serviceProvider);
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

        selectedPaymentMethodPin = EasyBind.subscribe(paymentMethodController.getSelectedPaymentMethod(),
                paymentMethod -> {
                    if (paymentMethod != null) {
                        model.setPaymentMethod(Optional.of(paymentMethod));
                        accountDataController.setPaymentMethod(paymentMethod);
                        optionsController.setPaymentMethod(paymentMethod);
                        summaryController.setPaymentMethod(paymentMethod);
                        boolean hasOptions = paymentMethod instanceof FiatPaymentMethod fiatMethod &&
                                hasConfigurableOptions(fiatMethod);
                        model.setOptionsVisible(hasOptions);
                        setChildTargets();

                        ReadOnlyBooleanProperty showOverlayProperty = accountDataController.getShowOverlay();
                        if (showOverlayProperty != null) {
                            showOverlayPin = EasyBind.subscribe(showOverlayProperty, showOverlay -> {
                                model.getNextButtonVisible().set(!showOverlay);
                                model.getBackButtonVisible().set(!showOverlay);
                            });
                        } else {
                            log.warn("showOverlayProperty is expected to be not null after setPaymentMethod was called");
                        }
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
        if (showOverlayPin != null) {
            showOverlayPin.unsubscribe();
            showOverlayPin = null;
        }
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCreateAccountButtonVisible().set(navigationTarget == NavigationTarget.CREATE_PAYMENT_ACCOUNT_SUMMARY);
        ReadOnlyBooleanProperty showOverlay = accountDataController.getShowOverlay();
        boolean isOverlayShown = showOverlay != null && showOverlay.get();
        model.getNextButtonVisible().set(!isOverlayShown &&
                navigationTarget != NavigationTarget.CREATE_PAYMENT_ACCOUNT_SUMMARY);
        model.getBackButtonVisible().set(!isOverlayShown && model.getCurrentIndex().get() > 0);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case CREATE_PAYMENT_ACCOUNT_PAYMENT_METHOD -> Optional.of(paymentMethodController);
            case CREATE_PAYMENT_ACCOUNT_DATA -> Optional.of(accountDataController);
            case CREATE_PAYMENT_ACCOUNT_OPTIONS -> Optional.of(optionsController);
            case CREATE_PAYMENT_ACCOUNT_SUMMARY -> Optional.of(summaryController);
            default -> Optional.empty();
        };
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEventWithTextInputFocusCheck(keyEvent, getView().getRoot(), this::navigateNext);
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
        if (target == NavigationTarget.CREATE_PAYMENT_ACCOUNT_SUMMARY) {
            summaryController.setAccountPayload(accountDataController.getAccountPayload());
        }
        model.getSelectedChildTarget().set(target);
        Navigation.navigateTo(target);
    }

    private void setChildTargets() {
        model.getChildTargets().clear();
        model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_PAYMENT_METHOD);
        model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_DATA);
        if (model.isOptionsVisible()) {
            model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_OPTIONS);
        }
        model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_SUMMARY);
    }


    private boolean hasConfigurableOptions(FiatPaymentMethod method) {
        // TODO: Implement logic to determine if the payment method has configurable options
        // For now, return false because no payment methods currently have Options page
        return false;
    }

    private boolean validate() {
        return switch (model.getSelectedChildTarget().get()) {
            case CREATE_PAYMENT_ACCOUNT_PAYMENT_METHOD -> paymentMethodController.validate();
            case CREATE_PAYMENT_ACCOUNT_DATA -> accountDataController.validate();
            case CREATE_PAYMENT_ACCOUNT_OPTIONS -> optionsController.validate();
            default -> true;
        };
    }
}