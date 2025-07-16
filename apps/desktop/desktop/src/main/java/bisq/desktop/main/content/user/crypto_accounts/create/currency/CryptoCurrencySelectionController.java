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

package bisq.desktop.main.content.user.crypto_accounts.create.currency;

import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.account.payment_method.CryptoPaymentMethodUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.crypto_accounts.create.currency.CryptoCurrencySelectionView.CryptoCurrencyItem;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;

@Slf4j
public class CryptoCurrencySelectionController implements Controller {
    private final CryptoCurrencySelectionModel model;
    @Getter
    private final CryptoCurrencySelectionView view;
    private Subscription searchTextPin;

    public CryptoCurrencySelectionController() {
        // We use sorting provided by the CryptoCurrencyRepository with major assets first
        List<CryptoCurrencyItem> items = CryptoPaymentMethodUtil.getAllCryptoPaymentMethods().stream()
                .map(CryptoCurrencyItem::new)
                .toList();
        model = new CryptoCurrencySelectionModel(items);
        view = new CryptoCurrencySelectionView(model, this);
    }

    @Override
    public void onActivate() {
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
                        return item.getCurrencyCodeAndDisplayNames().toLowerCase().contains(searchLowerCase);
                    }
                }
            });
        });
    }

    @Override
    public void onDeactivate() {
        searchTextPin.unsubscribe();
        model.getSearchText().set(null);
    }

    public boolean validate() {
        return model.getSelectedPaymentMethod().get() != null;
    }

    public ReadOnlyObjectProperty<CryptoPaymentMethod> getSelectedPaymentMethod() {
        return model.getSelectedPaymentMethod();
    }

    void onItemSelected(CryptoCurrencyItem item) {
        if (item != null) {
            model.getSelectedItem().set(item);
            model.getSelectedPaymentMethod().set(item.getPaymentMethod());
        }
    }

    void onSearchTextChanged(String searchText) {
        if (searchText != null) {
            model.getSearchText().set(searchText.trim());
        } else {
            model.getSearchText().set(null);
        }
    }
}