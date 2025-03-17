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

package bisq.desktop.main.content.bisq_easy.trade_wizard.select_offer;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
class TradeWizardSelectOfferModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private Market market;
    @Setter
    private List<BitcoinPaymentMethod> bitcoinPaymentMethods = new ArrayList<>();
    @Setter
    private List<FiatPaymentMethod> fiatPaymentMethods = new ArrayList<>();
    @Setter
    private PriceSpec priceSpec = new MarketPriceSpec();
    @Setter
    private QuoteSideAmountSpec quoteSideAmountSpec;
    @Setter
    private String headline;
    @Setter
    private String subHeadLine;
    @Setter
    private TradeWizardSelectOfferView.ListItem selectedItem;
    private final BooleanProperty showOffers = new SimpleBooleanProperty();
    private final ObservableList<TradeWizardSelectOfferView.ListItem> matchingOffers = FXCollections.observableArrayList();
    private final FilteredList<TradeWizardSelectOfferView.ListItem> filteredList = new FilteredList<>(matchingOffers);
    private final SortedList<TradeWizardSelectOfferView.ListItem> sortedList = new SortedList<>(filteredList);
    private final ObjectProperty<BisqEasyOffer> selectedBisqEasyOffer = new SimpleObjectProperty<>();
    private final BooleanProperty isBackButtonHighlighted = new SimpleBooleanProperty();

    void reset() {
        direction = null;
        market = null;
        bitcoinPaymentMethods.clear();
        fiatPaymentMethods.clear();
        priceSpec = new MarketPriceSpec();
        quoteSideAmountSpec = null;
        headline = null;
        subHeadLine = null;
        selectedItem = null;
        showOffers.set(false);
        matchingOffers.clear();
        selectedBisqEasyOffer.set(null);
        isBackButtonHighlighted.set(false);
    }
}