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

package bisq.desktop.main.content.bisq_easy.trade_wizard.take_offer;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
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

import java.util.List;

@Getter
class TradeWizardTakeOfferModel implements Model {
    @Setter
    private BisqEasyOfferbookChannel selectedChannel;
    @Setter
    private Direction direction;
    @Setter
    private Market market;
    @Setter
    private List<FiatPaymentMethod> fiatPaymentMethods;
    @Setter
    private PriceSpec priceSpec = new MarketPriceSpec();
    @Setter
    private AmountSpec amountSpec;
    @Setter
    private boolean isMinAmountEnabled;

    @Setter
    private BisqEasyOffer bisqEasyOffer;
    @Setter
    private BisqEasyOfferbookMessage myOfferMessage;
    @Setter
    private String quoteAmountAsString;
    @Setter
    private String myOfferText;
    @Setter
    private String headLine;
    @Setter
    private String subHeadLine;

    @Setter
    private TradeWizardTakeOfferView.ListItem selectedItem;
    private final BooleanProperty showOffers = new SimpleBooleanProperty();
    private final ObservableList<TradeWizardTakeOfferView.ListItem> matchingOffers = FXCollections.observableArrayList();
    private final FilteredList<TradeWizardTakeOfferView.ListItem> filteredList = new FilteredList<>(matchingOffers);
    private final SortedList<TradeWizardTakeOfferView.ListItem> sortedList = new SortedList<>(filteredList);
    private final ObjectProperty<BisqEasyOffer> selectedBisqEasyOffer = new SimpleObjectProperty<>();

    void reset() {
        selectedChannel = null;
        direction = null;
        market = null;
        fiatPaymentMethods.clear();
        priceSpec = new MarketPriceSpec();
        amountSpec = null;
        isMinAmountEnabled = false;

        bisqEasyOffer = null;
        myOfferMessage = null;
        quoteAmountAsString = null;
        myOfferText = null;
        headLine = null;
        subHeadLine = null;

        selectedItem = null;
        showOffers.set(false);
        matchingOffers.clear();
        selectedBisqEasyOffer.set(null);
    }
}