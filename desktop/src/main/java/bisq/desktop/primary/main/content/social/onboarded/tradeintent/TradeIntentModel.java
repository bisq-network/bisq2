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

package bisq.desktop.primary.main.content.social.onboarded.tradeintent;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.security.KeyPairService;
import bisq.social.intent.TradeIntent;
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

// Note: tradeintent package will likely get removed
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
    private final ObjectProperty<TradeIntent> tradeIntentProperty = new SimpleObjectProperty<>();

    public TradeIntentModel(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        keyPairService = applicationService.getKeyPairService();
    }

    boolean isMyTradeIntent(TradeIntentListItem item) {
        return keyPairService.findKeyPair(item.getNetworkId().getPubKey().keyId()).isPresent();
    }

    String getActionButtonTitle(TradeIntentListItem item) {
        return isMyTradeIntent(item) ? Res.get("remove") : Res.get("contact");
    }
}
