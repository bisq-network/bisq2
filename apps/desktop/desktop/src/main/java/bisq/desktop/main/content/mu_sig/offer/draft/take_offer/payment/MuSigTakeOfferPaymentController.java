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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.payment;

import com.google.common.collect.ImmutableMap;
import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.common.locale.CountryRepository;
import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferUseCase;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.AccountCompatibilityMismatch;
import bisq.offer.mu_sig.use_case.take_offer.payment_method.TakeOfferPaymentMethodService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class MuSigTakeOfferPaymentController implements Controller {
    private final MuSigTakeOfferPaymentModel model;
    @Getter
    private final MuSigTakeOfferPaymentView view;
    private final TakeOfferUseCase takeOfferService;
    private final TakeOfferPaymentMethodService takeOfferPaymentMethodService;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private Subscription paymentMethodWithoutAccountPin, paymentMethodWithMultipleAccountsPin;
    private final Set<Pin> pins = new HashSet<>();
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigTakeOfferPaymentController(ServiceProvider serviceProvider,
                                           TakeOfferUseCase takeOfferService,
                                           Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.takeOfferService = takeOfferService;
        takeOfferPaymentMethodService = takeOfferService.getPaymentMethodService();
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;

        model = new MuSigTakeOfferPaymentModel();
        view = new MuSigTakeOfferPaymentView(model, this);

        model.getSortedAccountsForPaymentMethod().setComparator(Comparator.comparing(Account::getAccountName));
        model.getSortedPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));
    }

    public void init(MuSigOffer muSigOffer) {
        Market market = muSigOffer.getMarket();
        model.setMarket(market);
        model.setPaymentMethodCurrencyCode(market.isCrypto() ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode());
        Direction displayDirection = muSigOffer.getDisplayDirection();
        model.setDisplayDirection(displayDirection);
        Direction takersDisplayDirection = muSigOffer.getTakersDisplayDirection();
        model.setHeadline(getPaymentMethodsHeadline(takersDisplayDirection.isBuy()));

        Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod =
                takeOfferPaymentMethodService.getAccountsByPaymentMethod();
        model.getAccountsByPaymentMethod().putAll(accountsByPaymentMethod);

        List<PaymentMethodSpec<?>> offeredPaymentMethodSpecs = market.isBaseCurrencyBitcoin()
                ? muSigOffer.getQuoteSidePaymentMethodSpecs()
                : muSigOffer.getBaseSidePaymentMethodSpecs();
        boolean isSinglePaymentMethod = offeredPaymentMethodSpecs.size() == 1;
        model.setSinglePaymentMethod(isSinglePaymentMethod);
        if (isSinglePaymentMethod) {
            PaymentMethod<?> paymentMethod = offeredPaymentMethodSpecs.get(0).getPaymentMethod();
            model.getSelectedPaymentMethodSpec().set(takeOfferPaymentMethodService.findTakerSidePaymentMethodSpec(paymentMethod).orElseThrow());

            List<Account<?, ?>> accountsForPaymentMethod = accountsByPaymentMethod.getOrDefault(paymentMethod, List.of());
            model.getAccountsForPaymentMethod().setAll(accountsForPaymentMethod);

            model.setSubtitle(Res.get("muSig.offer.taker.payment.subtitle.account", paymentMethod.getShortDisplayString()));
            model.setSinglePaymentMethodAccountSelectionDescription(Res.get("muSig.offer.taker.payment.singlePaymentMethod.accountSelection.prompt",
                    paymentMethod.getShortDisplayString()));
        } else {
            model.setSubtitle(Res.get("muSig.offer.taker.payment.subtitle.paymentMethod"));
        }

        List<? extends PaymentMethod<?>> offeredPaymentMethods = offeredPaymentMethodSpecs.stream()
                .map(spec -> (PaymentMethod<?>) spec.getPaymentMethod())
                .collect(Collectors.toList());
        model.getOfferedPaymentMethods().setAll(offeredPaymentMethods);
        // Methods whose rail limit cannot cover the offer amount are shown disabled with a
        // reason and cannot be selected (take-offer.md, "Amount limits").
        refreshInadmissiblePaymentMethods();

        // Seed the preselection applied by the use case (exactly one eligible account in total).
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedByMethod =
                takeOfferPaymentMethodService.getSelectedAccountByPaymentMethod();
        if (selectedByMethod.size() == 1) {
            PaymentMethod<?> selectedMethod = selectedByMethod.keySet().iterator().next();
            takeOfferPaymentMethodService.findTakerSidePaymentMethodSpec(selectedMethod)
                    .ifPresent(spec -> {
                        model.getSelectedPaymentMethodSpec().set(spec);
                        model.getSelectedAccount().set(selectedByMethod.get(selectedMethod));
                    });
        }
    }

    public ReadOnlyObjectProperty<Account<?, ?>> getSelectedAccount() {
        return model.getSelectedAccount();
    }

    public ReadOnlyObjectProperty<PaymentMethodSpec<?>> getPaymentMethodSpec() {
        return model.getSelectedPaymentMethodSpec();
    }

    public boolean validate() {
        if (model.getSelectedAccount().get() == null) {
            navigationButtonsVisibleHandler.accept(false);
            if (model.isSinglePaymentMethod() && model.getAccountsForPaymentMethod().isEmpty()) {
                // Single-method offers have no chip grid, so Next must lead back to the
                // account-creation prompt instead of the generic no-selection overlay.
                takeOfferPaymentMethodService.getTakerSidePaymentMethodSpecs().stream()
                        .findFirst()
                        .ifPresent(spec -> model.getPaymentMethodWithoutAccount().set(spec.getPaymentMethod()));
            } else {
                model.getShouldShowNoPaymentMethodSelectedOverlay().set(true);
            }
            return false;
        }

        return true;
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getPaymentMethodWithoutAccount().set(null);
        model.getPaymentMethodWithMultipleAccounts().set(null);

        // The rail limits are converted at the market price, so a method inadmissible at
        // initialization can become admissible again (and vice versa) while the take is open;
        // the chip states follow the domain recomputation. The revision signal fires after
        // every recomputation - the published projections are no substitute, as they can all
        // stay equal while another method's admissibility flips through the BTC/USD leg of the
        // limit conversions. The at-registration fire also reconciles changes that happened
        // while this step was not active.
        pins.add(takeOfferService.getAmountService().constraintsRecomputeRevisionObservable().addObserver(revision ->
                UIThread.run(this::refreshInadmissiblePaymentMethods)));

        paymentMethodWithoutAccountPin = EasyBind.subscribe(model.getPaymentMethodWithoutAccount(), paymentMethod -> {
            if (paymentMethod != null) {
                model.getNoAccountOverlayHeadlineText().set(
                        Res.get("muSig.offer.taker.payment.noAccountOverlay.title",
                                paymentMethod.getShortDisplayString()));
                model.getNoAccountOverlayReasonText().set(buildNoAccountReason(paymentMethod));
                updateShouldShowNoAccountOverlay(true);
            }
        });
        paymentMethodWithMultipleAccountsPin = EasyBind.subscribe(model.getPaymentMethodWithMultipleAccounts(),
                paymentMethod -> {
                    if (paymentMethod != null) {
                        model.getMultipleAccountsOverlayHeadlineText().set(
                                Res.get("muSig.offer.taker.payment.multipleAccountOverlay.title",
                                        paymentMethod.getShortDisplayString()));
                        updateShouldShowMultipleAccountsOverlay(true);
                    }
                });

        // A single-method offer without an eligible account has no chip grid to click, so the
        // create-account prompt must open directly (take-offer.md, "Payment method": the
        // prompt applies in every path, including a single-method offer without accounts).
        if (model.isSinglePaymentMethod() && model.getAccountsForPaymentMethod().isEmpty()) {
            PaymentMethodSpec<?> selectedSpec = model.getSelectedPaymentMethodSpec().get();
            if (selectedSpec != null) {
                model.getPaymentMethodWithoutAccount().set(selectedSpec.getPaymentMethod());
            }
        }

        // Deselection clears the service state explicitly in the handlers; a null here must not
        // erase the preselection the use case applied before this controller activates.
        subscriptions.add(EasyBind.subscribe(model.getSelectedAccount(), selectedAccount -> {
            if (selectedAccount != null) {
                // The service clears any previous selection before the put, so switching
                // methods cannot accumulate entries in the service state.
                takeOfferPaymentMethodService.putSelectedAccountByPaymentMethod(selectedAccount.getPaymentMethod(), selectedAccount);
            }
        }));
    }

    @Override
    public void onDeactivate() {
        pins.forEach(Pin::unbind);
        pins.clear();
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        updateShouldShowNoAccountOverlay(false);
        updateShouldShowMultipleAccountsOverlay(false);

        paymentMethodWithoutAccountPin.unsubscribe();
        paymentMethodWithMultipleAccountsPin.unsubscribe();
    }

    void onTogglePaymentMethod(PaymentMethod<?> paymentMethod, boolean isSelected) {
        if (paymentMethod == null) {
            return;
        }
        if (!takeOfferService.isPaymentMethodAdmissible(paymentMethod)) {
            // Not selectable; the chip carries the reason as tooltip. The view rejects these
            // clicks against its model projection, but the projection refreshes through a
            // queued UI task and can lag a recomputation triggered by a background price
            // update within the current event, so the domain is queried directly here. An
            // existing selection stays untouched; the rejection re-syncs the projection and
            // makes the view reconcile the toggle visuals the stale projection let move.
            rejectInadmissibleSelection();
            return;
        }
        if (isSelected) {
            if (model.getAccountsByPaymentMethod().containsKey(paymentMethod)) {
                model.getSelectedPaymentMethodSpec().set(takeOfferPaymentMethodService.findTakerSidePaymentMethodSpec(paymentMethod).orElseThrow());
                List<Account<?, ?>> accountsForPaymentMethod = model.getAccountsByPaymentMethod().get(paymentMethod);
                checkArgument(!accountsForPaymentMethod.isEmpty());

                if (accountsForPaymentMethod.size() == 1) {
                    model.getSelectedAccount().set(accountsForPaymentMethod.get(0));
                } else {
                    // Multiple accounts need an explicit pick from the overlay. Drop any prior
                    // account and domain selection first, so the newly set spec is never paired
                    // with an account from the previously selected method until the user chooses.
                    model.getSelectedAccount().set(null);
                    takeOfferPaymentMethodService.clearSelectedAccountByPaymentMethod();
                    model.getAccountsForPaymentMethod().setAll(accountsForPaymentMethod);
                    model.getPaymentMethodWithMultipleAccounts().set(paymentMethod);
                }
            } else {
                // Selecting a method without an eligible account must drop any previous
                // selection, else Next could proceed with the previously selected method.
                model.getSelectedAccount().set(null);
                model.getSelectedPaymentMethodSpec().set(null);
                takeOfferPaymentMethodService.clearSelectedAccountByPaymentMethod();
                model.getPaymentMethodWithoutAccount().set(paymentMethod);
            }
        } else {
            model.getPaymentMethodWithMultipleAccounts().set(null);
            model.getSelectedAccount().set(null);
            model.getSelectedPaymentMethodSpec().set(null);
            model.getToggleGroup().selectToggle(null);
            takeOfferPaymentMethodService.clearSelectedAccountByPaymentMethod();
        }
    }

    void onSelectAccount(Account<? extends PaymentMethod<?>, ?> account) {
        if (account != null) {
            if (!takeOfferService.isPaymentMethodAdmissible(account.getPaymentMethod())) {
                // The account combos bypass the chip grid, so the admissibility rejection must
                // hold here too, against the domain for the same reason as the chip guard.
                rejectInadmissibleSelection();
                return;
            }
            model.getSelectedAccount().set(account);
            model.getPaymentMethodWithMultipleAccounts().set(null);
        }
        updateShouldShowMultipleAccountsOverlay(false);
    }

    void onCloseMultipleAccountsOverlay() {
        model.getPaymentMethodWithoutAccount().set(null);
        model.getPaymentMethodWithMultipleAccounts().set(null);
        model.getSelectedPaymentMethodSpec().set(null);
        model.getSelectedAccount().set(null);
        model.getToggleGroup().selectToggle(null);
        takeOfferPaymentMethodService.clearSelectedAccountByPaymentMethod();
        updateShouldShowMultipleAccountsOverlay(false);
    }

    void onOpenCreateAccountScreen() {
        onCloseNoAccountOverlay();
        OverlayController.hide(() -> Navigation.navigateTo(NavigationTarget.FIAT_PAYMENT_ACCOUNTS));
    }

    void onCloseNoAccountOverlay() {
        model.getPaymentMethodWithMultipleAccounts().set(null);
        model.getPaymentMethodWithoutAccount().set(null);
        model.getSelectedPaymentMethodSpec().set(null);
        model.getToggleGroup().selectToggle(null);
        updateShouldShowNoAccountOverlay(false);
    }

    void onCloseNoPaymentMethodSelectedOverlay() {
        if (model.getShouldShowNoPaymentMethodSelectedOverlay().get()) {
            navigationButtonsVisibleHandler.accept(true);
            model.getShouldShowNoPaymentMethodSelectedOverlay().set(false);
        }
    }

    void onKeyPressedWhileShowingNoAccountOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseNoAccountOverlay);
    }

    void onKeyPressedWhileShowingMultipleAccountsOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseMultipleAccountsOverlay);
    }

    void onKeyPressedWhileShowingNoPaymentMethodSelectedOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseNoPaymentMethodSelectedOverlay);
    }

    private void updateShouldShowNoAccountOverlay(boolean shouldShow) {
        navigationButtonsVisibleHandler.accept(!shouldShow);
        model.getShouldShowNoAccountOverlay().set(shouldShow);
    }

    private void updateShouldShowMultipleAccountsOverlay(boolean shouldShow) {
        navigationButtonsVisibleHandler.accept(!shouldShow);
        model.getShouldShowMultipleAccountsOverlay().set(shouldShow);
    }

    // The taker has accounts for the method but none passed the offer's AccountOption
    // restrictions; explain the first mismatch so the prompt is actionable. Deterministic
    // pick: alphabetically first account name, country mismatch before bank.
    private boolean refreshInadmissiblePaymentMethods() {
        Set<PaymentMethod<?>> inadmissible = model.getOfferedPaymentMethods().stream()
                .filter(method -> !takeOfferService.isPaymentMethodAdmissible(method))
                .collect(Collectors.toSet());
        if (inadmissible.equals(model.getInadmissiblePaymentMethods())) {
            return false;
        }
        model.getInadmissiblePaymentMethods().clear();
        model.getInadmissiblePaymentMethods().addAll(inadmissible);
        model.getPaymentMethodAdmissibilityVersion().set(model.getPaymentMethodAdmissibilityVersion().get() + 1);
        return true;
    }

    // Called when a selection attempt reaches the controller through a stale projection:
    // sync the projection to the domain; when the projection was already current the version
    // is bumped anyway, as the rejected attempt may have moved toggle state in the view.
    private void rejectInadmissibleSelection() {
        if (!refreshInadmissiblePaymentMethods()) {
            model.getPaymentMethodAdmissibilityVersion().set(model.getPaymentMethodAdmissibilityVersion().get() + 1);
        }
    }

    private String buildNoAccountReason(PaymentMethod<?> paymentMethod) {
        List<AccountCompatibilityMismatch> mismatches =
                takeOfferPaymentMethodService.getIncompatibleAccountsByPaymentMethod().get(paymentMethod);
        if (mismatches == null || mismatches.isEmpty()) {
            return "";
        }
        AccountCompatibilityMismatch mismatch = mismatches.stream()
                .min(Comparator.<AccountCompatibilityMismatch, String>comparing(m -> m.account().getAccountName())
                        .thenComparing(m -> m.dimension().ordinal()))
                .orElseThrow();
        String methodName = paymentMethod.getShortDisplayString();
        return switch (mismatch.dimension()) {
            case COUNTRY -> {
                String acceptedCountries = mismatch.acceptedValues().stream()
                        .map(CountryRepository::getNameByCode)
                        .collect(Collectors.joining(", "));
                yield mismatch.accountValue()
                        .map(countryCode -> Res.get("muSig.offer.taker.payment.noAccountOverlay.reason.country",
                                acceptedCountries, methodName, CountryRepository.getNameByCode(countryCode)))
                        .orElseGet(() -> Res.get("muSig.offer.taker.payment.noAccountOverlay.reason.country.missing",
                                acceptedCountries, methodName));
            }
            case BANK -> {
                String acceptedBanks = String.join(", ", mismatch.acceptedValues());
                yield mismatch.accountValue()
                        .map(bankId -> Res.get("muSig.offer.taker.payment.noAccountOverlay.reason.bank",
                                acceptedBanks, methodName, bankId))
                        .orElseGet(() -> Res.get("muSig.offer.taker.payment.noAccountOverlay.reason.bank.missing",
                                acceptedBanks, methodName));
            }
        };
    }

    private String getPaymentMethodsHeadline(boolean isBuyer) {
        String currencyCode = model.getPaymentMethodCurrencyCode();
        if (model.getMarket().isCrypto()) {
            return isBuyer
                    ? Res.get("muSig.offer.taker.payment.cryptoMarket.headline.buyer", currencyCode)
                    : Res.get("muSig.offer.taker.payment.cryptoMarket.headline.seller", currencyCode);
        } else {
            return isBuyer
                    ? Res.get("muSig.offer.taker.payment.fiatMarket.headline.buyer", currencyCode)
                    : Res.get("muSig.offer.taker.payment.fiatMarket.headline.seller", currencyCode);
        }
    }
}
