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
import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.user.accounts.create.payment_method.PaymentMethodSelectionView.PaymentMethodItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
public class PaymentMethodSelectionModel implements Model {
    private final ObservableList<PaymentMethodItem> list = FXCollections.observableArrayList();
    private final FilteredList<PaymentMethodItem> filteredList = new FilteredList<>(list);
    private final SortedList<PaymentMethodItem> sortedList = new SortedList<>(filteredList);
    private final ObjectProperty<PaymentMethodItem> selectedItem = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod<?>> selectedPaymentMethod = new SimpleObjectProperty<>();
    private final StringProperty searchText = new SimpleStringProperty("");

    public PaymentMethodSelectionModel(List<PaymentMethodItem> list) {
        this.list.setAll(list);
    }
}