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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.payment_method;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.LocaleRepository;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.payment_method.PaymentMethodSelectionView.PaymentMethodItem;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class PaymentMethodSelectionController implements Controller {
    private final PaymentMethodSelectionModel model;
    @Getter
    private final PaymentMethodSelectionView view;
    private Subscription searchTextPin;

    public PaymentMethodSelectionController() {
        List<PaymentMethodItem> items = FiatPaymentRailUtil.getPaymentRails().stream()
                .filter(rail -> rail != FiatPaymentRail.CUSTOM)
                .filter(rail ->
                        // TODO until others are implemented
                        rail == FiatPaymentRail.F2F ||
                                rail == FiatPaymentRail.SEPA ||
                                rail == FiatPaymentRail.ZELLE ||
                                rail == FiatPaymentRail.REVOLUT ||
                                rail == FiatPaymentRail.PIX ||
                                rail == FiatPaymentRail.FASTER_PAYMENTS ||
                                rail == FiatPaymentRail.NATIONAL_BANK ||
                                rail == FiatPaymentRail.ACH_TRANSFER
                )
                .map(FiatPaymentMethod::fromPaymentRail)
                .map(PaymentMethodItem::new)
                .collect(Collectors.toList());
        model = new PaymentMethodSelectionModel(items);
        view = new PaymentMethodSelectionView(model, this);
    }

    @Override
    public void onActivate() {
        String userCountryCode = LocaleRepository.getDefaultLocale().getCountry();
        String userCurrencyCode = FiatCurrencyRepository.getCurrencyByCountryCode(userCountryCode).getCode();
        PaymentMethodComparator comparator = new PaymentMethodComparator(userCountryCode, userCurrencyCode);
        // We do not use sorted list as we want to use the column sort properties.
        // This initial sorting is just applied at start. If user use column sorting that will be applied.
        model.getList().sort(comparator);

        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            model.getFilteredList().setPredicate(item -> {
                if (searchText == null) {
                    return true;
                } else if (item == null) {
                    return false;
                } else {
                    String searchLowerCase = searchText.toLowerCase().trim();
                    if (searchLowerCase.isEmpty()) {
                        return true;
                    } else {
                        PaymentMethod<?> paymentMethod = item.getPaymentMethod();
                        return paymentMethod.getDisplayString().toLowerCase().contains(searchLowerCase) ||
                                paymentMethod.getShortDisplayString().toLowerCase().contains(searchLowerCase);
                    }
                }
            });
        });
    }

    @Override
    public void onDeactivate() {
        searchTextPin.unsubscribe();
    }

    public boolean validate() {
        return model.getSelectedPaymentMethod().get() != null;
    }

    public ReadOnlyObjectProperty<PaymentMethod<?>> getSelectedPaymentMethod() {
        return model.getSelectedPaymentMethod();
    }

    void onPaymentMethodSelected(PaymentMethodItem item) {
        if (item != null) {
            model.getSelectedItem().set(item);
            model.getSelectedPaymentMethod().set(item.getPaymentMethod());
        }
    }

    void onSearchTextChanged(String searchText) {
        if (searchText != null) {
            model.getSearchText().set(searchText.trim());
        } else {
            model.getSearchText().set("");
        }
    }
}
