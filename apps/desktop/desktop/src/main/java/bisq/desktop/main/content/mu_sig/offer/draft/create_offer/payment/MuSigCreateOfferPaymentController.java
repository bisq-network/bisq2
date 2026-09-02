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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.payment;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodUtil;
import bisq.common.data.Pair;
import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.PaymentMethodBasedAmountLimitsProvider;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelectionResult;
import bisq.presentation.formatters.AmountFormatter;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection.MAX_NUM_PAYMENT_METHODS;

@Slf4j
public class MuSigCreateOfferPaymentController implements Controller {
    private final MuSigCreateOfferPaymentModel model;
    @Getter
    private final MuSigCreateOfferPaymentView view;
    private final PaymentMethodSelection paymentMethodSelection;
    private final Region owner;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final ListChangeListener<PaymentMethod<?>> selectedPaymentMethodsListener;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();
    private final MarketSelection marketSelection;

    public MuSigCreateOfferPaymentController(ServiceProvider serviceProvider,
                                             CreateOfferUseCase createOfferUseCase,
                                             Region owner,
                                             Consumer<Boolean> navigationButtonsVisibleHandler) {
        paymentMethodSelection = createOfferUseCase.getPaymentMethodSelection();
        marketSelection = createOfferUseCase.getMarketSelection();

        this.owner = owner;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;

        model = new MuSigCreateOfferPaymentModel();
        view = new MuSigCreateOfferPaymentView(model, this);
        model.getSortedPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));
        selectedPaymentMethodsListener = c -> updateTradeLimitInfo();
    }

    public boolean validate() {
        if (paymentMethodSelection.getAccountByPaymentMethod().isEmpty()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getShouldShowNoPaymentMethodSelectedOverlay().set(true);
            model.getNoPaymentMethodSelectedOverlayText().set(
                    marketSelection.getMarket().isCrypto()
                            ? Res.get("muSig.offer.create.paymentMethods.noPaymentMethodSelectedOverlay.subTitle.crypto")
                            : Res.get("muSig.offer.create.paymentMethods.noPaymentMethodSelectedOverlay.subTitle.fiat"));
            return false;
        }

        return true;
    }

    @Override
    public void onActivate() {
        model.getSelectedAccountByPaymentMethod().clear();
        model.getSelectedPaymentMethods().clear();
        model.getPaymentMethodRequiringAccountSelection().set(null);
        model.getAccountsByPaymentMethod().clear();
        updateShouldShowNoAccountOverlay(false);
        updateShouldShowMultipleAccountsOverlay(false);
        model.getPaymentMethodWithoutAccount().set(null);

        Market market = marketSelection.getMarket();
        pins.add(paymentMethodSelection.accountByPaymentMethodObservable().addObserver(new HashMapObserver<>() {
            @Override
            public void put(PaymentMethod<?> paymentMethod, Account<?, ?> account) {
                UIThread.run(() -> {
                    model.getSelectedAccountByPaymentMethod().put(paymentMethod, account);
                    model.getSelectedPaymentMethods().add(paymentMethod);
                });
            }

            @Override
            public void remove(Object key) {
                if (key instanceof PaymentMethod<?> paymentMethod) {
                    model.getSelectedAccountByPaymentMethod().remove(paymentMethod);
                    model.getSelectedPaymentMethods().remove(paymentMethod);
                }
            }
        }));

        pins.add(paymentMethodSelection.accountsByPaymentMethodObservable().addObserver(new HashMapObserver<>() {
            @Override
            public void put(PaymentMethod<?> paymentMethod, List<Account<?, ?>> accounts) {
                UIThread.run(() -> {
                    model.getAccountsByPaymentMethod().put(paymentMethod, accounts);
                });
            }

            @Override
            public void remove(Object key) {
                if (key instanceof PaymentMethod<?> paymentMethod) {
                    model.getAccountsByPaymentMethod().remove(paymentMethod);
                }
            }
        }));

        subscriptions.add(EasyBind.subscribe(model.getPaymentMethodWithoutAccount(), paymentMethod -> {
            if (paymentMethod != null) {
                model.getNoAccountOverlayHeadlineText().set(
                        Res.get("muSig.offer.create.paymentMethod.noAccountOverlay.headline",
                                paymentMethod.getShortDisplayString()));
                updateShouldShowNoAccountOverlay(true);
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getPaymentMethodRequiringAccountSelection(), paymentMethod -> {
            if (paymentMethod != null) {
                model.getMultipleAccountsOverlayHeadlineText().set(
                        Res.get("muSig.offer.create.paymentMethod.multipleAccountOverlay.headline",
                                paymentMethod.getShortDisplayString()));
                updateShouldShowMultipleAccountsOverlay(true);
            }
        }));

        String relevantCurrencyCode = market.getRelevantCurrencyCode();
        List<PaymentMethod<?>> paymentMethods = PaymentMethodUtil.getPaymentMethods(relevantCurrencyCode);
        model.getPaymentMethods().setAll(paymentMethods);

        model.getSelectedPaymentMethods().addListener(selectedPaymentMethodsListener);
        updateTradeLimitInfo();
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pins.forEach(Pin::unbind);
        pins.clear();
        model.getSelectedPaymentMethods().removeListener(selectedPaymentMethodsListener);
    }

    void onTogglePaymentMethod(PaymentMethod<?> paymentMethod, boolean selected, Runnable deSelectHandler) {
        ObservableList<PaymentMethod<?>> selectedPaymentMethods = model.getSelectedPaymentMethods();
        if (selected) {
            if (selectedPaymentMethods.contains(paymentMethod)) {
                return;
            }

            if (selectedPaymentMethods.size() >= MAX_NUM_PAYMENT_METHODS) {
                new Popup().invalid(Res.get("muSig.offer.create.paymentMethods.warn.maxMethodsReached", MAX_NUM_PAYMENT_METHODS))
                        .owner(owner)
                        .onClose(deSelectHandler)
                        .show();
                return;
            }

            PaymentMethodSelectionResult selectionResult = paymentMethodSelection.evaluatePaymentMethodSelectionResult(paymentMethod);
            handlePaymentMethodSelectionResult(paymentMethod, selectionResult, deSelectHandler);
        } else {
            paymentMethodSelection.onDeselectPaymentMethod(paymentMethod);
            selectedPaymentMethods.remove(paymentMethod);
            clearMultipleAccountsOverlaySelection();
        }
    }

    Optional<Account<?, ?>> onSelectAccount(Account<? extends PaymentMethod<?>, ?> account, PaymentMethod<?> paymentMethod) {
        if (account == null || paymentMethod == null) {
            return Optional.empty();
        }

        PaymentMethodSelectionResult selectionResult = paymentMethodSelection.evaluatePaymentMethodSelectionResult(paymentMethod);
        return switch (selectionResult.status()) {
            case NO_ACCOUNT_AVAILABLE -> {
                clearMultipleAccountsOverlaySelection();
                model.getPaymentMethodWithoutAccount().set(paymentMethod);
                yield Optional.empty();
            }
            case SINGLE_ACCOUNT_SELECTED -> {
                Optional<Account<?, ?>> selectedAccount = tryAddAccountByPaymentMethodEntry(
                        selectionResult.methodAccountEntry().orElseThrow(),
                        this::clearMultipleAccountsOverlaySelection);
                selectedAccount.ifPresent(ignored -> clearMultipleAccountsOverlaySelection());
                yield selectedAccount;
            }
            case ACCOUNT_SELECTION_REQUIRED -> {
                updateAccountsForSelectedPaymentMethod(selectionResult.accountsRequiringSelection());
                if (!selectionResult.accountsRequiringSelection().contains(account)) {
                    showAccountUnavailablePopup();
                    yield Optional.empty();
                }
                Optional<Account<?, ?>> selectedAccount = tryAddAccountByPaymentMethodEntry(
                        Map.entry(paymentMethod, account),
                        this::clearMultipleAccountsOverlaySelection);
                selectedAccount.ifPresent(ignored -> clearMultipleAccountsOverlaySelection());
                yield selectedAccount;
            }
        };
    }

    void onNavigateToAccounts() {
        onCloseNoAccountOverlay();
        OverlayController.hide(() -> Navigation.navigateTo(NavigationTarget.FIAT_PAYMENT_ACCOUNTS));
    }

    void onCloseNoAccountOverlay() {
        PaymentMethod<?> paymentMethod = model.getPaymentMethodWithoutAccount().get();
        if (paymentMethod != null) {
            model.getSelectedPaymentMethods().remove(paymentMethod);
        }
        model.getPaymentMethodWithoutAccount().set(null);
        updateShouldShowNoAccountOverlay(false);
    }

    void onKeyPressedWhileShowingNoAccountOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onNavigateToAccounts);
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseNoAccountOverlay);
    }

    void onCloseMultipleAccountsOverlay() {
        PaymentMethod<?> paymentMethod = model.getPaymentMethodRequiringAccountSelection().get();
        if (paymentMethod != null) {
            model.getSelectedPaymentMethods().remove(paymentMethod);
        }
        clearMultipleAccountsOverlaySelection();
    }

    void onKeyPressedWhileShowingMultipleAccountsOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseMultipleAccountsOverlay);
    }

    void onCloseNoPaymentMethodSelectedOverlay() {
        if (model.getShouldShowNoPaymentMethodSelectedOverlay().get()) {
            navigationButtonsVisibleHandler.accept(true);
            model.getShouldShowNoPaymentMethodSelectedOverlay().set(false);
        }
    }

    void onKeyPressedWhileShowingNoPaymentMethodSelectedOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseNoPaymentMethodSelectedOverlay);
    }

    private void updateTradeLimitInfo() {
        ObservableList<PaymentMethod<?>> selectedPaymentMethods = model.getSelectedPaymentMethods();
        boolean isSinglePaymentMethod = selectedPaymentMethods.size() == 1;
        selectedPaymentMethods.stream()
                .map(paymentMethod -> new Pair<>(paymentMethod.getDisplayString(),
                        PaymentMethodBasedAmountLimitsProvider.evaluateLimitInUsd(paymentMethod.getPaymentRail())))
                .min(Comparator.comparing(Pair::getSecond))
                .ifPresentOrElse(pair -> {
                            String formatted = AmountFormatter.formatQuoteAmountWithCode(pair.getSecond());
                            if (isSinglePaymentMethod) {
                                model.getTradeLimitInfo().set(Res.get("muSig.offer.create.paymentMethods.tradeAmountLimitInfo.single", pair.getFirst(), formatted));
                            } else {
                                model.getTradeLimitInfo().set(Res.get("muSig.offer.create.paymentMethods.tradeAmountLimitInfo.multiple", formatted));
                            }
                        },
                        () -> model.getTradeLimitInfo().set(""));
    }

    private void handlePaymentMethodSelectionResult(PaymentMethod<?> paymentMethod,
                                                    PaymentMethodSelectionResult selectionResult,
                                                    Runnable deSelectHandler) {
        switch (selectionResult.status()) {
            case ACCOUNT_SELECTION_REQUIRED -> {
                updateAccountsForSelectedPaymentMethod(selectionResult.accountsRequiringSelection());
                model.getPaymentMethodRequiringAccountSelection().set(paymentMethod);
                updateShouldShowMultipleAccountsOverlay(true);
                deSelectHandler.run();
            }
            case NO_ACCOUNT_AVAILABLE -> {
                clearMultipleAccountsOverlaySelection();
                model.getPaymentMethodWithoutAccount().set(paymentMethod);
                deSelectHandler.run();
            }
            case SINGLE_ACCOUNT_SELECTED ->
                    tryAddAccountByPaymentMethodEntry(selectionResult.methodAccountEntry().orElseThrow(), deSelectHandler);
        }
    }

    private Optional<Account<?, ?>> tryAddAccountByPaymentMethodEntry(Map.Entry<PaymentMethod<?>, Account<?, ?>> entry,
                                                                      Runnable rejectHandler) {
        try {
            paymentMethodSelection.onAddAccountByPaymentMethodEntry(entry);
            return Optional.of(entry.getValue());
        } catch (IllegalArgumentException exception) {
            rejectHandler.run();
            handlePaymentMethodSelectionError(exception);
            return Optional.empty();
        }
    }

    private void handlePaymentMethodSelectionError(IllegalArgumentException exception) {
        switch (exception.getMessage()) {
            case PaymentMethodSelection.MAX_PAYMENT_METHODS_REACHED ->
                    showMaxMethodsReachedPopup();
            case PaymentMethodSelection.ACCOUNT_NOT_ELIGIBLE_FOR_MARKET ->
                    showAccountUnavailablePopup();
            default -> throw exception;
        }
    }

    private void updateAccountsForSelectedPaymentMethod(List<Account<?, ?>> accounts) {
        model.getAccountsForSelectedPaymentMethod().clear();
        model.getAccountsForSelectedPaymentMethod().addAll(accounts);
    }

    private void clearMultipleAccountsOverlaySelection() {
        model.getAccountsForSelectedPaymentMethod().clear();
        model.getPaymentMethodRequiringAccountSelection().set(null);
        updateShouldShowMultipleAccountsOverlay(false);
    }

    private void showMaxMethodsReachedPopup() {
        new Popup().invalid(Res.get("muSig.offer.create.paymentMethods.warn.maxMethodsReached", MAX_NUM_PAYMENT_METHODS))
                .owner(owner)
                .show();
    }

    private void showAccountUnavailablePopup() {
        new Popup().invalid(Res.get("muSig.offer.create.paymentMethods.warn.accountUnavailable"))
                .owner(owner)
                .show();
    }

    private void updateShouldShowNoAccountOverlay(boolean shouldShow) {
        navigationButtonsVisibleHandler.accept(!shouldShow);
        model.getShouldShowNoAccountOverlay().set(shouldShow);
    }

    private void updateShouldShowMultipleAccountsOverlay(boolean shouldShow) {
        navigationButtonsVisibleHandler.accept(!shouldShow);
        model.getShouldShowMultipleAccountsOverlay().set(shouldShow);
    }
}
