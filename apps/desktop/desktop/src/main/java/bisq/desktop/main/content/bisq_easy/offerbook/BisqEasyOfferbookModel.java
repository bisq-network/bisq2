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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.chat.ChatChannelDomain;
import bisq.desktop.main.content.chat.ChatModel;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
@Getter
public final class BisqEasyOfferbookModel extends ChatModel {
    private final BooleanProperty offerOnly = new SimpleBooleanProperty();
    private final BooleanProperty isTradeChannelVisible = new SimpleBooleanProperty();
    private final BooleanProperty showFilterOverlay = new SimpleBooleanProperty();
    private final ObservableList<MarketChannelItem> marketChannelItems = FXCollections.observableArrayList();
    private final FilteredList<MarketChannelItem> filteredMarketChannelItems = new FilteredList<>(marketChannelItems);
    private final SortedList<MarketChannelItem> sortedMarketChannelItems = new SortedList<>(filteredMarketChannelItems);
    private final ObjectProperty<MarketChannelItem> selectedMarketChannelItem = new SimpleObjectProperty<>();
    private final StringProperty marketSelectorSearchText = new SimpleStringProperty();
    private final ObjectProperty<MarketFilter> selectedMarketFilter = new SimpleObjectProperty<>();
    @Setter
    private Predicate<MarketChannelItem> marketPricePredicate = marketChannelItem -> true;
    @Setter
    private Predicate<MarketChannelItem> searchTextPredicate = marketChannelItem -> true;
    @Setter
    private Predicate<MarketChannelItem> marketFilterPredicate = marketChannelItem -> true;
    private final ObjectProperty<MarketSortType> selectedMarketSortType = new SimpleObjectProperty<>(MarketSortType.NUM_OFFERS);

    public BisqEasyOfferbookModel(ChatChannelDomain chatChannelDomain) {
        super(chatChannelDomain);
    }
}
