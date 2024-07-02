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

import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
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
    private final ObservableList<BitcoinPaymentMethodSpec> offeredBitcoinPaymentMethodSpecs = FXCollections.observableArrayList();
    private final SortedList<BitcoinPaymentMethodSpec> sortedBitcoinPaymentMethodSpecs = new SortedList<>(offeredBitcoinPaymentMethodSpecs);
    private final ObjectProperty<BitcoinPaymentMethodSpec> selectedBitcoinPaymentMethodSpec = new SimpleObjectProperty<>();
    @Setter
    private boolean bitcoinMethodVisible;
    @Setter
    private String bitcoinHeadline;
    @Setter
    private Market market;

    private final ObservableList<FiatPaymentMethodSpec> offeredFiatPaymentMethodSpecs = FXCollections.observableArrayList();
    private final SortedList<FiatPaymentMethodSpec> sortedFiatPaymentMethodSpecs = new SortedList<>(offeredFiatPaymentMethodSpecs);
    private final ObjectProperty<FiatPaymentMethodSpec> selectedFiatPaymentMethodSpec = new SimpleObjectProperty<>();
    @Setter
    private boolean fiatMethodVisible;
    @Setter
    private String fiatHeadline;
}