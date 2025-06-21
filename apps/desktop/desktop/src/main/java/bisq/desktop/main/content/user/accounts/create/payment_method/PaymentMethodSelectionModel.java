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

import bisq.account.payment_method.PaymentMethod;
import bisq.common.util.StringUtils;
import bisq.desktop.common.view.Model;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
public class PaymentMethodSelectionModel implements Model {
    private final ObservableList<PaymentMethodSelectionView.PaymentMethodItem> allPaymentMethodItems = FXCollections.observableArrayList();
    private final FilteredList<PaymentMethodSelectionView.PaymentMethodItem> filteredPaymentMethodItems =
            new FilteredList<>(allPaymentMethodItems);
    private final SortedList<PaymentMethodSelectionView.PaymentMethodItem> sortedPaymentMethodItems =
            new SortedList<>(filteredPaymentMethodItems);

    private final ObjectProperty<PaymentMethod<?>> selectedPaymentMethod = new SimpleObjectProperty<>();
    private final StringProperty searchText = new SimpleStringProperty("");
    private final IntegerProperty totalMethodsCount = new SimpleIntegerProperty(0);
    private final IntegerProperty filteredMethodsCount = new SimpleIntegerProperty(0);

    private final ListChangeListener<PaymentMethodSelectionView.PaymentMethodItem> filteredItemsChangeListener;

    public PaymentMethodSelectionModel(List<PaymentMethodSelectionView.PaymentMethodItem> items) {
        this.allPaymentMethodItems.setAll(items);
        totalMethodsCount.set(items.size());
        filteredItemsChangeListener = change ->
                filteredMethodsCount.set(filteredPaymentMethodItems.size());
        filteredPaymentMethodItems.addListener(filteredItemsChangeListener);

        searchText.addListener((obs, oldValue, newValue) ->
                updateFilterPredicate());
    }

    public void updateFilterPredicate() {
        String searchLowerCase = searchText.get().toLowerCase().trim();
        if (searchLowerCase.isEmpty()) {
            filteredPaymentMethodItems.setPredicate(null);
        } else {
            filteredPaymentMethodItems.setPredicate(item ->
                    Optional.ofNullable(item)
                            .map(PaymentMethodSelectionView.PaymentMethodItem::getPaymentMethod)
                            .map(method -> containsSearchTerm(item, method, searchLowerCase))
                            .orElse(false)
            );
        }
    }

    public void selectPaymentMethod(PaymentMethod<?> method) {
        selectedPaymentMethod.set(method);
    }

    private boolean containsSearchTerm(PaymentMethodSelectionView.PaymentMethodItem item,
                                       PaymentMethod<?> method,
                                       String searchLowerCase) {
        String displayString = StringUtils.toOptional(method.getDisplayString()).orElse("");
        String shortDisplayString = StringUtils.toOptional(method.getShortDisplayString()).orElse("");
        String allCurrencies = StringUtils.toOptional(item.getAllCurrencies()).orElse("");
        String allCountries = StringUtils.toOptional(item.getAllCountries()).orElse("");

        return displayString.toLowerCase().contains(searchLowerCase) ||
                shortDisplayString.toLowerCase().contains(searchLowerCase) ||
                allCurrencies.toLowerCase().contains(searchLowerCase) ||
                allCountries.toLowerCase().contains(searchLowerCase);
    }
}