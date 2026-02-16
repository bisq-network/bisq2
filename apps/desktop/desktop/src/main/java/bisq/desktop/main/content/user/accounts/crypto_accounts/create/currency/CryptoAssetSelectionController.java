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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.currency;

import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.account.payment_method.cbdc.CbdcPaymentMethod;
import bisq.account.payment_method.cbdc.CbdcPaymentMethodUtil;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.crypto.CryptoPaymentMethodUtil;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethodUtil;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.currency.CryptoAssetSelectionView.CryptoAssetItem;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class CryptoAssetSelectionController implements Controller {
    private final CryptoAssetSelectionModel model;
    @Getter
    private final CryptoAssetSelectionView view;
    private Subscription filterMenuItemTogglePin;

    public CryptoAssetSelectionController() {
        // We use sorting provided by the CryptoAssetRepository with major assets first
        Stream<CryptoPaymentMethod> cryptoPaymentMethodStream = CryptoPaymentMethodUtil.getPaymentMethods().stream();
        Stream<StableCoinPaymentMethod> stableCoinPaymentMethods = StableCoinPaymentMethodUtil.getPaymentMethods().stream();
        Stream<CbdcPaymentMethod> cbdcPaymentMethods = CbdcPaymentMethodUtil.getPaymentMethods().stream();
        List<CryptoAssetItem> items = Stream.concat(cryptoPaymentMethodStream,
                        Stream.concat(stableCoinPaymentMethods, cbdcPaymentMethods))
                .map(CryptoAssetItem::new)
                .filter(e -> !e.getTicker().equals("BTC"))
                .toList();

        ToggleGroup toggleGroup = new ToggleGroup();
        RichTableView.FilterMenuItem<CryptoAssetItem> showAllFilterMenuItem = getShowAllFilterMenuItem(toggleGroup);
        RichTableView.FilterMenuItem<CryptoAssetItem> cryptoAssetItemFilterMenuItem = getFilterMenuItem(CryptoAssetItem.Type.CRYPTO_CURRENCY, toggleGroup);
        List<RichTableView.FilterMenuItem<CryptoAssetItem>> filterMenuItems = List.of(
                showAllFilterMenuItem,
                cryptoAssetItemFilterMenuItem,
                getFilterMenuItem(CryptoAssetItem.Type.STABLE_COIN, toggleGroup),
                getFilterMenuItem(CryptoAssetItem.Type.CBDC, toggleGroup));
        toggleGroup.selectToggle(cryptoAssetItemFilterMenuItem);
        model = new CryptoAssetSelectionModel(items, toggleGroup, filterMenuItems);
        view = new CryptoAssetSelectionView(model, this);
    }

    @Override
    public void onActivate() {
        filterMenuItemTogglePin = EasyBind.subscribe(model.getFilterMenuItemToggleGroup().selectedToggleProperty(), this::updateFilter);
    }

    @Override
    public void onDeactivate() {
        filterMenuItemTogglePin.unsubscribe();
        model.getSearchText().set(null);
    }

    public boolean validate() {
        return model.getSelectedPaymentMethod().get() != null;
    }

    public ReadOnlyObjectProperty<DigitalAssetPaymentMethod> getSelectedPaymentMethod() {
        return model.getSelectedPaymentMethod();
    }

    void onItemSelected(CryptoAssetItem item) {
        if (item != null) {
            model.getSelectedItem().set(item);
            model.getSelectedPaymentMethod().set(item.getPaymentMethod());
        }
    }

    void onSearchTextChanged(String searchText) {
        if (searchText != null) {
            model.getSearchText().set(searchText.trim());
            model.setSearchStringPredicate(item -> {
                if (item == null) {
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
        } else {
            model.getSearchText().set(null);
            model.setSearchStringPredicate(item -> true);
        }
        applyPredicates();
    }

    private void updateFilter(Toggle selectedToggle) {
        if (selectedToggle instanceof RichTableView.FilterMenuItem<?> filterMenuItem) {
            Optional<Object> data = filterMenuItem.getData();
            if (data.isPresent() && data.get() instanceof CryptoAssetItem.Type type) {
                model.getSelectedType().set(type);
            } else {
                model.getSelectedType().set(null);
            }
        }

        RichTableView.FilterMenuItem.fromToggle(selectedToggle)
                .ifPresentOrElse(selectedFilterMenuItem -> {
                    UIThread.runOnNextRenderFrame(() -> {
                        model.setFilterItemPredicate(item -> selectedFilterMenuItem.getFilter().test(item));
                        applyPredicates();
                    });

                }, () -> {
                    model.setFilterItemPredicate(item -> true);
                    applyPredicates();
                });
    }

    private void applyPredicates() {
        model.getFilteredList().setPredicate(item ->
                model.getFilterItemPredicate().test(item) &&
                        model.getSearchStringPredicate().test(item)
        );
    }

    private RichTableView.FilterMenuItem<CryptoAssetItem> getShowAllFilterMenuItem(ToggleGroup toggleGroup) {
        return RichTableView.FilterMenuItem.getShowAllFilterMenuItem(toggleGroup);
    }

    private RichTableView.FilterMenuItem<CryptoAssetItem> getFilterMenuItem(CryptoAssetItem.Type type,
                                                                            ToggleGroup toggleGroup) {
        return new RichTableView.FilterMenuItem<>(
                toggleGroup,
                type.getDisplayString(),
                Optional.of(type),
                item -> item.getType().equals(type));
    }
}