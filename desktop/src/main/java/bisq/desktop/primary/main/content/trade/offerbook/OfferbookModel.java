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

import bisq.application.DefaultServiceProvider;
import bisq.common.monetary.Market;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.offer.Offer;
import bisq.security.KeyPairService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
    private final ObservableList<OfferListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<OfferListItem> filteredItems = new FilteredList<>(listItems);
    private final SortedList<OfferListItem> sortedItems = new SortedList<>(filteredItems);
    private final StringProperty addDataResultProperty = new SimpleStringProperty("");
    private final StringProperty removeDataResultProperty = new SimpleStringProperty("");
    private final StringProperty priceHeaderTitle = new SimpleStringProperty(Res.offerbook.get("offerbook.table.header.price"));
    private final StringProperty baseAmountHeaderTitle = new SimpleStringProperty(Res.offerbook.get("offerbook.table.header.baseAmount"));
    private final StringProperty quoteAmountHeaderTitle = new SimpleStringProperty(Res.offerbook.get("offerbook.table.header.quoteAmount"));

    public OfferbookModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        keyPairService = serviceProvider.getKeyPairService();
    }

    void addOffer(Offer offer) {
        OfferListItem item = new OfferListItem(offer);
        if (!listItems.contains(item)) {
            listItems.add(item);
        }
    }

    void removeOffer(Offer offer) {
        listItems.remove(new OfferListItem(offer));
    }

    void fillOfferListItems(List<OfferListItem> list) {
        listItems.setAll(list);
    }

    boolean isMyOffer(OfferListItem item) {
        return keyPairService.findKeyPair(item.getOffer().getMakerNetworkId().getPubKey().keyId()).isPresent();
    }

    String getActionButtonTitle(OfferListItem item) {
        return isMyOffer(item) ? Res.common.get("remove") : Res.common.get("contact");
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

    public void applyMarketChange( Market market ) {
        if (market != null) {
            priceHeaderTitle.set(Res.offerbook.get("offerbook.table.header.price", market.quoteCurrencyCode(), market.baseCurrencyCode()));
            baseAmountHeaderTitle.set(Res.offerbook.get("offerbook.table.header.baseAmount", market.baseCurrencyCode()));
            quoteAmountHeaderTitle.set(Res.offerbook.get("offerbook.table.header.quoteAmount", market.quoteCurrencyCode()));
        }
    }
}
