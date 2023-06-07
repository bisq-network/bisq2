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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.method;

import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class TakerSelectPaymentMethodModel implements Model {
    // Method enum name or custom name
    private final ObservableList<String> allPaymentMethodNames = FXCollections.observableArrayList();
    private final ObservableList<String> addedCustomMethodNames = FXCollections.observableArrayList();
    private final ObservableList<String> selectedPaymentMethodNames = FXCollections.observableArrayList();
    private final StringProperty customMethodName = new SimpleStringProperty();
    private final BooleanProperty isPaymentMethodsEmpty = new SimpleBooleanProperty();
    private final BooleanProperty isAddCustomMethodIconEnabled = new SimpleBooleanProperty();
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();

    void reset() {
        allPaymentMethodNames.clear();
        addedCustomMethodNames.clear();
        selectedPaymentMethodNames.clear();
        customMethodName.set(null);
        isPaymentMethodsEmpty.set(false);
        isAddCustomMethodIconEnabled.set(false);
        selectedMarket.set(null);
    }
}