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

package bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.FilteredList;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
class OfferbookListModel implements bisq.desktop.common.view.Model {
    private final Set<String> chatMessageIds = new HashSet<>();
    private final ObservableList<OfferbookListItem> offerbookListItems = FXCollections.observableArrayList();
    private final FilteredList<OfferbookListItem> filteredOfferbookListItems = new FilteredList<>(offerbookListItems);
    private final StringProperty fiatAmountTitle = new SimpleStringProperty();
    private final BooleanProperty showBuyOffers = new SimpleBooleanProperty();
    private final BooleanProperty showOfferListExpanded = new SimpleBooleanProperty();
    private final StringProperty paymentFilterTitle = new SimpleStringProperty();
    private final ObservableList<FiatPaymentMethod> availableMarketPayments = FXCollections.observableArrayList();
    private final ObservableSet<FiatPaymentMethod> selectedMarketPayments = FXCollections.observableSet();
    private final BooleanProperty isCustomPaymentsSelected = new SimpleBooleanProperty();
    private final IntegerProperty activeMarketPaymentsCount = new SimpleIntegerProperty();
    private final SimpleObjectProperty<BisqEasyOfferbookChannel> channel = new SimpleObjectProperty<>();
    private final BooleanProperty showMyOffersOnly = new SimpleBooleanProperty();

    OfferbookListModel() {
    }
}
