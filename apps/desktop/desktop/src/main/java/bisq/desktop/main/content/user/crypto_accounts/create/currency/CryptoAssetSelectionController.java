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
import bisq.account.payment_method.StableCoinPaymentMethod;
import bisq.account.payment_method.StablecoinPaymentMethodUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.crypto_accounts.create.currency.CryptoAssetSelectionView.CryptoAssetItem;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class CryptoAssetSelectionController implements Controller {
    private final CryptoAssetSelectionModel model;
    @Getter
    private final CryptoAssetSelectionView view;
    private Subscription searchTextPin;

    public CryptoAssetSelectionController() {
        // We use sorting provided by the CryptoAssetRepository with major assets first

        Stream<CryptoPaymentMethod> cryptoPaymentMethodStream = CryptoPaymentMethodUtil.getAllCryptoPaymentMethods().stream();
        Stream<StableCoinPaymentMethod> stableCoinPaymentMethods = StablecoinPaymentMethodUtil.getMajorStableCoinPaymentMethods().stream();
        List<CryptoAssetItem> items = Stream.concat(cryptoPaymentMethodStream, stableCoinPaymentMethods)
                .map(CryptoAssetItem::new)
                .toList();
        model = new CryptoAssetSelectionModel(items);
        view = new CryptoAssetSelectionView(model, this);
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
                        return item.relevantStrings().toLowerCase().contains(searchLowerCase);
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

    void onItemSelected(CryptoAssetItem item) {
        if (item != null) {
            model.getSelectedItem().set(item);
            //todo
            model.getSelectedPaymentMethod().set((CryptoPaymentMethod) item.getPaymentMethod());
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