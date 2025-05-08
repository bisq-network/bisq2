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

package bisq.desktop.main.content.mu_sig.create_offer.payment_methods;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MuSigCreateOfferPaymentMethodsModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private String subtitleLabel;
    private final ObservableList<FiatPaymentMethod> paymentMethods = FXCollections.observableArrayList();
    private final SortedList<FiatPaymentMethod> sortedPaymentMethods = new SortedList<>(paymentMethods);
    private final ObservableList<FiatPaymentMethod> selectedPaymentMethods = FXCollections.observableArrayList();
    private final ObservableList<FiatPaymentMethod> addedCustomPaymentMethods = FXCollections.observableArrayList();
    private final StringProperty customPaymentMethodName = new SimpleStringProperty("");
    private final BooleanProperty isPaymentMethodsEmpty = new SimpleBooleanProperty();
    private final BooleanProperty canAddCustomPaymentMethod = new SimpleBooleanProperty();
    private final ObjectProperty<Market> market = new SimpleObjectProperty<>();

    void reset() {
        direction = null;
        subtitleLabel = null;
        paymentMethods.clear();
        selectedPaymentMethods.clear();
        addedCustomPaymentMethods.clear();
        customPaymentMethodName.set("");
        isPaymentMethodsEmpty.set(false);
        canAddCustomPaymentMethod.set(false);
        market.set(null);
    }
}
