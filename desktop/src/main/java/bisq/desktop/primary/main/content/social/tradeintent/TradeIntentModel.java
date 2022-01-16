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

package bisq.desktop.primary.main.content.social.tradeintent;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.security.KeyPairService;
import bisq.social.intent.TradeIntent;
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
public class TradeIntentModel implements Model {
    private final NetworkService networkService;
    private final KeyPairService keyPairService;
    private final ObservableList<TradeIntentListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<TradeIntentListItem> filteredItems = new FilteredList<>(listItems);
    private final SortedList<TradeIntentListItem> sortedItems = new SortedList<>(filteredItems);
    private final StringProperty addDataResultProperty = new SimpleStringProperty("");
    private final StringProperty removeDataResultProperty = new SimpleStringProperty("");

    public TradeIntentModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        keyPairService = serviceProvider.getKeyPairService();
    }

    void addPayload(AuthenticatedPayload payload) {
        TradeIntentListItem item = new TradeIntentListItem(payload);
        if (!listItems.contains(item)) {
            listItems.add(item);
        }
    }

    void removePayload(AuthenticatedPayload payload) {
        listItems.remove(new TradeIntentListItem(payload));
    }

    void fillTradeIntentListItem(List<TradeIntentListItem> list) {
        listItems.setAll(list);
    }

    boolean isMyTradeIntent(TradeIntentListItem item) {
        return keyPairService.findKeyPair(item.getNetworkId().getPubKey().keyId()).isPresent();
    }

    String getActionButtonTitle(TradeIntentListItem item) {
        return isMyTradeIntent(item) ? Res.common.get("remove") : Res.common.get("contact");
    }

    void setAddTradeIntentError(TradeIntent tradeIntent, Throwable throwable) {
        log.error("Error at add tradeIntent: tradeIntent={}, error={}", tradeIntent, throwable.toString());  //todo
    }

    void setAddTradeIntentError(Throwable throwable) {
        log.error("Error at add tradeIntent: error={}", throwable.toString());  //todo
    }

    void setAddTradeIntentResult(TradeIntent tradeIntent, BroadcastResult broadcastResult) {
        log.info("Add tradeIntent result for tradeIntent {}: {}",
                tradeIntent, broadcastResult.toString()); //todo
        addDataResultProperty.set(broadcastResult.toString());
    }

    void setRemoveTradeIntentError(TradeIntent tradeIntent, Throwable throwable) {
        log.error("Error at remove tradeIntent: tradeIntent={}, error={}", tradeIntent, throwable.toString());  //todo
    }

    void setRemoveTradeIntentResult(TradeIntent tradeIntent, BroadcastResult broadcastResult) {
        log.info("Add tradeIntent result for tradeIntent {}: {}",
                tradeIntent, broadcastResult.toString()); //todo
        removeDataResultProperty.set(broadcastResult.toString());
    }
}
