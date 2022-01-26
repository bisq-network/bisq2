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

package bisq.desktop.primary.main.content.trade.offerbook;

import bisq.application.DefaultApplicationService;
import bisq.common.monetary.Market;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.security.KeyPairService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
public class OfferbookModel implements Model {
    private final NetworkService networkService;
    private final KeyPairService keyPairService;

    // References to data in component models
    final ReadOnlyObjectProperty<Market> selectedMarketProperty;
    final ReadOnlyObjectProperty<Direction> directionProperty;

    final ObservableList<OfferListItem> listItems = FXCollections.observableArrayList();
    final FilteredList<OfferListItem> filteredItems = new FilteredList<>(listItems);
    final SortedList<OfferListItem> sortedItems = new SortedList<>(filteredItems);
    final StringProperty addDataResultProperty = new SimpleStringProperty("");
    final StringProperty removeDataResultProperty = new SimpleStringProperty("");
    final StringProperty priceHeaderTitle = new SimpleStringProperty(Res.offerbook.get("offerbook.table.header.price"));
    final StringProperty baseAmountHeaderTitle = new SimpleStringProperty(Res.offerbook.get("offerbook.table.header.baseAmount"));
    final StringProperty quoteAmountHeaderTitle = new SimpleStringProperty(Res.offerbook.get("offerbook.table.header.quoteAmount"));
    final BooleanProperty showAllMarkets = new SimpleBooleanProperty();
    final BooleanProperty marketSelectionDisabled = new SimpleBooleanProperty();
    final MarketPriceService marketPriceService;
    final StringProperty createOfferButtonText = new SimpleStringProperty(Res.offerbook.get("offerbook.table.header.quoteAmount"));

    BooleanProperty showCreateOfferTab = new SimpleBooleanProperty();
     BooleanProperty showTakeOfferTab = new SimpleBooleanProperty();


    public OfferbookModel(DefaultApplicationService applicationService,
                          ReadOnlyObjectProperty<Market> selectedMarketProperty,
                          ReadOnlyObjectProperty<Direction> directionProperty) {
        networkService = applicationService.getNetworkService();
        keyPairService = applicationService.getKeyPairService();
        marketPriceService = applicationService.getMarketPriceService();
        this.selectedMarketProperty = selectedMarketProperty;
        this.directionProperty = directionProperty;
    }

    void addOffer(Offer offer) {
        OfferListItem item = new OfferListItem(offer, marketPriceService);
        if (!listItems.contains(item)) {
            listItems.add(item);
        }
    }

    void removeOffer(Offer offer) {
        listItems.remove(new OfferListItem(offer, marketPriceService));
    }

    void fillOfferListItems(List<OfferListItem> list) {
        listItems.setAll(list);
    }

    boolean isMyOffer(OfferListItem item) {
        return keyPairService.findKeyPair(item.getOffer().getMakerNetworkId().getPubKey().keyId()).isPresent();
    }

    String getActionButtonTitle(OfferListItem item) {
        if (isMyOffer(item)) {
            return Res.common.get("remove");
        } else {
            String currencyCode = item.getOffer().getMarket().baseCurrencyCode();
            String dir = item.getOffer().getDirection().isBuy() ?
                    Res.offerbook.get("direction.label.sell", currencyCode) :
                    Res.offerbook.get("direction.label.buy", currencyCode);
            return Res.offerbook.get("offerbook.table.action.takeOffer", dir);
        }
    }

    String getCreateOfferButtonTitle( ) {
        String currencyCode = selectedMarketProperty.get().baseCurrencyCode();
        String dir = directionProperty.get().isBuy() ?
                Res.offerbook.get("direction.label.sell", currencyCode) :
                Res.offerbook.get("direction.label.buy", currencyCode);
        return Res.offerbook.get("offerbook.createOffer.button", dir);
    }

    void setAddOfferError(Offer offer, Throwable throwable) {
        log.error("Error at add offer: offer={}, error={}", offer, throwable.toString());  //todo
    }

    void setAddOfferError(Throwable throwable) {
        log.error("Error at add offer: error={}", throwable.toString());  //todo
    }

    void setAddOfferResult(Offer offer, BroadcastResult broadcastResult) {
        log.info("Add offer result for offer {}: {}",
                offer, broadcastResult.toString()); //todo
        addDataResultProperty.set(broadcastResult.toString());
    }

    void setRemoveOfferError(Offer offer, Throwable throwable) {
        log.error("Error at remove offer: offer={}, error={}", offer, throwable.toString());  //todo
    }

    void setRemoveOfferResult(Offer offer, BroadcastResult broadcastResult) {
        log.info("Add offer result for offer {}: {}",
                offer, broadcastResult.toString()); //todo
        removeDataResultProperty.set(broadcastResult.toString());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ReadOnlyObjectProperty
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public ReadOnlyObjectProperty<Market> selectedMarketProperty() {
        return selectedMarketProperty;
    }


    public ReadOnlyObjectProperty<Direction> directionProperty() {
        return directionProperty;
    }

}
