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

package bisq.desktop.primary.main.content.trade.multisig.old.offerbook;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.offer.Direction;
import bisq.offer.poc.PocOffer;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.security.KeyPairService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class OfferbookModel implements Model {
    private final NetworkService networkService;
    private final KeyPairService keyPairService;

    // listItems is bound to set from OfferBookService
    final ObservableList<OfferListItem> listItems = FXCollections.observableArrayList();
    final FilteredList<OfferListItem> filteredItems = new FilteredList<>(listItems);
    final SortedList<OfferListItem> sortedItems = new SortedList<>(filteredItems);
    final StringProperty addDataResultProperty = new SimpleStringProperty("");
    final StringProperty removeDataResultProperty = new SimpleStringProperty("");
    final StringProperty priceHeaderTitle = new SimpleStringProperty();
    final StringProperty baseAmountHeaderTitle = new SimpleStringProperty();
    final StringProperty quoteAmountHeaderTitle = new SimpleStringProperty();
    final BooleanProperty showAllMarkets = new SimpleBooleanProperty();
    final BooleanProperty marketSelectionDisabled = new SimpleBooleanProperty();
    final MarketPriceService marketPriceService;
    final StringProperty createOfferButtonText = new SimpleStringProperty(Res.get("offerbook.table.header.quoteAmount"));

    boolean showCreateOfferTab;
    final BooleanProperty showTakeOfferTab = new SimpleBooleanProperty();
    public Market selectedMarket;
    public Direction direction;

    public OfferbookModel(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        keyPairService = applicationService.getKeyPairService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
    }

    boolean isMyOffer(OfferListItem item) {
        return keyPairService.findKeyPair(item.getOffer().getMakerNetworkId().getPubKey().getKeyId()).isPresent();
    }

    String getActionButtonTitle(OfferListItem item) {
        if (isMyOffer(item)) {
            return Res.get("remove");
        } else {
            String currencyCode = item.getOffer().getMarket().getBaseCurrencyCode();
            String dir = item.getOffer().getDirection().isBuy() ?
                    Res.get("direction.label.sell", currencyCode) :
                    Res.get("direction.label.buy", currencyCode);
            return Res.get("offerbook.table.action.takeOffer", dir);
        }
    }

 /*   String getCreateOfferButtonTitle() {
        String currencyCode = selectedMarket.get().baseCurrencyCode();
        String dir = directionProperty.get().isBuy() ?
                Res.get("direction.label.sell", currencyCode) :
                Res.get("direction.label.buy", currencyCode);
        return Res.get("offerbook.createOffer.button", dir);
    }*/

    void setAddOfferError(PocOffer offer, Throwable throwable) {
        log.error("Error at add offer: offer={}, error={}", offer, throwable.toString());  //todo
    }

    void setAddOfferError(Throwable throwable) {
        log.error("Error at add offer: error={}", throwable.toString());  //todo
    }

    void setAddOfferResult(PocOffer offer, BroadcastResult broadcastResult) {
        log.info("Add offer result for offer {}: {}",
                offer, broadcastResult.toString()); //todo
        addDataResultProperty.set(broadcastResult.toString());
    }

    void setRemoveOfferError(PocOffer offer, Throwable throwable) {
        log.error("Error at remove offer: offer={}, error={}", offer, throwable.toString());  //todo
    }

    void setRemoveOfferResult(PocOffer offer, BroadcastResult broadcastResult) {
        log.info("Add offer result for offer {}: {}",
                offer, broadcastResult.toString()); //todo
        removeDataResultProperty.set(broadcastResult.toString());
    }
}
