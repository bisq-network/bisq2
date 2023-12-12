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

package bisq.desktop.main.content.bisq_easy.take_offer.payment_method;

import bisq.desktop.common.view.Model;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TakeOfferPaymentModel implements Model {
    private final ObservableList<FiatPaymentMethodSpec> offeredSpecs = FXCollections.observableArrayList();
    private final SortedList<FiatPaymentMethodSpec> sortedSpecs = new SortedList<>(offeredSpecs);
    private final ObjectProperty<FiatPaymentMethodSpec> selectedSpec = new SimpleObjectProperty<>();
    @Setter
    private String headline;
}