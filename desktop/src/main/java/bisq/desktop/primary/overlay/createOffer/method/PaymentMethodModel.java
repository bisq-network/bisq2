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

package bisq.desktop.primary.overlay.createOffer.method;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class PaymentMethodModel implements Model {
    private final ObservableList<String> allPaymentMethods = FXCollections.observableArrayList();
    private final ObservableList<String> addedCustomMethods = FXCollections.observableArrayList();
    private final ObservableList<String> selectedPaymentMethods = FXCollections.observableArrayList();
    private final StringProperty customMethod = new SimpleStringProperty();
    private final BooleanProperty paymentMethodsEmpty = new SimpleBooleanProperty();
    private final BooleanProperty addCustomMethodIconVisible = new SimpleBooleanProperty();
}