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

package bisq.desktop.main.content.user.accounts.create.payment_method;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class PaymentMethodSelectionController implements Controller {
    // Popularity scores based on historical snapshot of Bisq1 offers count
    // Higher scores indicate more commonly used payment methods
    private static final Map<FiatPaymentRail, Integer> POPULARITY_SCORES = Map.ofEntries(
            Map.entry(FiatPaymentRail.SEPA, 10),
            Map.entry(FiatPaymentRail.ZELLE, 9),
            Map.entry(FiatPaymentRail.PIX, 8),
            Map.entry(FiatPaymentRail.NATIONAL_BANK, 7),
            Map.entry(FiatPaymentRail.REVOLUT, 6),
            Map.entry(FiatPaymentRail.CASH_BY_MAIL, 6),
            Map.entry(FiatPaymentRail.ACH_TRANSFER, 5),
            Map.entry(FiatPaymentRail.STRIKE, 5),
            Map.entry(FiatPaymentRail.INTERAC_E_TRANSFER, 4),
            Map.entry(FiatPaymentRail.WISE, 4),
            Map.entry(FiatPaymentRail.F2F, 3),
            Map.entry(FiatPaymentRail.US_POSTAL_MONEY_ORDER, 3),
            Map.entry(FiatPaymentRail.PAY_ID, 3),
            Map.entry(FiatPaymentRail.FASTER_PAYMENTS, 3),
            Map.entry(FiatPaymentRail.AMAZON_GIFT_CARD, 2),
            Map.entry(FiatPaymentRail.SWIFT, 2),
            Map.entry(FiatPaymentRail.BIZUM, 2),
            Map.entry(FiatPaymentRail.CASH_DEPOSIT, 2),
            Map.entry(FiatPaymentRail.UPI, 1),
            Map.entry(FiatPaymentRail.CASH_APP, 1)
    );

    private final PaymentMethodSelectionModel model;
    @Getter
    private final PaymentMethodSelectionView view;
    private final Runnable onSelectionConfirmedHandler;

    private String userCountryCode;
    private String userCurrencyCode;
    private boolean isInitialized = false;

    public PaymentMethodSelectionController(Runnable onSelectionConfirmedHandler) {
        this.onSelectionConfirmedHandler = onSelectionConfirmedHandler;
        model = new PaymentMethodSelectionModel();
        view = new PaymentMethodSelectionView(model, this);
    }

    public void init() {
        if (isInitialized) {
            return;
        }

        List<FiatPaymentMethod> availableMethods = FiatPaymentRailUtil.getPaymentRails().stream()
                .filter(rail -> rail != FiatPaymentRail.CUSTOM)
                .filter(FiatPaymentRail::isActive)
                .map(FiatPaymentMethod::fromPaymentRail)
                .collect(Collectors.toList());

        model.getAllPaymentMethods().setAll(availableMethods);

        detectUserLocale();
        setupLocaleAwareSorting();
        model.updateFilterPredicate();

        isInitialized = true;
    }

    @Override
    public void onActivate() {
        if (!isInitialized) {
            init();
        }

        model.updateFilterPredicate();

        UIThread.runOnNextRenderFrame(() -> {
            view.getTableView().refresh();
            performSelectionRestoration();
        });
    }

    @Override
    public void onDeactivate() {
    }

    public void cleanup() {
        model.getAllPaymentMethods().clear();
        model.getAllPaymentMethodItems().clear();
        model.selectPaymentMethod(null);
        model.getSearchText().set("");
        isInitialized = false;
    }

    public ReadOnlyObjectProperty<PaymentMethod<?>> getSelectedPaymentMethod() {
        return model.getSelectedPaymentMethod();
    }

    void onSelectPaymentMethod(PaymentMethod<?> paymentMethod) {
        if (paymentMethod != null) {
            model.selectPaymentMethod(paymentMethod);
            if (onSelectionConfirmedHandler != null) {
                onSelectionConfirmedHandler.run();
            }
        }
    }

    void onSearchTextChanged(String searchText) {
        model.getSearchText().set(StringUtils.toOptional(searchText).orElse("").trim());
    }

    private void performSelectionRestoration() {
        PaymentMethod<?> currentSelection = model.getSelectedPaymentMethod().get();

        if (currentSelection != null) {
            PaymentMethodSelectionView.PaymentMethodItem selectedItem = model.getFilteredPaymentMethodItems().stream()
                    .filter(item -> item.getPaymentMethod().equals(currentSelection))
                    .findFirst()
                    .orElse(null);

            if (selectedItem != null) {
                view.getTableView().getSelectionModel().select(selectedItem);
                view.getTableView().scrollTo(selectedItem);
            } else {
                view.getTableView().getSelectionModel().clearSelection();
            }
            return;
        }

        selectFirstAvailableItem();
    }

    private void selectFirstAvailableItem() {
        if (model.getFilteredPaymentMethodItems().isEmpty()) {
            return;
        }

        PaymentMethodSelectionView.PaymentMethodItem firstItem = model.getFilteredPaymentMethodItems().getFirst();

        model.selectPaymentMethod(firstItem.getPaymentMethod());
        view.getTableView().getSelectionModel().select(firstItem);
        view.getTableView().scrollTo(firstItem);
    }

    private void detectUserLocale() {
        Locale userLocale = LocaleRepository.getDefaultLocale();
        userCountryCode = userLocale.getCountry();
        userCurrencyCode = FiatCurrencyRepository.getCurrencyByCountryCode(userCountryCode).getCode();
    }

    private void setupLocaleAwareSorting() {
        LocaleAwarePaymentMethodComparator localeComparator =
                new LocaleAwarePaymentMethodComparator(userCountryCode, userCurrencyCode);
        model.getAllPaymentMethodItems().sort(localeComparator);
    }

    private static class LocaleAwarePaymentMethodComparator implements
            Comparator<PaymentMethodSelectionView.PaymentMethodItem> {
        private final String userCountryCode;
        private final String userCurrencyCode;

        public LocaleAwarePaymentMethodComparator(String userCountryCode, String userCurrencyCode) {
            this.userCountryCode = StringUtils.toOptional(userCountryCode).orElse("").toUpperCase();
            this.userCurrencyCode = StringUtils.toOptional(userCurrencyCode).orElse("").toUpperCase();
        }

        @Override
        public int compare(PaymentMethodSelectionView.PaymentMethodItem a,
                           PaymentMethodSelectionView.PaymentMethodItem b) {
            int localeRelevanceA = calculateLocaleRelevance(a);
            int localeRelevanceB = calculateLocaleRelevance(b);

            if (localeRelevanceA != localeRelevanceB) {
                return Integer.compare(localeRelevanceB, localeRelevanceA);
            }

            int popularityA = calculatePopularityScore(a.getPaymentMethod());
            int popularityB = calculatePopularityScore(b.getPaymentMethod());

            if (popularityA != popularityB) {
                return Integer.compare(popularityB, popularityA);
            }

            return a.getName().compareToIgnoreCase(b.getName());
        }

        private int calculateLocaleRelevance(PaymentMethodSelectionView.PaymentMethodItem item) {
            PaymentMethod<?> method = item.getPaymentMethod();
            int relevanceScore = 0;

            if (method instanceof FiatPaymentMethod fiatMethod) {
                FiatPaymentRail rail = fiatMethod.getPaymentRail();

                if (rail.getCurrencyCodes().contains(userCurrencyCode)) {
                    relevanceScore += 2;
                }

                boolean supportsUserCountry = rail.getCountries().stream()
                        .anyMatch(country -> country.getCode().equalsIgnoreCase(userCountryCode));
                if (supportsUserCountry) {
                    relevanceScore++;
                }
            }

            return relevanceScore;
        }

        private int calculatePopularityScore(PaymentMethod<?> method) {
            if (method instanceof FiatPaymentMethod fiatMethod) {
                FiatPaymentRail rail = fiatMethod.getPaymentRail();
                return POPULARITY_SCORES.getOrDefault(rail, 0);
            }
            return 0;
        }
    }
}