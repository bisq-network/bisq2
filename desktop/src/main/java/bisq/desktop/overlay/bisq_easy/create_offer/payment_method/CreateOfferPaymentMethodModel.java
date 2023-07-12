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

package bisq.desktop.overlay.bisq_easy.create_offer.payment_method;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;

@Getter
public class CreateOfferPaymentMethodModel implements Model {
    private final ObservableList<FiatPaymentMethod> fiatPaymentMethods = FXCollections.observableArrayList();
    private final SortedList<FiatPaymentMethod> sortedFiatPaymentMethods = new SortedList<>(fiatPaymentMethods);
    private final ObservableList<FiatPaymentMethod> selectedFiatPaymentMethods = FXCollections.observableArrayList();
    private final ObservableList<FiatPaymentMethod> addedCustomFiatPaymentMethods = FXCollections.observableArrayList();
    private final StringProperty customFiatPaymentMethodName = new SimpleStringProperty("");
    private final BooleanProperty isPaymentMethodsEmpty = new SimpleBooleanProperty();
    private final BooleanProperty isAddCustomMethodIconEnabled = new SimpleBooleanProperty();
    private final BooleanProperty showCustomMethodNotEmptyWarning = new SimpleBooleanProperty();
    private final ObjectProperty<Market> market = new SimpleObjectProperty<>();

    void reset() {
        fiatPaymentMethods.clear();
        selectedFiatPaymentMethods.clear();
        addedCustomFiatPaymentMethods.clear();
        customFiatPaymentMethodName.set("");
        isPaymentMethodsEmpty.set(false);
        isAddCustomMethodIconEnabled.set(false);
        market.set(null);
        customFiatPaymentMethodName.set("");
        showCustomMethodNotEmptyWarning.set(false);
    }
}