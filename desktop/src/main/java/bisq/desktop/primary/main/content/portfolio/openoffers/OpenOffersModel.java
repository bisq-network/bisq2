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

package bisq.desktop.primary.main.content.portfolio.openoffers;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Model;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.offer.Offer;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.security.KeyPairService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class OpenOffersModel implements Model {
    private final NetworkService networkService;
    private final KeyPairService keyPairService;
    final MarketPriceService marketPriceService;

    // listItems is bound to set from OpenOfferService
    final ObservableList<OpenOfferListItem> listItems = FXCollections.observableArrayList();
    final FilteredList<OpenOfferListItem> filteredItems = new FilteredList<>(listItems);
    final SortedList<OpenOfferListItem> sortedItems = new SortedList<>(filteredItems);
    final StringProperty addDataResultProperty = new SimpleStringProperty("");
    final StringProperty removeDataResultProperty = new SimpleStringProperty("");
    final StringProperty priceHeaderTitle = new SimpleStringProperty();
    final StringProperty baseAmountHeaderTitle = new SimpleStringProperty();
    final StringProperty quoteAmountHeaderTitle = new SimpleStringProperty();


    public OpenOffersModel(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        keyPairService = applicationService.getKeyPairService();
        marketPriceService = applicationService.getMarketPriceService();
    }

    void setRemoveOfferError(Offer offer, Throwable throwable) {
        log.error("Error at remove offer: offer={}, error={}", offer, throwable.toString());  //todo
    }

    void setRemoveOfferResult(Offer offer, BroadcastResult broadcastResult) {
        log.info("Add offer result for offer {}: {}",
                offer, broadcastResult.toString()); //todo
        removeDataResultProperty.set(broadcastResult.toString());
    }
}
