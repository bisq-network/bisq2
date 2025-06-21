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

package bisq.desktop.main.content.user.accounts.create;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.F2FAccount;
import bisq.account.accounts.F2FAccountPayload;
import bisq.account.accounts.SepaAccount;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.locale.Country;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.user.accounts.create.account_data.PaymentDataEntryController;
import bisq.desktop.main.content.user.accounts.create.options.PaymentOptionsController;
import bisq.desktop.main.content.user.accounts.create.payment_method.PaymentMethodSelectionController;
import bisq.desktop.main.content.user.accounts.create.summary.PaymentSummaryController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class CreatePaymentAccountController extends NavigationController {
    private final AccountService accountService;
    private final OverlayController overlayController;
    @Getter
    private final CreatePaymentAccountModel model;
    @Getter
    private final CreatePaymentAccountView view;
    private final PaymentMethodSelectionController paymentMethodController;
    private final PaymentDataEntryController accountDataController;
    private final PaymentOptionsController optionsController;
    private final PaymentSummaryController summaryController;

    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;


    private Subscription selectedPaymentMethodPin, accountDataPin;

    public CreatePaymentAccountController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CREATE_PAYMENT_ACCOUNT);

        this.accountService = serviceProvider.getAccountService();
        overlayController = OverlayController.getInstance();

        model = new CreatePaymentAccountModel();
        view = new CreatePaymentAccountView(model, this);

        paymentMethodController = new PaymentMethodSelectionController(this::onPaymentMethodSelected);
        accountDataController = new PaymentDataEntryController(serviceProvider);
        optionsController = new PaymentOptionsController(serviceProvider);
        summaryController = new PaymentSummaryController(this::createAndSaveAccount);

        initializeWizardFlow();
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void onActivate() {
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        NavigationTarget firstTarget = model.getChildTargets().getFirst();
        model.getSelectedChildTarget().set(firstTarget);
        model.getBackButtonText().set(Res.get("action.back"));
        model.getNextButtonVisible().set(true);

        setupReactiveDataFlow();
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        if (selectedPaymentMethodPin != null) {
            selectedPaymentMethodPin.unsubscribe();
            selectedPaymentMethodPin = null;
        }
        if (accountDataPin != null) {
            accountDataPin.unsubscribe();
            accountDataPin = null;
        }
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);

        boolean isSummary = navigationTarget == NavigationTarget.CREATE_PAYMENT_ACCOUNT_SUMMARY;
        model.getCreateAccountButtonVisible().set(isSummary);
        model.getNextButtonVisible().set(!isSummary);
        model.getBackButtonVisible().set(model.getCurrentIndex().get() > 0);
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
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onNext);
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            if (!validateCurrentStep()) {
                return;
            }

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
        if (accountDataController != null) {
            accountDataController.cleanup();
        }
        if (paymentMethodController != null) {
            paymentMethodController.cleanup();
        }
        if (optionsController != null) {
            optionsController.cleanup();
        }
        if (summaryController != null) {
            summaryController.cleanup();
        }

        resetModel();

        OverlayController.hide();
    }

    void onCreateAccount() {
        summaryController.createAccount();
    }

    private void onPaymentMethodSelected() {
        onNext();
    }

    private void initializeWizardFlow() {
        model.getChildTargets().clear();

        model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_PAYMENT_METHOD);
        model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_DATA);
        model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_SUMMARY);

        model.getCurrentIndex().set(0);
        NavigationTarget firstTarget = model.getChildTargets().getFirst();
        model.getSelectedChildTarget().set(firstTarget);
    }

    private void setupReactiveDataFlow() {
        selectedPaymentMethodPin = EasyBind.subscribe(paymentMethodController.getSelectedPaymentMethod(),
                paymentMethod -> {
                    if (paymentMethod != null) {
                        model.setPaymentMethod(Optional.of(paymentMethod));
                        accountDataController.setPaymentMethod(paymentMethod);
                        optionsController.setPaymentMethod(paymentMethod);
                        summaryController.setPaymentMethod(paymentMethod);
                    }
                    updateChildTargets();
                });

        accountDataPin = EasyBind.subscribe(accountDataController.getAccountData(),
                data -> {
                    if (data != null) {
                        model.setAccountData(Optional.of(data));
                        summaryController.setAccountData(data);
                    }
                });
    }

    private void updateChildTargets() {
        model.getPaymentMethodOpt().ifPresent(paymentMethod -> {
            boolean hasOptions = paymentMethod instanceof FiatPaymentMethod fiatMethod &&
                    hasConfigurableOptions(fiatMethod);
            model.setOptionsVisible(hasOptions);

            model.getChildTargets().clear();
            model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_PAYMENT_METHOD);
            model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_DATA);
            if (hasOptions) {
                model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_OPTIONS);
            }
            model.getChildTargets().add(NavigationTarget.CREATE_PAYMENT_ACCOUNT_SUMMARY);
        });
    }

    private boolean hasConfigurableOptions(FiatPaymentMethod method) {
        // TODO: Implement logic to determine if the payment method has configurable options
        // For now, return false because no payment methods currently have Options page
        return false;
    }

    private void navigateToIndex(int index) {
        model.getCurrentIndex().set(index);
        NavigationTarget target = model.getChildTargets().get(index);
        model.getSelectedChildTarget().set(target);
        Navigation.navigateTo(target);
    }

    private boolean validateCurrentStep() {
        NavigationTarget current = model.getSelectedChildTarget().get();

        if (current == NavigationTarget.CREATE_PAYMENT_ACCOUNT_PAYMENT_METHOD) {
            return paymentMethodController.getSelectedPaymentMethod().get() != null;
        }
        if (current == NavigationTarget.CREATE_PAYMENT_ACCOUNT_DATA) {
            return accountDataController.validate();
        } else if (current == NavigationTarget.CREATE_PAYMENT_ACCOUNT_OPTIONS) {
            return optionsController.validate();
        }

        return true;
    }

    private void createAndSaveAccount() {
        getCreatedAccount().ifPresent(account -> {
            accountService.addPaymentAccount(account);
            accountService.setSelectedAccount(account);
            onClose();
        });
    }

    private Optional<Account<?, ?>> getCreatedAccount() {
        Optional<PaymentMethod<?>> paymentMethodOpt = model.getPaymentMethodOpt();
        Optional<Map<String, Object>> accountDataOpt = model.getAccountDataOpt();

        if (paymentMethodOpt.isEmpty() || accountDataOpt.isEmpty()) {
            return Optional.empty();
        }

        PaymentMethod<?> paymentMethod = paymentMethodOpt.get();
        Map<String, Object> accountData = accountDataOpt.get();
        String accountName = summaryController.getAccountName().orElse("");

        if (paymentMethod.getPaymentRail() instanceof FiatPaymentRail fiatRail) {
            return switch (fiatRail) {
                case SEPA -> createSepaAccount(accountName, accountData)
                        .map(account -> (Account<?, ?>) account);
                case F2F -> createF2FAccount(accountName, accountData)
                        .map(account -> (Account<?, ?>) account);
                default -> Optional.empty();
            };
        }

        return Optional.empty();
    }

    private Optional<SepaAccount> createSepaAccount(String accountName, Map<String, Object> data) {
        String holderName = (String) data.get("holderName");
        String iban = (String) data.get("iban");
        String bic = (String) data.get("bic");
        Optional<Country> countryOpt = Optional.ofNullable((Country) data.get("country"));

        if (StringUtils.isEmpty(holderName) || StringUtils.isEmpty(iban) || countryOpt.isEmpty()) {
            return Optional.empty();
        }

        List<String> acceptedCountryCodes;
        try {
            Object countryCodes = data.get("acceptedCountryCodes");

            if (countryCodes instanceof List<?> list &&
                    list.stream().allMatch(item -> item instanceof String)) {
                @SuppressWarnings("unchecked")
                List<String> codes = (List<String>) countryCodes;
                acceptedCountryCodes = codes;
            } else {
                acceptedCountryCodes = FiatPaymentRailUtil.getSepaEuroCountries();
            }
        } catch (Exception e) {
            log.error("Failed to parse accepted country codes", e);
            acceptedCountryCodes = FiatPaymentRailUtil.getSepaEuroCountries();
        }

        return Optional.of(new SepaAccount(accountName, holderName, iban, bic, countryOpt.get(), acceptedCountryCodes));
    }

    private Optional<F2FAccount> createF2FAccount(String accountName, Map<String, Object> data) {
        String city = (String) data.get("city");
        String contact = (String) data.get("contact");
        Optional<String> extraInfo = StringUtils.toOptional((String) data.get("extraInfo"));
        Optional<Country> countryOpt = Optional.ofNullable((Country) data.get("country"));

        if (StringUtils.isEmpty(city) || StringUtils.isEmpty(contact) || countryOpt.isEmpty()) {
            return Optional.empty();
        }

        F2FAccountPayload payload = new F2FAccountPayload(
                StringUtils.createUid(),
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.F2F).getName(),
                countryOpt.get().getCode(),
                city,
                contact,
                extraInfo.orElse("")
        );

        return Optional.of(new F2FAccount(accountName, payload, countryOpt.get()));
    }

    private void resetModel() {
        model.setPaymentMethod(Optional.empty());
        model.setAccountData(Optional.empty());
        model.setOptionsData(Optional.empty());
        model.setOptionsVisible(false);
        model.setAnimateRightOut(true);

        resetSelectedChildTarget();
        model.getChildTargets().clear();
    }
}